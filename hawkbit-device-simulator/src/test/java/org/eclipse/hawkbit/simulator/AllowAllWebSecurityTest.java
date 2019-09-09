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

import io.qameta.allure.Description;
import org.eclipse.hawkbit.simulator.http.AuthProperties;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {AuthProperties.CONFIGURATION_PREFIX + ".enabled = " + "false"})
public class AllowAllWebSecurityTest extends DdiWebSecurityTest {

    @Test
    @Description("Verifies status when accessing base url - results in 200")
    public void shouldGetOkForBaseUrl() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL)).andExpect(status().isOk());
    }

    @Test
    @Description("Verifies status when creating simulated devices - results in 200")
    public void shouldGetOkForStart() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)).andExpect(status().isOk());
    }

}
