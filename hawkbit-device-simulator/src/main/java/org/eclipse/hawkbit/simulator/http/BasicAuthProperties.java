/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Basic authentication properties.
 *
 */
@ConfigurationProperties(prefix = BasicAuthProperties.CONFIGURATION_PREFIX)
public class BasicAuthProperties {

    /**
     * The prefix for this configuration.
     */
    public static final String CONFIGURATION_PREFIX = "hawkbit.device.simulator.auth";

    /**
     * Indicates if basic auth is enabled for the device simulator.
     */
    private boolean enabled;

    /**
     * The set username for basic auth
     */
    private String user;

    /**
     * The set password for basic auth
     */
    private String password;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
