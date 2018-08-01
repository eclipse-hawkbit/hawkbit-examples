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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DmfActionStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfActionUpdateStatus;
import org.eclipse.hawkbit.dmf.json.model.DmfAttributeUpdate;
import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.eclipse.hawkbit.simulator.SimulationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;

/**
 * Sender service to send messages to update server.
 */
public class DmfSenderService extends MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmfSenderService.class);

    private final String spExchange;

    private final SimulationProperties simulationProperties;

    /**
     *
     * @param rabbitTemplate
     *            the rabbit template
     * @param amqpProperties
     *            the amqp properties
     * @param simulationProperties
     *            for attributes update class
     */
    DmfSenderService(final RabbitTemplate rabbitTemplate, final AmqpProperties amqpProperties,
            final SimulationProperties simulationProperties) {
        super(rabbitTemplate, amqpProperties);
        spExchange = AmqpSettings.DMF_EXCHANGE;
        this.simulationProperties = simulationProperties;
    }

    public void ping(final String tenant, final String correlationId) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put(MessageHeaderKey.TENANT, tenant);
        messageProperties.getHeaders().put(MessageHeaderKey.TYPE, MessageType.PING.toString());
        messageProperties.setCorrelationId(correlationId.getBytes());
        messageProperties.setReplyTo(amqpProperties.getSenderForSpExchange());
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);

        sendMessage(spExchange, new Message(null, messageProperties));
    }

    /**
     * Finish the update process. This will send a action status to SP.
     *
     * @param update
     *            the simulated update object
     * @param updateResultMessages
     *            a description according the update process
     * @param actionType
     *            indicating whether to download and install or skip
     *            installation due to maintenance window.
     */
    public void finishUpdateProcess(final SimulatedUpdate update, final List<String> updateResultMessages) {
        final Message updateResultMessage = createUpdateResultMessage(update, DmfActionStatus.FINISHED,
                updateResultMessages);
        sendMessage(spExchange, updateResultMessage);
    }

    /**
     * Finish update process with error and send error to SP.
     *
     * @param update
     *            the simulated update object
     * @param updateResultMessages
     *            list of messages for error
     */
    public void finishUpdateProcessWithError(final SimulatedUpdate update, final List<String> updateResultMessages) {
        sendErrorgMessage(update, updateResultMessages);
        LOGGER.debug("Update process finished with error \"{}\" reported by thing {}", updateResultMessages,
                update.getThingId());
    }

    /**
     * Send a message if the message is not null.
     *
     * @param address
     *            the exchange name
     * @param message
     *            the amqp message which will be send if its not null
     */
    public void sendMessage(final String address, final Message message) {
        if (message == null) {
            return;
        }
        message.getMessageProperties().getHeaders().remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);

        final String correlationId = UUID.randomUUID().toString();

        if (isCorrelationIdEmpty(message)) {
            message.getMessageProperties().setCorrelationId(correlationId.getBytes(StandardCharsets.UTF_8));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending message {} to exchange {} with correlationId {}", message, address, correlationId);
        } else {
            LOGGER.debug("Sending message to exchange {} with correlationId {}", address, correlationId);
        }

        rabbitTemplate.send(address, null, message, new CorrelationData(correlationId));
    }

    private static boolean isCorrelationIdEmpty(final Message message) {
        return message.getMessageProperties().getCorrelationId() == null
                || message.getMessageProperties().getCorrelationId().length <= 0;
    }

    /**
     * Convert object and message properties to message.
     *
     * @param object
     *            to get converted
     * @param messageProperties
     *            to get converted
     * @return converted message
     */
    public Message convertMessage(final Object object, final MessageProperties messageProperties) {
        return rabbitTemplate.getMessageConverter().toMessage(object, messageProperties);
    }

    /**
     * Send an error message to SP.
     *
     * @param tenant
     *            the tenant
     * @param updateResultMessages
     *            the error message description to send
     * @param actionId
     *            the ID of the action for the error message
     */
    public void sendErrorMessage(final String tenant, final List<String> updateResultMessages, final Long actionId) {
        final Message message = createActionStatusMessage(tenant, DmfActionStatus.ERROR, updateResultMessages,
                actionId);
        sendMessage(spExchange, message);
    }

    /**
     * Send a warning message to SP.
     *
     * @param update
     *            the simulated update object
     * @param updateResultMessages
     *            a warning description
     */
    public void sendWarningMessage(final SimulatedUpdate update, final List<String> updateResultMessages) {
        final Message message = createActionStatusMessage(update, updateResultMessages, DmfActionStatus.WARNING);
        sendMessage(spExchange, message);
    }

    /**
     * Method to send a action status to SP.
     *
     * @param tenant
     *            the tenant
     * @param actionStatus
     *            the action status
     * @param updateResultMessages
     *            the message to get send
     * @param actionId
     *            the cached value
     */
    public void sendActionStatusMessage(final String tenant, final DmfActionStatus actionStatus,
            final List<String> updateResultMessages, final Long actionId) {
        final Message message = createActionStatusMessage(tenant, actionStatus, updateResultMessages, actionId);
        sendMessage(message);

    }

    /**
     * Create new thing created message and send to update server.
     *
     * @param tenant
     *            the tenant to create the target
     * @param targetId
     *            the ID of the target to create or update
     */
    public void createOrUpdateThing(final String tenant, final String targetId) {
        sendMessage(spExchange, thingCreatedMessage(tenant, targetId));

        LOGGER.debug("Created thing created message and send to update server for Thing \"{}\"", targetId);
    }

    /**
     * Create new attribute update message and send to update server.
     *
     * @param tenant
     *            the tenant to create the target
     * @param targetId
     *            the ID of the target to create or update
     */
    public void updateAttributesOfThing(final String tenant, final String targetId) {
        sendMessage(spExchange, updateAttributes(tenant, targetId, DmfUpdateMode.MERGE,
                simulationProperties.getAttributes().stream().collect(Collectors
                        .toMap(SimulationProperties.Attribute::getKey, SimulationProperties.Attribute::getValue))));

        LOGGER.debug("Create update attributes message and send to update server for Thing \"{}\"", targetId);
    }

    /**
     * Create new attribute update message for specific attribute and send to
     * update server
     *
     * @param tenant
     *            the tenant to create the target
     * @param targetId
     *            the ID of the target to create or update
     * @param mode
     *            the update mode ('merge', 'replace', or 'remove')
     * @param key
     *            the key of the attribute
     * @param value
     *            the value of the attribute
     */
    public void updateAttributesOfThing(final String tenant, final String targetId, final DmfUpdateMode mode,
            final String key, final String value) {
        sendMessage(spExchange, updateAttributes(tenant, targetId, mode, Collections.singletonMap(key, value)));
    }

    private Message thingCreatedMessage(final String tenant, final String targetId) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, MessageType.THING_CREATED.name());
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, targetId);
        messagePropertiesForSP.setHeader(MessageHeaderKey.SENDER, "simulator");
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(amqpProperties.getSenderForSpExchange());
        return new Message(null, messagePropertiesForSP);
    }

    private MessageProperties createAttributeUpdateMessage(final String tenant, final String targetId) {
        final MessageProperties messagePropertiesForSP = new MessageProperties();
        messagePropertiesForSP.setHeader(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        messagePropertiesForSP.setHeader(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ATTRIBUTES);
        messagePropertiesForSP.setHeader(MessageHeaderKey.TENANT, tenant);
        messagePropertiesForSP.setHeader(MessageHeaderKey.THING_ID, targetId);
        messagePropertiesForSP.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messagePropertiesForSP.setReplyTo(amqpProperties.getSenderForSpExchange());
        return messagePropertiesForSP;
    }

    private Message updateAttributes(final String tenant, final String targetId, final DmfUpdateMode mode,
            final Map<String, String> attributes) {
        final MessageProperties messagePropertiesForSP = createAttributeUpdateMessage(tenant, targetId);
        final DmfAttributeUpdate attributeUpdate = new DmfAttributeUpdate();
        attributeUpdate.setMode(mode);
        attributeUpdate.getAttributes().putAll(attributes);

        return convertMessage(attributeUpdate, messagePropertiesForSP);
    }

    /**
     * Send a created message to SP.
     *
     * @param message
     *            the message to get send
     */
    private void sendMessage(final Message message) {
        sendMessage(spExchange, message);
    }

    /**
     * Send error message to SP.
     *
     * @param context
     *            the current context
     * @param updateResultMessages
     *            a list of descriptions according the update process
     */
    private void sendErrorgMessage(final SimulatedUpdate update, final List<String> updateResultMessages) {
        final Message message = createActionStatusMessage(update, updateResultMessages, DmfActionStatus.ERROR);
        sendMessage(spExchange, message);
    }

    /**
     * Create a action status message.
     *
     * @param actionStatus
     *            the ActionStatus
     * @param actionMessage
     *            the message description
     * @param actionId
     *            the action id
     * @param cacheValue
     *            the cacheValue value
     */
    private Message createActionStatusMessage(final String tenant, final DmfActionStatus actionStatus,
            final List<String> updateResultMessages, final Long actionId) {
        final MessageProperties messageProperties = new MessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(actionId, actionStatus);
        headers.put(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        headers.put(MessageHeaderKey.TENANT, tenant);
        headers.put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        headers.put(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        actionUpdateStatus.addMessage(updateResultMessages);

        return convertMessage(actionUpdateStatus, messageProperties);
    }

    private Message createUpdateResultMessage(final SimulatedUpdate cacheValue, final DmfActionStatus actionStatus,
            final List<String> updateResultMessages) {
        final MessageProperties messageProperties = new MessageProperties();
        final Map<String, Object> headers = messageProperties.getHeaders();
        final DmfActionUpdateStatus actionUpdateStatus = new DmfActionUpdateStatus(cacheValue.getActionId(),
                actionStatus);
        headers.put(MessageHeaderKey.TYPE, MessageType.EVENT.name());
        headers.put(MessageHeaderKey.TENANT, cacheValue.getTenant());
        headers.put(MessageHeaderKey.TOPIC, EventTopic.UPDATE_ACTION_STATUS.name());
        headers.put(MessageHeaderKey.CONTENT_TYPE, MessageProperties.CONTENT_TYPE_JSON);
        actionUpdateStatus.addMessage(updateResultMessages);
        return convertMessage(actionUpdateStatus, messageProperties);
    }

    private Message createActionStatusMessage(final SimulatedUpdate update, final List<String> updateResultMessages,
            final DmfActionStatus status) {
        return createActionStatusMessage(update.getTenant(), status, updateResultMessages, update.getActionId());
    }

}
