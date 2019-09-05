/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

import org.eclipse.hawkbit.simulator.http.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;


@TestPropertySource(properties = { AuthProperties.CONFIGURATION_PREFIX + ".enabled = " + "true" })
class AuthenticatedWebSecurityTest extends WebSecurityTest{

    @Autowired
    private AuthProperties authProperties;

    @Test
    void shouldGetUnauthorizedForBaseUrl() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetUnauthorizedForStart() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetOkForAuthenticatedUser() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)
                .with(httpBasic(authProperties.getUser(), authProperties.getPassword())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetUnauthorizedForNotExistingUser() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)
                .with(httpBasic(authProperties.getUser() + "random", authProperties.getPassword())))
                .andExpect(status().isUnauthorized());
    }

}
