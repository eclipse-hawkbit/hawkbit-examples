/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.client.resource.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssigment;

/**
 * 
 * Builder pattern for building {@link MgmtSoftwareModuleAssigment}.
 *
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class SoftwareModuleAssigmentBuilder {

    private final List<Long> ids;

    public SoftwareModuleAssigmentBuilder() {
        ids = new ArrayList<>();
    }

    /**
     * @param id
     *            the id of the software module
     * @return the builder itself
     */
    public SoftwareModuleAssigmentBuilder id(final Long id) {
        ids.add(id);
        return this;
    }

    /**
     * Builds a list with a single entry of {@link MgmtSoftwareModuleAssigment}
     * which can directly be used in the RESTful-API.
     * 
     * @return a single entry list of {@link MgmtSoftwareModuleAssigment}
     */
    public List<MgmtSoftwareModuleAssigment> build() {
        final List<MgmtSoftwareModuleAssigment> softwareModuleAssigmentRestList = new ArrayList<>();
        for (final Long id : ids) {
            final MgmtSoftwareModuleAssigment softwareModuleAssigmentRest = new MgmtSoftwareModuleAssigment();
            softwareModuleAssigmentRest.setId(id);
            softwareModuleAssigmentRestList.add(softwareModuleAssigmentRest);
        }
        return softwareModuleAssigmentRestList;
    }

}
