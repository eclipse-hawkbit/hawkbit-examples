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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

public class GcpCredentials {


	private static Path keysFile = null;
	private static GoogleCredentials googleCredentials;
	private static GoogleCredential googleCredential;
	private static CredentialsProvider credentialsProvider;

	private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentials.class);

	protected static GoogleCredential getCredential() {
		if(googleCredential == null) {
			try {
				googleCredential = GoogleCredential.fromStream(Files.newInputStream(keysFile));
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Please make sure to put your keys.json in the project");
				LOGGER.error("Please make sure to put your keys.json in the project");
			}
		}
		return googleCredential;
	}


	protected static CredentialsProvider getCredentialProvider() {
		if(credentialsProvider == null) {
			try {
				credentialsProvider =  FixedCredentialsProvider.create(
						ServiceAccountCredentials.fromStream(Files.newInputStream(keysFile)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return credentialsProvider;
	}

	protected static GoogleCredentials getCredentials() {
		if(googleCredentials == null) {
			try {
				googleCredentials = GoogleCredentials.fromStream(Files.newInputStream(keysFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return googleCredentials;
	}

	public static void setKeysFilePath(String keysPath) {
		LOGGER.info("==========> Setting keys path to "+keysPath);
		keysFile = Paths.get(keysPath);
		LOGGER.info("==========> Setting keys path to "+keysFile.toString());
		int n;
		try (InputStream in = Files.newInputStream(keysFile)) {
			while ((n = in.read()) != -1) {
				System.out.print((char) n);
			}
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}
		LOGGER.info("============================================================ ");

		getCredentials();
		getCredential();
	}

}
