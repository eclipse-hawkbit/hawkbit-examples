/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.simulator.http.BasicAuthProperties;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@Feature("Component Tests - Hawkbit Device Simulator")
@Story("Web Security Test, Basic Authentication")
@TestPropertySource(properties = {BasicAuthProperties.CONFIGURATION_ENABLED_PROPERTY + " = true"})
public class AuthenticatedWebSecurityTest extends DdiWebSecurityTest {

    @Autowired
    private BasicAuthProperties basicAuthProperties;

    @Test
    @Description("Verifies status when accessing base url - results in 200")
    public void shouldGetOkForBaseUrl() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL)).andExpect(status().isOk());
    }

    @Test
    @Description("Verifies status when creating simulated devices - results in 401")
    public void shouldGetUnauthorizedForStart() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)).andExpect(status().isUnauthorized());
    }

    @Test
    @Description("Verifies status when creating simulated devices as authorized user - results in 200")
    public void shouldGetOkForAuthenticatedUser() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)
                .with(httpBasic(basicAuthProperties.getUser(), basicAuthProperties.getPassword())))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Verifies status when creating simulated devices with wrong credentials - results in 401")
    public void shouldGetUnauthorizedForNotExistingUser() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)
                .with(httpBasic(basicAuthProperties.getUser() + "random", basicAuthProperties.getPassword())))
                .andExpect(status().isUnauthorized());
    }

}
