/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfDownloadAndUpdateRequest;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;
import org.eclipse.hawkbit.simulator.DeviceSimulatorRepository;
import org.eclipse.hawkbit.simulator.DeviceSimulatorUpdater;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Handle all incoming Messages from hawkBit update server.
 *
 */
public class DmfReceiverService extends MessageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmfReceiverService.class);

    private final DmfSenderService spSenderService;

    private final DeviceSimulatorUpdater deviceUpdater;

    private final DeviceSimulatorRepository repository;

    private final Set<String> openPings = new ConcurrentHashSet<>();

    /**
     * Constructor.
     * 
     * @param rabbitTemplate
     *            for sending messages
     * @param amqpProperties
     *            for amqp configuration
     * @param spSenderService
     *            to send messages
     * @param deviceUpdater
     *            simulator service for updates
     * @param repository
     *            to manage simulated devices
     */
    DmfReceiverService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties,
            final DmfSenderService spSenderService, final DeviceSimulatorUpdater deviceUpdater,
            final DeviceSimulatorRepository repository) {
        super(rabbitTemplate, amqpProperties);
        this.spSenderService = spSenderService;
        this.deviceUpdater = deviceUpdater;
        this.repository = repository;
    }

    /**
     * Method to validate if content type is set in the message properties.
     *
     * @param message
     *            the message to get validated
     */
    private void checkContentTypeJson(final Message message) {
        if (message.getBody().length == 0) {
            return;
        }
        final MessageProperties messageProperties = message.getMessageProperties();
        final String headerContentType = (String) messageProperties.getHeaders().get("content-type");
        if (null != headerContentType) {
            messageProperties.setContentType(headerContentType);
        }
        final String contentType = messageProperties.getContentType();
        if (contentType != null && contentType.contains("json")) {
            return;
        }
        throw new AmqpRejectAndDontRequeueException("Content-Type is not JSON compatible");
    }

    /**
     * Handle the incoming Message from Queue with the property
     * (hawkbit.device.simulator.amqp.receiverConnectorQueueFromSp).
     *
     * @param message
     *            the incoming message
     * @param type
     *            the action type
     * @param thingId
     *            the thing id in message header
     * @param tenant
     *            the device belongs to
     */
    @RabbitListener(queues = "${hawkbit.device.simulator.amqp.receiverConnectorQueueFromSp}")
    public void recieveMessageSp(final Message message, @Header(MessageHeaderKey.TYPE) final String type,
            @Header(name = MessageHeaderKey.THING_ID, required = false) final String thingId,
            @Header(MessageHeaderKey.TENANT) final String tenant) {
        final MessageType messageType = MessageType.valueOf(type);

        if (MessageType.EVENT.equals(messageType)) {
            checkContentTypeJson(message);
            handleEventMessage(message, thingId);
            return;
        }

        if (MessageType.THING_DELETED.equals(messageType)) {
            checkContentTypeJson(message);
            repository.remove(tenant, thingId);
            return;
        }

        if (MessageType.PING_RESPONSE.equals(messageType)) {
            final String correlationId = new String(message.getMessageProperties().getCorrelationId(),
                    StandardCharsets.UTF_8);
            if (!openPings.remove(correlationId)) {
                LOGGER.error("Unknown PING_RESPONSE received for correlationId: {}.", correlationId);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Got ping response from tenant {} with correlationId {} with timestamp {}", tenant,
                        correlationId, new String(message.getBody(), StandardCharsets.UTF_8));
            }

            return;
        }

        LOGGER.info("No valid message type property.");
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    void checkDmfHealth() {
        if (!amqpProperties.isCheckDmfHealth()) {
            return;
        }

        if (openPings.size() > 5) {
            LOGGER.error("Currently {} open pings! DMF does not seem to be reachable.", openPings.size());
        } else {
            LOGGER.debug("Currently {} open pings", openPings.size());
        }

        repository.getTenants().forEach(tenant -> {
            final String correlationId = UUID.randomUUID().toString();
            spSenderService.ping(tenant, correlationId);
            openPings.add(correlationId);
            LOGGER.debug("Ping tenant {} with correlationId {}", tenant, correlationId);
        });
    }

    private void handleEventMessage(final Message message, final String thingId) {
        final Object eventHeader = message.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC);
        if (eventHeader == null) {
            logAndThrowMessageError(message, "Event Topic is not set");
        }
        // Exception squid:S2259 - Checked before
        @SuppressWarnings({ "squid:S2259" })
        final EventTopic eventTopic = EventTopic.valueOf(eventHeader.toString());
        switch (eventTopic) {
        case DOWNLOAD_AND_INSTALL:
        case DOWNLOAD:
            handleUpdateProcess(message, thingId, eventTopic);
            break;
        case CANCEL_DOWNLOAD:
            handleCancelDownloadAction(message, thingId);
            break;
        case REQUEST_ATTRIBUTES_UPDATE:
            handleAttributeUpdateRequest(message, thingId);
            break;
        default:
            LOGGER.info("No valid event property.");
            break;
        }
    }

    private void handleAttributeUpdateRequest(final Message message, final String thingId) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final String tenant = (String) headers.get(MessageHeaderKey.TENANT);

        spSenderService.updateAttributesOfThing(tenant, thingId);
    }

    private void handleCancelDownloadAction(final Message message, final String thingId) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final String tenant = (String) headers.get(MessageHeaderKey.TENANT);
        final Long actionId = convertMessage(message, Long.class);

        final SimulatedUpdate update = new SimulatedUpdate(tenant, thingId, actionId);
        spSenderService.finishUpdateProcess(update, Arrays.asList("Simulation canceled"));
    }

    private void handleUpdateProcess(final Message message, final String thingId, final EventTopic actionType) {
        final MessageProperties messageProperties = message.getMessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final String tenant = (String) headers.get(MessageHeaderKey.TENANT);

        final DmfDownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(message,
                DmfDownloadAndUpdateRequest.class);
        final Long actionId = downloadAndUpdateRequest.getActionId();
        final String targetSecurityToken = downloadAndUpdateRequest.getTargetSecurityToken();

        deviceUpdater.startUpdate(tenant, thingId, actionId, null, downloadAndUpdateRequest.getSoftwareModules(),
                targetSecurityToken, null, device -> sendFeedback(actionId, device), actionType);
    }

    private void sendFeedback(final Long actionId, final AbstractSimulatedDevice device) {
        switch (device.getUpdateStatus().getResponseStatus()) {
        case SUCCESSFUL:
            spSenderService.finishUpdateProcess(new SimulatedUpdate(device.getTenant(), device.getId(), actionId),
                    device.getUpdateStatus().getStatusMessages());
            break;
        case ERROR:
            spSenderService.finishUpdateProcessWithError(
                    new SimulatedUpdate(device.getTenant(), device.getId(), actionId),
                    device.getUpdateStatus().getStatusMessages());
            break;
        case DOWNLOADING:
            spSenderService.sendActionStatusMessage(device.getTenant(), DmfActionStatus.DOWNLOAD,
                    device.getUpdateStatus().getStatusMessages(), actionId);
            break;
        case DOWNLOADED:
            spSenderService.sendActionStatusMessage(device.getTenant(), DmfActionStatus.DOWNLOADED,
                    device.getUpdateStatus().getStatusMessages(), actionId);
            break;
        case RUNNING:
            spSenderService.sendActionStatusMessage(device.getTenant(), DmfActionStatus.RUNNING,
                    device.getUpdateStatus().getStatusMessages(), actionId);
            break;
        default:
            break;
        }
    }
}
