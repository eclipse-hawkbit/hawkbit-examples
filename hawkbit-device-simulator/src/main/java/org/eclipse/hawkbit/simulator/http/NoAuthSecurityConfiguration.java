/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.simulator.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring security configuration.
 * Depending on the ~.auth.enabled property access is granted for all requests.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = BasicAuthProperties.CONFIGURATION_ENABLED_PROPERTY, havingValue = "false", matchIfMissing = true)
public class NoAuthSecurityConfiguration {

    /**
     * Spring security configuration to grant access for all incoming requests.
     * Default configuration, if ~.auth.enabled is false or not set
     */
    @Bean
    protected SecurityFilterChain filterChainNoAuth(final HttpSecurity httpSec) throws Exception {
        return httpSec
                .authorizeRequests(amrmRegistry -> amrmRegistry.requestMatchers("/**").permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
