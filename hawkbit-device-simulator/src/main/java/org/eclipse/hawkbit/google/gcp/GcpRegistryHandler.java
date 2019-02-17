package org.eclipse.hawkbit.google.gcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudiot.v1.CloudIot;
import com.google.api.services.cloudiot.v1.CloudIotScopes;
import com.google.api.services.cloudiot.v1.model.Device;
import com.google.api.services.cloudiot.v1.model.DeviceRegistry;



public class GcpRegistryHandler {

	final static String APP_NAME = "ota-iot-231619";


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


	public static void listDevices(String projectId, String cloudRegion, String registryName)
			throws GeneralSecurityException, IOException {
		GoogleCredential credential =
				GoogleCredential.getApplicationDefault().createScoped(CloudIotScopes.all());
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(credential);
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.setApplicationName(APP_NAME)
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
	}

	/** Lists all of the registries associated with the given project. */
	public static void listRegistries(String projectId, String cloudRegion)
			throws GeneralSecurityException, IOException {



		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service = new CloudIot.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init)
				.setApplicationName(APP_NAME).build();

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
				GoogleNetHttpTransport.newTrustedTransport(),jsonFactory, init)
				.setApplicationName(APP_NAME).build();

		final String registryPath = String.format("projects/%s/locations/%s/registries/%s",
				projectId, cloudRegion, registryName);

		return service.projects().locations().registries().get(registryPath).execute();
	}
}
