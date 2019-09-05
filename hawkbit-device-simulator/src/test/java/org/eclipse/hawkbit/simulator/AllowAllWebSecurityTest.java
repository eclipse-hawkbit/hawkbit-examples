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

import org.eclipse.hawkbit.simulator.http.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;


@TestPropertySource(properties = { AuthProperties.CONFIGURATION_PREFIX + ".enabled = " + "false" })
class AllowAllWebSecurityTest extends WebSecurityTest{

    @Test
    void shouldGetOkForBaseUrl() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL)).andExpect(status().isOk());
    }

    @Test
    void shouldGetOkForStart() throws Exception {
        mockMvc.perform(get(SIMULATOR_BASE_URL_START)).andExpect(status().isOk());
    }

}
