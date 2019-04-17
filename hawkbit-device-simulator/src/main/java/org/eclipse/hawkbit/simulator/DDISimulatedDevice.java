/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiResult;
import org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus;
import org.eclipse.hawkbit.ddi.json.model.DdiStatus.ExecutionStatus;
import org.eclipse.hawkbit.ddi.json.model.DdiUpdateMode;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifact;
import org.eclipse.hawkbit.dmf.json.model.DmfArtifactHash;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.simulator.DeviceSimulatorUpdater.UpdaterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A simulated device using the DDI API of the hawkBit update server.
 *
 */
public class DDISimulatedDevice extends AbstractSimulatedDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(DDISimulatedDevice.class);

    private final DdiRootControllerRestApi controllerResource;

    private final DeviceSimulatorUpdater deviceUpdater;

    private final String gatewayToken;

    private volatile boolean removed;
    private volatile Long currentActionId;

    /**
     * @param id
     *            the ID of the device
     * @param tenant
     *            the tenant of the simulated device
     * @param pollDelaySec
     *            the delay of the poll interval in sec
     * @param controllerResource
     *            the http controller resource
     * @param deviceUpdater
     *            the service to update devices
     * @param gatewayToken
     *            to authenticate at DDI and for download as well
     */
    public DDISimulatedDevice(final String id, final String tenant, final int pollDelaySec,
            final DdiRootControllerRestApi controllerResource, final DeviceSimulatorUpdater deviceUpdater,
            final String gatewayToken) {
        super(id, tenant, Protocol.DDI_HTTP, pollDelaySec);
        this.controllerResource = controllerResource;
        this.deviceUpdater = deviceUpdater;
        this.gatewayToken = gatewayToken;
        LOGGER.info("Id: %s, tenant: %s \n", id, tenant);
    }

    @Override
    public void clean() {
        super.clean();
        removed = true;
    }

    /**
     * Polls the base URL for the DDI API interface.
     */
    @Override
    public void poll() {
        if (!removed) {
            ResponseEntity<DdiControllerBase> poll = null;
            try {
                poll = controllerResource.getControllerBase(getTenant(), getId());
            } catch (final RuntimeException ex) {
                LOGGER.error("Failed base poll", ex);
                return;
            }

            if (!HttpStatus.OK.equals(poll.getStatusCode())) {
                return;
            }

            final String href = poll.getBody().getLink("deploymentBase").getHref();
            if (href == null) {
                return;
            }

            final long actionId = Long.parseLong(href.substring(href.lastIndexOf('/') + 1, href.indexOf('?')));
            if (currentActionId == null || currentActionId == actionId) {
                final ResponseEntity<DdiDeploymentBase> action = controllerResource
                        .getControllerBasedeploymentAction(getTenant(), getId(), actionId, -1, null);

                if (!HttpStatus.OK.equals(action.getStatusCode())) {
                    return;
                }

                final HandlingType updateType = action.getBody().getDeployment().getUpdate();
                final List<DdiChunk> modules = action.getBody().getDeployment().getChunks();

                currentActionId = actionId;
                startDdiUpdate(actionId, updateType, modules);
            }
        }
    }

    @Override
    public void updateAttribute(final String mode, final String key, final String value) {

        final DdiUpdateMode updateMode;
        switch (mode.toLowerCase()) {
        case "replace":
            updateMode = DdiUpdateMode.REPLACE;
            break;
        case "remove":
            updateMode = DdiUpdateMode.REMOVE;
            break;
        case "merge":
        default:
            updateMode = DdiUpdateMode.MERGE;
            break;
        }

        final DdiStatus status = new DdiStatus(ExecutionStatus.CLOSED, new DdiResult(FinalResult.SUCCESS, null), null);

        final DdiConfigData configData = new DdiConfigData(null, null, status, Collections.singletonMap(key, value),
                updateMode);

        controllerResource.putConfigData(configData, super.getTenant(), super.getId());
    }

    private static DmfSoftwareModule convertChunk(final DdiChunk ddi) {
        final DmfSoftwareModule converted = new DmfSoftwareModule();
        converted.setModuleVersion(ddi.getVersion());
        converted.setArtifacts(
                ddi.getArtifacts().stream().map(DDISimulatedDevice::convertArtifact).collect(Collectors.toList()));

        return converted;
    }

    private static DmfArtifact convertArtifact(final DdiArtifact ddi) {
        final DmfArtifact converted = new DmfArtifact();
        converted.setSize(ddi.getSize());
        converted.setFilename(ddi.getFilename());
        converted.setHashes(new DmfArtifactHash(ddi.getHashes().getSha1(), (ddi.getHashes().getMd5())));
        final Map<String, String> urls = new HashMap<>();

        if (ddi.getLink("download") != null) {
            urls.put("HTTPS", ddi.getLink("download").getHref());
        }

        if (ddi.getLink("download-http") != null) {
            urls.put("HTTP", ddi.getLink("download-http").getHref());
        }

        converted.setUrls(urls);

        return converted;
    }

    private void startDdiUpdate(final long actionId, final HandlingType updateType, final List<DdiChunk> modules) {

        deviceUpdater.startUpdate(getTenant(), getId(),
                modules.stream().map(DDISimulatedDevice::convertChunk).collect(Collectors.toList()), null, gatewayToken,
                sendFeedback(actionId),
                HandlingType.SKIP.equals(updateType) ? EventTopic.DOWNLOAD : EventTopic.DOWNLOAD_AND_INSTALL);
    }

    private UpdaterCallback sendFeedback(final long actionId) {
        return device -> {
            final DdiActionFeedback feedback = calculateFeedback(actionId, device);
            controllerResource.postBasedeploymentActionFeedback(feedback, getTenant(), getId(), actionId);
            currentActionId = null;
        };
    }

    private DdiActionFeedback calculateFeedback(final long actionId, final AbstractSimulatedDevice device) {
        DdiActionFeedback feedback;

        switch (device.getUpdateStatus().getResponseStatus()) {
        case SUCCESSFUL:
            feedback = new DdiActionFeedback(actionId, null, new DdiStatus(ExecutionStatus.CLOSED,
                    new DdiResult(FinalResult.SUCCESS, null), device.getUpdateStatus().getStatusMessages()));
            break;
        case ERROR:
            feedback = new DdiActionFeedback(actionId, null, new DdiStatus(ExecutionStatus.CLOSED,
                    new DdiResult(FinalResult.FAILURE, null), device.getUpdateStatus().getStatusMessages()));
            break;
        case DOWNLOADING:
            feedback = new DdiActionFeedback(actionId, null, new DdiStatus(ExecutionStatus.DOWNLOAD,
                    new DdiResult(FinalResult.NONE, null), device.getUpdateStatus().getStatusMessages()));
            break;
        case DOWNLOADED:
            feedback = new DdiActionFeedback(actionId, null, new DdiStatus(ExecutionStatus.DOWNLOADED,
                    new DdiResult(FinalResult.NONE, null), device.getUpdateStatus().getStatusMessages()));
            break;
        case RUNNING:
            feedback = new DdiActionFeedback(actionId, null, new DdiStatus(ExecutionStatus.PROCEEDING,
                    new DdiResult(FinalResult.NONE, null), device.getUpdateStatus().getStatusMessages()));
            break;
        default:
            throw new IllegalStateException("simulated device has an unknown response status + "
                    + device.getUpdateStatus().getResponseStatus());
        }
        return feedback;
    }
}
