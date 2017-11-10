/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.amqp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Object for holding attributes for a simulated update for the device
 * simulator.
 */
public class SimulatedUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String tenant;
    private final String thingId;
    private final Long actionId;
    private transient LocalDateTime startCacheTime;

    SimulatedUpdate(final String tenant, final String thingId, final Long actionId) {
        this.tenant = tenant;
        this.thingId = thingId;
        this.actionId = actionId;
        this.startCacheTime = LocalDateTime.now();
    }

    public String getTenant() {
        return tenant;
    }

    public String getThingId() {
        return thingId;
    }

    public Long getActionId() {
        return actionId;
    }

    public LocalDateTime getStartCacheTime() {
        return startCacheTime;
    }
}
