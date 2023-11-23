/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.client.resource;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Client binding for the Target filter query resource of the management API.
 */
@FeignClient(name = "MgmtTargetFilterQueryClient", url = "${hawkbit.url:localhost:8080}")
public interface MgmtTargetFilterQueryClientResource extends MgmtTargetFilterQueryRestApi {
}
