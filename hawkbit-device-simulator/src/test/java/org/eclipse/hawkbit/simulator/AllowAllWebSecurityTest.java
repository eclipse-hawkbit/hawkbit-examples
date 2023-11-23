/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.simulator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.simulator.http.BasicAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@Feature("Component Tests - Hawkbit Device Simulator")
@Story("Web Security Test, Allow All Requests")
@TestPropertySource(properties = {BasicAuthProperties.CONFIGURATION_ENABLED_PROPERTY + " = false"})
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
