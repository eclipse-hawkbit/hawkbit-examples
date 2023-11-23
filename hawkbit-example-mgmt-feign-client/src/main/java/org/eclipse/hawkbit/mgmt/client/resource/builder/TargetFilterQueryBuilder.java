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

import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQueryRequestBody;

/**
 * Builder pattern for building {@link MgmtTargetFilterQueryRequestBody}.
 */
// Exception squid:S1701 - builder pattern
@SuppressWarnings({ "squid:S1701" })
public class TargetFilterQueryBuilder {

    private String name;
    private String query;

    /**
     * @param name
     *            the name of the filter
     * @return the builder itself
     */
    public TargetFilterQueryBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param query
     *            the query filter
     * @return the builder itself
     */
    public TargetFilterQueryBuilder query(final String query) {
        this.query = query;
        return this;
    }

    /**
     * Builds a single entry of {@link MgmtTargetFilterQueryRequestBody} which
     * can directly be used to post on the RESTful-API.
     * 
     * @return a single entry of {@link MgmtTargetFilterQueryRequestBody}
     */
    public MgmtTargetFilterQueryRequestBody build() {
        final MgmtTargetFilterQueryRequestBody body = new MgmtTargetFilterQueryRequestBody();
        body.setName(name);
        body.setQuery(query);
        return body;
    }
}
