package org.eclipse.hawkbit.google.gcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.google.api.services.cloudiot.v1.model.DeviceCredential;
import com.google.api.services.cloudiot.v1.model.DeviceRegistry;
import com.google.api.services.cloudiot.v1.model.DeviceState;
import com.google.api.services.cloudiot.v1.model.EventNotificationConfig;
import com.google.api.services.cloudiot.v1.model.ListDeviceStatesResponse;
import com.google.api.services.cloudiot.v1.model.ModifyCloudToDeviceConfigRequest;
import com.google.api.services.cloudiot.v1.model.PublicKeyCredential;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceRequest;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceResponse;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

//TODO: how to make this multi-registries?
//allowing getting config and setting config for all
//also firestore might break since devices id are unique per registry
public class GcpIoTHandler {


	private static final Logger LOGGER = LoggerFactory.getLogger(GcpIoTHandler.class);

	public static GoogleCredential getCredentialsFromFile(){
		GoogleCredential credential = null;
		try {
			ClassLoader classLoader = GcpIoTHandler.class.getClassLoader();
			String path = classLoader.getResource("keys.json").getPath();
			credential = GoogleCredential.fromStream(new FileInputStream(path))
					.createScoped(CloudIotScopes.all());
		} catch (IOException e) {
			System.out.println("Please make sure to put your keys.json in the project");
		}
		return credential;
	}


	public static List<Device> getAllDevices(String projectId, String cloudRegion) throws GeneralSecurityException, IOException{
		List<Device> allDevices_per_project = new ArrayList<Device>();
		List<DeviceRegistry> gcp_registries = GcpIoTHandler.listRegistries(GcpOTA.PROJECT_ID, GcpOTA.CLOUD_REGION);
		for(DeviceRegistry gcp_registry : gcp_registries)
		{
			allDevices_per_project.addAll(listDevices(projectId, cloudRegion, gcp_registry.getId()));
		}
		return allDevices_per_project;
	}

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
			System.out.println("Found " + registries.size() + " registries");
			for (DeviceRegistry r: registries) {
				System.out.println("Id: " + r.getId());
				System.out.println("Name: " + r.getName());
				if (r.getMqttConfig() != null) {
					System.out.println("Config: " + r.getMqttConfig().toPrettyString());
				}
				System.out.println();
			}
		} else {
			System.out.println("Project has no registries.");
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

			System.out.println("Listing device configs for " + devicePath);
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
				System.out.println("Config version: " + config.getVersion());
				System.out.println("Contents: " + config.getBinaryData());
				System.out.println();
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

			System.out.println("Listing device configs for " + devicePath);
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
				System.out.println("Config version: " + config.getVersion());
				System.out.println("Contents: " + config.getBinaryData());
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

			System.out.println("Updated: " + config.getVersion());
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

			System.out.println("Retrieving device states " + devicePath);

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
			System.out.printf("Sending command to %s\n", devicePath);
			SendCommandToDeviceResponse res =
					service
					.projects()
					.locations()
					.registries()
					.devices()
					.sendCommandToDevice(devicePath, req)
					.execute();

			System.out.println("Command response: " + res.toString());

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

	public static Device createDeviceWithRs256(
			String deviceId,
			String certificateFilePath,
			String projectId,
			String cloudRegion,
			String registryName)
					throws GeneralSecurityException, IOException {
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.build();

		final String registryPath =
				String.format(
						"projects/%s/locations/%s/registries/%s", projectId, cloudRegion, registryName);

		PublicKeyCredential publicKeyCredential = new PublicKeyCredential();
		String key = Files.asCharSource(new File(certificateFilePath), Charsets.UTF_8).read();
		publicKeyCredential.setKey(key);
		publicKeyCredential.setFormat("RSA_X509_PEM");

		DeviceCredential devCredential = new DeviceCredential();
		devCredential.setPublicKey(publicKeyCredential);

		System.out.println("Creating device with id: " + deviceId);
		Device device = new Device();
		device.setId(deviceId);
		device.setCredentials(Arrays.asList(devCredential));
		Device createdDevice =
				service
				.projects()
				.locations()
				.registries()
				.devices()
				.create(registryPath, device)
				.execute();

		System.out.println("Created device: " + createdDevice.toPrettyString());
		return createdDevice;
	}

}
