/**
 * Copyright 2019 Google LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.google.gcp;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public class HawkBitSoftwareModuleHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(HawkBitSoftwareModuleHandler.class);


	private static CloseableHttpClient createHttpClientThatAcceptsAllServerCerts()
			throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		final SSLContextBuilder builder = SSLContextBuilder.create();
		builder.loadTrustMaterial(null, (chain, authType) -> true);
		final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
		return HttpClients.custom().setSSLSocketFactory(sslsf).build();
	}



	protected static String downloadFileData(final String url,final String targetToken)
			throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

		final CloseableHttpClient httpclient = createHttpClientThatAcceptsAllServerCerts();
		final HttpGet request = new HttpGet(url);

		if (!StringUtils.isEmpty(targetToken)) {
			request.addHeader(HttpHeaders.AUTHORIZATION, "TargetToken " + targetToken);
		}

		try (final CloseableHttpResponse response = httpclient.execute(request)) {

			if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
				String message = "download "+url+" failed (" + response.getStatusLine().getStatusCode()+ ")";
				LOGGER.error(message);
				return null;
			}
			String payload = null;
			try {
				InputStream is = response.getEntity().getContent();
				payload = new String(ByteStreams.toByteArray(is),Charsets.UTF_8);
				System.out.println("Payload ==========> "+payload);	
			} catch (Exception e) {
				e.printStackTrace();
			}
			return payload;
		}
	}


}
