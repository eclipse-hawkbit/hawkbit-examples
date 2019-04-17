/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator;

import org.eclipse.hawkbit.dmf.json.model.DmfUpdateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.hawkbit.simulator.amqp.DmfSenderService;

/**
 * A simulated device using the DMF API of the hawkBit update server.
 */
public class DMFSimulatedDevice extends AbstractSimulatedDevice {
    private final DmfSenderService spSenderService;

	private static final Logger LOGGER = LoggerFactory.getLogger(DMFSimulatedDevice.class);

    
    /**
     * @param id
     *            the ID of the device
     * @param tenant
     *            the tenant of the simulated device
     */
    public DMFSimulatedDevice(final String id, final String tenant, final DmfSenderService spSenderService,
            final int pollDelaySec) {
        super(id, tenant, Protocol.DMF_AMQP, pollDelaySec);
        this.spSenderService = spSenderService;
        LOGGER.info(" Id: {}, tenant: {} \n", id, tenant);
    }

    @Override
    public void poll() {
    	LOGGER.info("handling event of tenant "+super.getTenant());

        spSenderService.createOrUpdateThing(super.getTenant(), super.getId());
    }

    @Override
    public void updateAttribute(final String mode, final String key, final String value) {
    	System.out.println("[DMFSimulatedDevice] handling updateAttribute");

        final DmfUpdateMode updateMode;

        switch (mode.toLowerCase()) {

            case "replace" :
                updateMode = DmfUpdateMode.REPLACE;
                break;
            case "remove" :
                updateMode = DmfUpdateMode.REMOVE;
                break;
            case "merge" :
            default :
                updateMode = DmfUpdateMode.MERGE;
                break;
        }

        spSenderService.updateAttributesOfThing(super.getTenant(), super.getId(), updateMode, key, value);
    }

}
