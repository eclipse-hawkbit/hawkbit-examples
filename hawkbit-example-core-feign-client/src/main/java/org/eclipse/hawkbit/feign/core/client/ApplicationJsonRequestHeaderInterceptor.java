/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.feign.core.client;

import org.springframework.http.MediaType;

import feign.RequestInterceptor;
import feign.RequestTemplate;


/**
 * An feign request interceptor to set the defined {@code Accept} and
 * {@code Content-Type} headers for each request to {@code application/json}.
 */
public class ApplicationJsonRequestHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(final RequestTemplate template) {
        template.header("Accept", MediaType.APPLICATION_JSON_VALUE);
        template.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }

}
