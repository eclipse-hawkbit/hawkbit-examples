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

import org.eclipse.hawkbit.mgmt.json.model.artifact.MgmtArtifact;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import feign.Param;

/**
 * Client binding for the SoftwareModule resource of the management API.
 */
@FeignClient(name = "MgmtSoftwareModuleClient", url = "${hawkbit.url:localhost:8080}")
public interface MgmtSoftwareModuleClientResource extends MgmtSoftwareModuleRestApi {

    @Override
    @PostMapping(value = MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "/{softwareModuleId}/artifacts")
    ResponseEntity<MgmtArtifact> uploadArtifact(@PathVariable("softwareModuleId") final Long softwareModuleId,
            @Param("file") final MultipartFile file,
            @RequestParam(value = "filename", required = false) final String optionalFileName,
            @RequestParam(value = "md5sum", required = false) final String md5Sum,
            @RequestParam(value = "sha1sum", required = false) final String sha1Sum,
            @RequestParam(value = "sha256sum", required = false) final String sha256sum);
}
