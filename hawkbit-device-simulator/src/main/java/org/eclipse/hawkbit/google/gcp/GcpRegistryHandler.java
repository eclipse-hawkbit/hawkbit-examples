package org.eclipse.hawkbit.google.gcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudiot.v1.CloudIot;
import com.google.api.services.cloudiot.v1.CloudIotScopes;
import com.google.api.services.cloudiot.v1.model.Device;
import com.google.api.services.cloudiot.v1.model.DeviceCredential;
import com.google.api.services.cloudiot.v1.model.DeviceRegistry;
import com.google.api.services.cloudiot.v1.model.EventNotificationConfig;
import com.google.api.services.cloudiot.v1.model.PublicKeyCredential;
import com.google.common.base.Charsets;
import com.google.common.io.Files;


public class GcpRegistryHandler {

	private static GoogleCredential getCredentialsFromFile()
	{
		GoogleCredential credential = null;
		try {
			ClassLoader classLoader = GcpRegistryHandler.class.getClassLoader();
			String path = classLoader.getResource("keys.json").getPath();
			credential = GoogleCredential.fromStream(new FileInputStream(path))
					.createScoped(CloudIotScopes.all());
		} catch (IOException e) {
			System.out.println("Please make sure to put your keys.json in the project");
		}
		return credential;
	}


	public static List<Device> getAllDevices(String projectId, String cloudRegion) throws GeneralSecurityException, IOException
	{
		List<Device> allDevices_per_project = new ArrayList<Device>();
		List<DeviceRegistry> gcp_registries = GcpRegistryHandler.listRegistries(GCP_OTA.PROJECT_ID, GCP_OTA.CLOUD_REGION);
		for(DeviceRegistry gcp_registry : gcp_registries)
		{
			allDevices_per_project.addAll(listDevices(projectId, cloudRegion, gcp_registry.getId()));
		}
		return allDevices_per_project;
	}


	/** Create a registry for Cloud IoT. */
	public static DeviceRegistry createRegistry(
			String cloudRegion, String projectId, String registryName, String pubsubTopicPath)
					throws GeneralSecurityException, IOException {
		GoogleCredential credential =
				GoogleCredential.getApplicationDefault().createScoped(CloudIotScopes.all());
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(credential);
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.build();

		final String projectPath = "projects/" + projectId + "/locations/" + cloudRegion;
		final String fullPubsubPath = "projects/" + projectId + "/topics/" + pubsubTopicPath;

		DeviceRegistry registry = new DeviceRegistry();
		EventNotificationConfig notificationConfig = new EventNotificationConfig();
		notificationConfig.setPubsubTopicName(fullPubsubPath);
		List<EventNotificationConfig> notificationConfigs = new ArrayList<EventNotificationConfig>();
		notificationConfigs.add(notificationConfig);
		registry.setEventNotificationConfigs(notificationConfigs);
		registry.setId(registryName);

		DeviceRegistry reg =
				service.projects().locations().registries().create(projectPath, registry).execute();
		System.out.println("Created registry: " + reg.getName());
		
		return reg;
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
