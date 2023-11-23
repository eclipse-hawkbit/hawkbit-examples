/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.simulator.http;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * A feign interceptor to apply the gateway-token header to each http-request.
 * 
 */
public class GatewayTokenInterceptor implements RequestInterceptor {

    private final String gatewayToken;

    /**
     * @param gatewayToken
     *            the gatwway token to be used in the http-header
     */
    public GatewayTokenInterceptor(final String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    @Override
    public void apply(final RequestTemplate template) {
        template.header("Authorization", "GatewayToken " + gatewayToken);
    }
}
