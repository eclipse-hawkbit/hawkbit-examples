/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

/**
 * The bean of a simulated device which can be stored in the
 * {@link DeviceSimulatorRepository} or shown in the UI.
 *
 */
public abstract class AbstractSimulatedDevice {

    private String id;
    private String tenant;
    private UpdateStatus updateStatus;
    private Protocol protocol = Protocol.DMF_AMQP;
    private String targetSecurityToken;
    private int pollDelaySec;
    private int nextPollCounterSec;

    /**
     * Enum definition of the protocol to be used for the simulated device.
     * 
     */
    public enum Protocol {
        /**
         * Device Management Federation API via AMQP, push mechanism.
         */
        DMF_AMQP,
        /**
         * Direct Device Interface via HTTP, poll mechanism.
         */
        DDI_HTTP
    }

    /**
     * empty constructor.
     */
    AbstractSimulatedDevice() {

    }

    /**
     * Creates a new simulated device.
     * 
     * @param id
     *            the ID of the simulated device
     * @param tenant
     *            the tenant of the simulated device
     * @param pollDelaySec
     */
    AbstractSimulatedDevice(final String id, final String tenant, final Protocol protocol, final int pollDelaySec) {
        this.id = id;
        this.tenant = tenant;
        this.protocol = protocol;
        this.pollDelaySec = pollDelaySec;
    }

    /**
     * Can be called by a scheduler to trigger a device polling, like in real
     * scenarios devices are frequently asking for updates etc.
     */
    public abstract void poll();

    public int getPollDelaySec() {
        return pollDelaySec;
    }

    public void setPollDelaySec(final int pollDelaySec) {
        this.pollDelaySec = pollDelaySec;
    }

    public abstract void updateAttribute(final String mode, final String key, final String value);

    /**
     * Method to clean-up resource e.g. when the simulated device has been
     * removed from the repository.
     */
    public void clean() {
        this.updateStatus = null;
    }

    public String getId() {
        return id;
    }

    public String getTenant() {
        return tenant;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public UpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    public void setUpdateStatus(final UpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public int getNextPollCounterSec() {
        return nextPollCounterSec;
    }

    public void setNextPollCounterSec(final int nextPollDelayInSec) {
        this.nextPollCounterSec = nextPollDelayInSec;
    }

    public String getTargetSecurityToken() {
        return targetSecurityToken;
    }

    public void setTargetSecurityToken(final String targetSecurityToken) {
        this.targetSecurityToken = targetSecurityToken;
    }

}
