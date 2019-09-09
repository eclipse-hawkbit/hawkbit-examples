/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.simulator.amqp.AmqpProperties;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@Feature("Unit Tests - Web Security")
@Story("Hawkbit Device Simulator")
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {AmqpProperties.CONFIGURATION_PREFIX + ".enabled = " + "false"})
public abstract class DdiWebSecurityTest {

    @Autowired
    protected MockMvc mockMvc;

    static final String SIMULATOR_BASE_URL = "/";
    static final String SIMULATOR_BASE_URL_START = SIMULATOR_BASE_URL + "start?amount=5&api=ddi";

}
