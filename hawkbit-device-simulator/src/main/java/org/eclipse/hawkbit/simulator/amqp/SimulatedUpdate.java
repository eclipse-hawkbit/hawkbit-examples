/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    /**
     * @param tenant
     *          the tenant for this thing and for this simulated update
     * @param thingId
     *          the thing id that this simulated update correlates to
     * @param actionId
     *          the id of the action related to this simulated update
     */
    public SimulatedUpdate(final String tenant, final String thingId, final Long actionId) {
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
