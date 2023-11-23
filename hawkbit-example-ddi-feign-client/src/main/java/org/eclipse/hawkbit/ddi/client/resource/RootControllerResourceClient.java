/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.client.resource;

import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Client binding for the Rootcontroller resource of the DDI API.
 */
@FeignClient(name = "RootControllerClient", url = "${hawkbit.url:localhost:8080}")
public interface RootControllerResourceClient extends DdiRootControllerRestApi {

}
