/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.hawkbit.ddi.client.resource.RootControllerResourceClient;
import org.eclipse.hawkbit.feign.core.client.IgnoreMultipleConsumersProducersSpringMvcContract;
import org.eclipse.hawkbit.google.gcp.GCP_OTA;
import org.eclipse.hawkbit.google.gcp.GcpRegistryHandler;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice.Protocol;
import org.eclipse.hawkbit.simulator.amqp.DmfSenderService;
import org.eclipse.hawkbit.simulator.http.GatewayTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Feign;
import feign.Logger.Level;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;

/**
 * The simulated device factory to create either {@link DMFSimulatedDevice} or
 * {@link DDISimulatedDevice}.
 */
@Service
public class SimulatedDeviceFactory {
    @Autowired
    private DeviceSimulatorUpdater deviceUpdater;

    @Autowired(required = false)
    private DmfSenderService spSenderService;

    @Autowired
    private ScheduledExecutorService threadPool;

    /**
     * Creating a simulated device.
     * 
     * @param id
     *            the ID of the simulated device
     * @param tenant
     *            the tenant of the simulated device
     * @param protocol
     *            the protocol which should be used be the simulated device
     * @param pollDelaySec
     *            the poll delay time in seconds which should be used for
     *            {@link DDISimulatedDevice}s and {@link DMFSimulatedDevice}
     * @param baseEndpoint
     *            the http base endpoint which should be used for
     *            {@link DDISimulatedDevice}s
     * @param gatewayToken
     *            the gatewayToken to be used to authenticate
     *            {@link DDISimulatedDevice}s at the endpoint
     * @return the created simulated device
     */
    public AbstractSimulatedDevice createSimulatedDevice(final String id, final String tenant, final Protocol protocol,
            final int pollDelaySec, final URL baseEndpoint, final String gatewayToken) {
        return createSimulatedDevice(id, tenant, protocol, pollDelaySec, baseEndpoint, gatewayToken, false);
    }

    private AbstractSimulatedDevice createSimulatedDevice(final String id, final String tenant, final Protocol protocol,
            final int pollDelaySec, final URL baseEndpoint, final String gatewayToken, final boolean pollImmediatly) {
        switch (protocol) {
        case DMF_AMQP:
            return createDmfDevice(id, tenant, pollDelaySec, pollImmediatly);
        case DDI_HTTP:
            return createDdiDevice(id, tenant, pollDelaySec, baseEndpoint, gatewayToken);
        default:
            throw new IllegalArgumentException("Protocol " + protocol + " unknown");
        }
    }

    private AbstractSimulatedDevice createDdiDevice(final String id, final String tenant, final int pollDelaySec,
            final URL baseEndpoint, final String gatewayToken) {

        final ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new Jackson2HalModule());

        final RootControllerResourceClient controllerResource = Feign.builder()
                .requestInterceptor(new GatewayTokenInterceptor(gatewayToken))
                .contract(new IgnoreMultipleConsumersProducersSpringMvcContract()).logLevel(Level.HEADERS)
                .decoder(new ResponseEntityDecoder(new JacksonDecoder(mapper))).encoder(new JacksonEncoder())
                .logger(new Slf4jLogger()).decode404()
                .target(RootControllerResourceClient.class, baseEndpoint.toString());

        return new DDISimulatedDevice(id, tenant, pollDelaySec, controllerResource, deviceUpdater, gatewayToken);
    }

    private AbstractSimulatedDevice createDmfDevice(final String id, final String tenant, final int pollDelaySec,
            final boolean pollImmediatly) {
        final AbstractSimulatedDevice device = new DMFSimulatedDevice(id, tenant, spSenderService, pollDelaySec);
        device.setNextPollCounterSec(pollDelaySec);
        if (pollImmediatly) {
            spSenderService.createOrUpdateThing(tenant, id);
        }

        threadPool.schedule(() -> spSenderService.updateAttributesOfThing(tenant, id), 2_000, TimeUnit.MILLISECONDS);

        return device;
    }

    /**
     * Creating a simulated device and send an immediate DMF poll to update
     * server.
     * 
     * @param id
     *            the ID of the simulated device
     * @param tenant
     *            the tenant of the simulated device
     * @param protocol
     *            the protocol which should be used be the simulated device
     * @param pollDelaySec
     *            the poll delay time in seconds which should be used for
     *            {@link DDISimulatedDevice}s and {@link DMFSimulatedDevice}
     * @param baseEndpoint
     *            the http base endpoint which should be used for
     *            {@link DDISimulatedDevice}s
     * @param gatewayToken
     *            the gatewayToken to be used to authenticate
     *            {@link DDISimulatedDevice}s at the endpoint
     * @return the created simulated device
     */
    public AbstractSimulatedDevice createSimulatedDeviceWithImmediatePoll(final String id, final String tenant,
            final Protocol protocol, final int pollDelaySec, final URL baseEndpoint, final String gatewayToken) {
        return createSimulatedDevice(id, tenant, protocol, pollDelaySec, baseEndpoint, gatewayToken, true);
    }
}
