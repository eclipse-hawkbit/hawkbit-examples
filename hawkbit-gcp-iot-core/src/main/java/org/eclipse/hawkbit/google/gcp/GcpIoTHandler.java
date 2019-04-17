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
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudiot.v1.CloudIot;
import com.google.api.services.cloudiot.v1.CloudIotScopes;
import com.google.api.services.cloudiot.v1.model.Device;
import com.google.api.services.cloudiot.v1.model.DeviceConfig;
import com.google.api.services.cloudiot.v1.model.DeviceRegistry;
import com.google.api.services.cloudiot.v1.model.DeviceState;
import com.google.api.services.cloudiot.v1.model.ListDeviceStatesResponse;
import com.google.api.services.cloudiot.v1.model.ModifyCloudToDeviceConfigRequest;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceRequest;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceResponse;


public class GcpIoTHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GcpIoTHandler.class);

	public static GoogleCredential getCredentialsFromFile()
	{
		GoogleCredential credential = null;
		credential = GcpCredentials.getCredential()
				.createScoped(CloudIotScopes.all());
		return credential;
	}


//	public static List<Device> getAllDevices(String projectId, String cloudRegion, String registryId) throws GeneralSecurityException, IOException
//	{
//		List<Device> allDevices_per_project = new ArrayList<Device>();
//		List<DeviceRegistry> gcp_registries = GcpIoTHandler.listRegistries(GcpOTA.PROJECT_ID, GcpOTA.CLOUD_REGION);
//		for(DeviceRegistry gcp_registry : gcp_registries)
//		{
//			allDevices_per_project.addAll(listDevices(projectId, cloudRegion, gcp_registry.getId()));
//		}
//		return allDevices_per_project;
//	}

	public static List<Device> listDevices(String projectId, String cloudRegion, String registryName)
			throws GeneralSecurityException, IOException {
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.build();

		final String registryPath =
				String.format(
						"projects/%s/locations/%s/registries/%s", projectId, cloudRegion, registryName);

		List<Device> devices =
				service
				.projects()
				.locations()
				.registries()
				.devices()
				.list(registryPath)
				.execute()
				.getDevices();

		if (devices != null) {
			System.out.println("Found " + devices.size() + " devices");
			for (Device d : devices) {
				System.out.println("Id: " + d.getId());
				if (d.getConfig() != null) {
					// Note that this will show the device config in Base64 encoded format.
					System.out.println("Config: " + d.getConfig().toPrettyString());
				}
				System.out.println();
			}
		} else {
			LOGGER.warn("Registry has no devices.");
			System.out.println("Registry has no devices.");
		}
		return devices;
	}

	public static boolean atLeastOnceConnected(String deviceId) {
		try {
			return atLeastOnceConnected(deviceId, 
					GcpOTA.PROJECT_ID, 
					GcpOTA.CLOUD_REGION, 
					GcpOTA.REGISTRY_NAME);
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}


	private static boolean atLeastOnceConnected(String deviceId, String projectId, String cloudRegion, String registryName)
			throws GeneralSecurityException, IOException {
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.build();

		final String deviceUniqueId =
				String.format(
						"projects/%s/locations/%s/registries/%s/devices/%s", 
						projectId, cloudRegion, registryName, deviceId);

		String lastTimeEvent =
				service
				.projects()
				.locations()
				.registries()
				.devices()
				.get(deviceUniqueId)
				.execute()
				.getLastEventTime();

		String lastHRbeat =
				service
				.projects()
				.locations()
				.registries()
				.devices()
				.get(deviceUniqueId)
				.execute()
				.getLastHeartbeatTime();
		System.out.println(lastHRbeat+" : last hear beat, lastTimeEvent "+lastTimeEvent);
		return (lastTimeEvent !=null || lastHRbeat!=null);
	}



	/**
	 * Retrieves Device Metadata
	 * @return Map of metadata
	 * */
	public static Map<String, String> getDeviceMetadata(String deviceId) {
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		CloudIot service;
		try {
			service = new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
					.build();

			final String deviceUniqueId =
					String.format(
							"projects/%s/locations/%s/registries/%s/devices/%s", 
							GcpOTA.PROJECT_ID,
							GcpOTA.CLOUD_REGION,
							GcpOTA.REGISTRY_NAME,
							deviceId);

			return 	service
					.projects()
					.locations()
					.registries()
					.devices()
					.get(deviceUniqueId)
					.execute().getMetadata();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch(GoogleJsonResponseException e) {
			e.printStackTrace();
			LOGGER.error("Couldn't find the device: "+deviceId+" in the registry");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	

	
	

	/** Lists all of the registries associated with the given project. */
	public static List<DeviceRegistry> listRegistries(String projectId, String cloudRegion)
			throws GeneralSecurityException, IOException {

		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service = new CloudIot.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init).build();

		final String projectPath = "projects/" + projectId + "/locations/" + cloudRegion;

		List<DeviceRegistry> registries =
				service
				.projects()
				.locations()
				.registries()
				.list(projectPath)
				.execute()
				.getDeviceRegistries();

		if (registries != null) {
			LOGGER.info("Found " + registries.size() + " registries");
			for (DeviceRegistry r: registries) {
				LOGGER.info("Id: " + r.getId());
				LOGGER.info("Name: " + r.getName());
				if (r.getMqttConfig() != null) {
					LOGGER.info("Config: " + r.getMqttConfig().toPrettyString());
				}
				System.out.println();
			}
		} else {
			LOGGER.warn("Project has no registries.");
		}
		return registries;

	}

	/** List all of the configs for the given device. */
	public static void listDeviceConfigs(
			String deviceId, String projectId, String cloudRegion, String registryName)
	{
		try {
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
			final CloudIot service = new CloudIot.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init).build();

			final String devicePath = String.format("projects/%s/locations/%s/registries/%s/devices/%s",
					projectId, cloudRegion, registryName, deviceId);

			LOGGER.info("Listing device configs for " + devicePath);
			List<DeviceConfig> deviceConfigs =
					service
					.projects()
					.locations()
					.registries()
					.devices()
					.configVersions()
					.list(devicePath)
					.execute()
					.getDeviceConfigs();

			for (DeviceConfig config : deviceConfigs) {
				LOGGER.info("\nConfig version: " + config.getVersion());
				LOGGER.info("Contents: " + config.getBinaryData());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/** List all of the configs for the given device. */
	public static long getLatestConfig(
			String deviceId, String projectId, String cloudRegion, String registryName)
	{
		long configVersion = 0;
		try {
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
			final CloudIot service = new CloudIot.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init).build();

			final String devicePath = String.format("projects/%s/locations/%s/registries/%s/devices/%s",
					projectId, cloudRegion, registryName, deviceId);

			LOGGER.info("Listing device configs for " + devicePath);
			List<DeviceConfig> deviceConfigs =
					service
					.projects()
					.locations()
					.registries()
					.devices()
					.configVersions()
					.list(devicePath)
					.execute()
					.getDeviceConfigs();


			for (DeviceConfig config : deviceConfigs) {
				LOGGER.info("\nConfig version: " + config.getVersion());
				LOGGER.info("Contents: " + config.getBinaryData());
				if(configVersion < config.getVersion())
				{
					configVersion = config.getVersion();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return configVersion;
	}


	/** Set a device configuration to the specified data (string, JSON) and version (0 for latest). */
	public static void setDeviceConfiguration(
			String deviceId, String projectId, String cloudRegion, String registryName,
			String data, long version)
	{
		try {
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
			CloudIot service = new CloudIot.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init).build();
			final String devicePath = String.format("projects/%s/locations/%s/registries/%s/devices/%s",
					projectId, cloudRegion, registryName, deviceId);

			ModifyCloudToDeviceConfigRequest req = new ModifyCloudToDeviceConfigRequest();
			req.setVersionToUpdate(version);

			// Data sent through the wire has to be base64 encoded.
			Base64.Encoder encoder = Base64.getEncoder();
			String encPayload = encoder.encodeToString(data.getBytes("UTF-8"));
			req.setBinaryData(encPayload);

			DeviceConfig config =
					service
					.projects()
					.locations()
					.registries()
					.devices()
					.modifyCloudToDeviceConfig(devicePath, req).execute();

			LOGGER.info("Updated: " + config.getVersion());
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}


	}

	/** Retrieves device metadata from a registry. **/
	public static List<DeviceState> getDeviceStates(
			String deviceId, String projectId, String cloudRegion, String registryName)
	{

		try {
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
			final CloudIot service = new CloudIot.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init).build();

			final String devicePath = String.format("projects/%s/locations/%s/registries/%s/devices/%s",
					projectId, cloudRegion, registryName, deviceId);

			LOGGER.info("Retrieving device states " + devicePath);

			ListDeviceStatesResponse resp  = service
					.projects()
					.locations()
					.registries()
					.devices()
					.states()
					.list(devicePath).execute();

			return resp.getDeviceStates();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void sendCommand(
			String deviceId, String projectId, String cloudRegion, String registryName, String data)
	{
		try {
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
			final CloudIot service =
					new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
					.build();

			final String devicePath =
					String.format(
							"projects/%s/locations/%s/registries/%s/devices/%s",
							projectId, cloudRegion, registryName, deviceId);

			SendCommandToDeviceRequest req = new SendCommandToDeviceRequest();

			// Data sent through the wire has to be base64 encoded.
			Base64.Encoder encoder = Base64.getEncoder();
			String encPayload = encoder.encodeToString(data.getBytes("UTF-8"));
			req.setBinaryData(encPayload);
			LOGGER.info("Sending command to %s\n", devicePath);
			SendCommandToDeviceResponse res =
					service
					.projects()
					.locations()
					.registries()
					.devices()
					.sendCommandToDevice(devicePath, req)
					.execute();

			LOGGER.info("Command response: " + res.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}  	
	}

	/** Retrieves registry metadata from a project. **/
	public static DeviceRegistry getRegistry(
			String projectId, String cloudRegion, String registryName)
					throws GeneralSecurityException, IOException {
		GoogleCredential credential =
				GoogleCredential.getApplicationDefault().createScoped(CloudIotScopes.all());
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(credential);
		final CloudIot service = new CloudIot.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init).build();

		final String registryPath = String.format("projects/%s/locations/%s/registries/%s",
				projectId, cloudRegion, registryName);

		return service.projects().locations().registries().get(registryPath).execute();
	}
}
