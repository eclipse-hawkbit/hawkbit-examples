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

import org.eclipse.hawkbit.mgmt.rest.api.MgmtDownloadRestApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 *
 */
@FeignClient(name = "MgmtDownloadClient", url = "${hawkbit.url:localhost:8080}")
public interface MgmtDownloadClientResource extends MgmtDownloadRestApi {
}
