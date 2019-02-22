package org.eclipse.hawkbit.google.gcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudiot.v1.CloudIot;
import com.google.api.services.cloudiot.v1.CloudIotScopes;
import com.google.api.services.cloudiot.v1.model.BindDeviceToGatewayRequest;
import com.google.api.services.cloudiot.v1.model.BindDeviceToGatewayResponse;
import com.google.api.services.cloudiot.v1.model.Device;
import com.google.api.services.cloudiot.v1.model.DeviceCredential;
import com.google.api.services.cloudiot.v1.model.DeviceRegistry;
import com.google.api.services.cloudiot.v1.model.EventNotificationConfig;
import com.google.api.services.cloudiot.v1.model.PublicKeyCredential;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceRequest;
import com.google.api.services.cloudiot.v1.model.SendCommandToDeviceResponse;
import com.google.api.services.cloudiot.v1.model.UnbindDeviceFromGatewayRequest;
import com.google.api.services.cloudiot.v1.model.UnbindDeviceFromGatewayResponse;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.util.Base64;



public class GCP_IoTHandler {

	public static GoogleCredential getCredentialsFromFile()
	{
		GoogleCredential credential = null;
		try {
			ClassLoader classLoader = GCP_IoTHandler.class.getClassLoader();
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
		List<DeviceRegistry> gcp_registries = GCP_IoTHandler.listRegistries(GCP_OTA.PROJECT_ID, GCP_OTA.CLOUD_REGION);
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



	/** Create a device to bind to a gateway. */
	public static void createDevice(
			String projectId, String cloudRegion, String registryName, String deviceId)
					throws GeneralSecurityException, IOException {
		// [START create_device]
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
				.setFieldMask("config,gatewayConfig")
				.execute()
				.getDevices();

		if (devices != null) {
			System.out.println("Found " + devices.size() + " devices");
			for (Device d : devices) {
				if ((d.getId() != null && d.getId().equals(deviceId))
						|| (d.getName() != null && d.getName().equals(deviceId))) {
					System.out.println("Device exists, skipping.");
					return;
				}
			}
		}
	}


	public static void bindDeviceToGateway(
			String projectId, String cloudRegion, String registryName, String deviceId, String gatewayId)
					throws GeneralSecurityException, IOException {
		// [START bind_device_to_gateway]
		createDevice(projectId, cloudRegion, registryName, deviceId);

		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.build();

		final String registryPath =
				String.format(
						"projects/%s/locations/%s/registries/%s", projectId, cloudRegion, registryName);

		BindDeviceToGatewayRequest request = new BindDeviceToGatewayRequest();
		request.setDeviceId(deviceId);
		request.setGatewayId(gatewayId);

		BindDeviceToGatewayResponse response =
				service
				.projects()
				.locations()
				.registries()
				.bindDeviceToGateway(registryPath, request)
				.execute();

		System.out.println(String.format("Device bound: %s", response.toPrettyString()));
		// [END bind_device_to_gateway]
	}

	public static void unbindDeviceFromGateway(
			String projectId, String cloudRegion, String registryName, String deviceId, String gatewayId)
					throws GeneralSecurityException, IOException {
		// [START unbind_device_from_gateway]
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpRequestInitializer init = new RetryHttpInitializerWrapper(getCredentialsFromFile());
		final CloudIot service =
				new CloudIot.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, init)
				.build();

		final String registryPath =
				String.format(
						"projects/%s/locations/%s/registries/%s", projectId, cloudRegion, registryName);

		UnbindDeviceFromGatewayRequest request = new UnbindDeviceFromGatewayRequest();
		request.setDeviceId(deviceId);
		request.setGatewayId(gatewayId);

		UnbindDeviceFromGatewayResponse response =
				service
				.projects()
				.locations()
				.registries()
				.unbindDeviceFromGateway(registryPath, request)
				.execute();

		System.out.println(String.format("Device unbound: %s", response.toPrettyString()));
		// [END unbind_device_from_gateway]
	}

	public static void attachDeviceToGateway(MqttClient client, String deviceId)
			throws MqttException {
		// [START attach_device]
		final String attachTopic = String.format("/devices/%s/attach", deviceId);
		System.out.println(String.format("Attaching: %s", attachTopic));
		String attachPayload = "{}";
		MqttMessage message = new MqttMessage(attachPayload.getBytes());
		message.setQos(1);
		client.publish(attachTopic, message);
		// [END attach_device]
	}

	/** Detaches a bound device from the Gateway. */
	public static void detachDeviceFromGateway(MqttClient client, String deviceId)
			throws MqttException {
		// [START detach_device]
		final String detachTopic = String.format("/devices/%s/detach", deviceId);
		System.out.println(String.format("Detaching: %s", detachTopic));
		String attachPayload = "{}";
		MqttMessage message = new MqttMessage(attachPayload.getBytes());
		message.setQos(1);
		client.publish(detachTopic, message);
		// [END detach_device]
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
	
	
	/*  @SuppressWarnings("deprecation")
	  public static String uploadFile(Part filePart, final String bucketName) throws IOException {
	    DateTimeFormatter dtf = DateTimeFormat.forPattern("-YYYY-MM-dd-HHmmssSSS");
	    DateTime dt = DateTime.now(DateTimeZone.UTC);
	    String dtString = dt.toString(dtf);
	    final String fileName = filePart.getSubmittedFileName() + dtString;

	    // the inputstream is closed by default, so we don't need to close it here
	    BlobInfo blobInfo =
	        storage.create(
	            BlobInfo
	                .newBuilder(bucketName, fileName)
	                // Modify access list to allow all users with link to read file
	                .setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.READER))))
	                .build(),
	            filePart.getInputStream());
	    // return the public download link
	    return blobInfo.getMediaLink();
	  }*/

	public static void sendCommand(
		      String deviceId, String projectId, String cloudRegion, String registryName, String data)
		      throws GeneralSecurityException, IOException {
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
