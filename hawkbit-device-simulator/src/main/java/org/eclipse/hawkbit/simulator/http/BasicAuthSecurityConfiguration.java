/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring security configuration to grant access for the configured in-memory user.
 */
@Configuration
@EnableConfigurationProperties({BasicAuthProperties.class})
@ConditionalOnProperty(name = BasicAuthProperties.CONFIGURATION_ENABLED_PROPERTY, havingValue = "true")
public class BasicAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private BasicAuthProperties basicAuthProperties;

    protected BasicAuthSecurityConfiguration(BasicAuthProperties basicAuthProperties) {
        this.basicAuthProperties = basicAuthProperties;
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser(basicAuthProperties.getUser())
                .password(passwordEncoder().encode(basicAuthProperties.getPassword()))
                .roles("ADMIN");
    }

    @Override
    protected void configure(final HttpSecurity httpSec) throws Exception {
        httpSec.csrf().disable().authorizeRequests()
                .antMatchers("/").permitAll()
                .anyRequest().authenticated()
                .and().httpBasic()
                .and().exceptionHandling()
                .authenticationEntryPoint((request, response, e) -> {
                    response.addHeader("WWW-Authenticate", "Basic realm=\"Device Simulator\"");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write(HttpStatus.UNAUTHORIZED.getReasonPhrase());
                });
    }

    @Bean
    protected PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
