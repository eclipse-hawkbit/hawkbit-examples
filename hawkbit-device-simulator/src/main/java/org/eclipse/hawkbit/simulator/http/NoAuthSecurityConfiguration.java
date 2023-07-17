/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
                .authorizeRequests(amrmRegistry -> amrmRegistry.antMatchers("/**").permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
