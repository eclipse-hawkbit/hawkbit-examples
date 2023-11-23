/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.simulator;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.hawkbit.simulator.amqp.AmqpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Execution of operations after startup. Set up of simulations.
 *
 */
@Component
@ConditionalOnProperty(prefix = "hawkbit.device.simulator", name = "autostart", matchIfMissing = true)
public class SimulatorStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorStartup.class);

    @Autowired
    private SimulationProperties simulationProperties;

    @Autowired
    private DeviceSimulatorRepository repository;

    @Autowired
    private SimulatedDeviceFactory deviceFactory;

    @Autowired
    private AmqpProperties amqpProperties;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        LOGGER.debug("{} autostarts will be executed", simulationProperties.getAutostarts().size());

        simulationProperties.getAutostarts().forEach(autostart -> {
            LOGGER.debug("Autostart runs for tenant {} and API {}", autostart.getTenant(), autostart.getApi());
            for (int i = 0; i < autostart.getAmount(); i++) {
                final String deviceId = autostart.getName() + i;
                try {
                    if (amqpProperties.isEnabled()) {
                        repository.add(deviceFactory.createSimulatedDeviceWithImmediatePoll(deviceId,
                                autostart.getTenant(), autostart.getApi(), autostart.getPollDelay(),
                                new URL(autostart.getEndpoint()), autostart.getGatewayToken()));
                    }

                } catch (final MalformedURLException e) {
                    LOGGER.error("Creation of simulated device at startup failed.", e);
                }
            }
        });
    }

}
