/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import org.eclipse.hawkbit.simulator.http.AuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring security configuration.
 * Depending on the ~.auth.enabled property either basic authentication is enabled or access is granted for all requests.
 */
// Exception squid:S1118 - Spring configuration classes need public constructor
@SuppressWarnings({ "squid:S1118" })
@EnableWebSecurity
public class WebSecurityConfiguration {

    private static final String ENABLED = "enabled";

    /**
     * Spring security configuration to grant access for all incoming requests.
     */
    @Configuration
    @ConditionalOnProperty(prefix = AuthProperties.CONFIGURATION_PREFIX, name = ENABLED, havingValue = "false")
    public static class AllowAllWebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(final HttpSecurity httpSec) throws Exception {
            httpSec.csrf().disable().authorizeRequests().antMatchers("/**").permitAll().anyRequest().authenticated();
        }
    }

    /**
     * Spring security configuration to grant access for the configured in-memory user.
     */
    @Configuration
    @EnableConfigurationProperties({AuthProperties.class})
    @ConditionalOnProperty(prefix = AuthProperties.CONFIGURATION_PREFIX, name = ENABLED, havingValue = "true")
    public static class AuthenticatedWebSecurityConfig extends WebSecurityConfigurerAdapter {

        private AuthProperties authProperties;

        public AuthenticatedWebSecurityConfig(AuthProperties authProperties){
            this.authProperties = authProperties;
        }

        @Override
        protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    .withUser(authProperties.getUser())
                    .password(passwordEncoder().encode(authProperties.getPassword()))
                    .roles("ADMIN");
        }

        @Override
        protected void configure(final HttpSecurity httpSec) throws Exception {
            httpSec.csrf().disable().authorizeRequests().anyRequest().authenticated()
                    .and().httpBasic()
                    .and().exceptionHandling()
                     .authenticationEntryPoint((request, response, e) -> {
                        response.addHeader("WWW-Authenticate", "Basic realm=\"Device Simulator\"");
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.getWriter().write(HttpStatus.UNAUTHORIZED.getReasonPhrase());
                    });
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

}
