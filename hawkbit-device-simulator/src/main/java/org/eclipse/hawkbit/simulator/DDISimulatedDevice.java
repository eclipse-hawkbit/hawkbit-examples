/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiConfirmationFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
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
import org.springframework.hateoas.Link;
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

    private static final String DEPLOYMENT_BASE_LINK = "deploymentBase";

    private static final String CONFIRMATION_BASE_LINK = "confirmationBase";

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
            final Optional<Link> confirmationBaseLink = getRequiredLink(CONFIRMATION_BASE_LINK);
            if (confirmationBaseLink.isPresent()) {
                sendConfirmationFeedback(getActionId(confirmationBaseLink.get()));
            } else {
                getRequiredLink(DEPLOYMENT_BASE_LINK).flatMap(this::getActionWithDeployment).ifPresent(actionWithDeployment -> {
                    final Long actionId = actionWithDeployment.getKey();
                    final DdiDeployment deployment = actionWithDeployment.getValue().getDeployment();
                    final HandlingType updateType = deployment.getUpdate();
                    final List<DdiChunk> modules = deployment.getChunks();

                    currentActionId = actionId;
                    startDdiUpdate(actionId, updateType, modules);
                });
            }
        }
    }

    private Optional<Link> getRequiredLink(final String nameOfTheLink) {
        ResponseEntity<DdiControllerBase> poll = null;
        try {
            poll = controllerResource.getControllerBase(getTenant(), getId());
        } catch (final RuntimeException ex) {
            LOGGER.error("Failed base poll", ex);
            return Optional.empty();
        }

        if (HttpStatus.OK != poll.getStatusCode()) {
            return Optional.empty();
        }

        final DdiControllerBase pollBody = poll.getBody();
        return pollBody != null ? pollBody.getLink(nameOfTheLink) : Optional.empty();
    }

    private Optional<Entry<Long, DdiDeploymentBase>> getActionWithDeployment(final Link deploymentBaseLink) {
        final long actionId = getActionId(deploymentBaseLink);
        if (currentActionId == null || currentActionId == actionId) {
            final ResponseEntity<DdiDeploymentBase> action = controllerResource
                    .getControllerBasedeploymentAction(getTenant(), getId(), actionId, -1, null);

            if (HttpStatus.OK != action.getStatusCode()) {
                return Optional.empty();
            }

            if (action.getBody() != null) {
                return Optional.of(new AbstractMap.SimpleEntry<>(actionId, action.getBody()));
            }
        }

        return Optional.empty();
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

        final DdiConfigData configData = new DdiConfigData(Collections.singletonMap(key, value), updateMode);

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
        ddi.getLink("download").ifPresent(link -> urls.put("HTTPS", link.getHref()));
        ddi.getLink("download-http").ifPresent(link -> urls.put("HTTP", link.getHref()));
        converted.setUrls(urls);

        return converted;
    }

    private void startDdiUpdate(final long actionId, final HandlingType updateType, final List<DdiChunk> modules) {

        deviceUpdater.startUpdate(getTenant(), getId(),
                modules.stream().map(DDISimulatedDevice::convertChunk).collect(Collectors.toList()), null, gatewayToken,
                sendFeedback(actionId),
                HandlingType.SKIP == updateType ? EventTopic.DOWNLOAD : EventTopic.DOWNLOAD_AND_INSTALL);
    }

    private UpdaterCallback sendFeedback(final long actionId) {
        return device -> {
            final DdiActionFeedback feedback = calculateFeedback(device);
            controllerResource.postBasedeploymentActionFeedback(feedback, getTenant(), getId(), actionId);
            currentActionId = null;
        };
    }

    private void sendConfirmationFeedback(final long actionId) {
        final DdiConfirmationFeedback ddiConfirmationFeedback = new DdiConfirmationFeedback(
                DdiConfirmationFeedback.Confirmation.CONFIRMED, 0, Collections.singletonList(
                "the confirmation status for the device is" + DdiConfirmationFeedback.Confirmation.CONFIRMED));
        controllerResource.postConfirmationActionFeedback(ddiConfirmationFeedback, getTenant(), getId(), actionId);
    }

    private DdiActionFeedback calculateFeedback(final AbstractSimulatedDevice device) {
        DdiActionFeedback feedback;

        switch (device.getUpdateStatus().getResponseStatus()) {
        case SUCCESSFUL:
            feedback = new DdiActionFeedback(null, new DdiStatus(ExecutionStatus.CLOSED,
                    new DdiResult(FinalResult.SUCCESS, null), 200, device.getUpdateStatus().getStatusMessages()));
            break;
        case ERROR:
            feedback = new DdiActionFeedback(null, new DdiStatus(ExecutionStatus.CLOSED,
                    new DdiResult(FinalResult.FAILURE, null), null, device.getUpdateStatus().getStatusMessages()));
            break;
        case DOWNLOADING:
            feedback = new DdiActionFeedback(null, new DdiStatus(ExecutionStatus.DOWNLOAD,
                    new DdiResult(FinalResult.NONE, null), null, device.getUpdateStatus().getStatusMessages()));
            break;
        case DOWNLOADED:
            feedback = new DdiActionFeedback(null, new DdiStatus(ExecutionStatus.DOWNLOADED,
                    new DdiResult(FinalResult.NONE, null), null, device.getUpdateStatus().getStatusMessages()));
            break;
        case RUNNING:
            feedback = new DdiActionFeedback(null, new DdiStatus(ExecutionStatus.PROCEEDING,
                    new DdiResult(FinalResult.NONE, null), null, device.getUpdateStatus().getStatusMessages()));
            break;
        default:
            throw new IllegalStateException("simulated device has an unknown response status + "
                    + device.getUpdateStatus().getResponseStatus());
        }
        return feedback;
    }

    private long getActionId(final Link link) {
        final String href = link.getHref();
        return Long.parseLong(href.substring(href.lastIndexOf('/') + 1, href.indexOf('?')));
    }
}
