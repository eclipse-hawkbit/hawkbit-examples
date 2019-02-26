package org.eclipse.hawkbit.google.gcp;




import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;
import org.eclipse.hawkbit.simulator.DeviceSimulatorUpdater.UpdaterCallback;
import org.eclipse.hawkbit.simulator.UpdateStatus;
import org.eclipse.hawkbit.simulator.UpdateStatus.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

public class GCP_Subscriber {

	private static final BlockingQueue<PubsubMessage> messages = new LinkedBlockingDeque<>();
	private static Map<String, UpdaterCallback> mapCallbacks = new HashMap<String, UpdaterCallback>();

	private static Map<String, AbstractSimulatedDevice> mapDevices = new HashMap<String, AbstractSimulatedDevice>();

	private static Gson gson = new Gson();

	private static final Logger LOGGER = LoggerFactory.getLogger(GCP_Subscriber.class);

	static class StateMessageReceiver implements MessageReceiver {

		@Override
		public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
			messages.offer(message);
			consumer.ack();
		}
	}

	/** Receive messages over a subscription. */
	public static void init() {
		ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(
				GCP_OTA.PROJECT_ID, GCP_OTA.SUBSCRIPTION_STATE_ID);
		Subscriber subscriber = null;
		try {
			// create a subscriber bound to the asynchronous message receiver
			subscriber =
					Subscriber.newBuilder(subscriptionName, new StateMessageReceiver()).build();
			subscriber.startAsync().awaitRunning();
			// Continue to listen to messages
			while (true) {
				PubsubMessage message = messages.take();
				System.out.println("Message Id: " + message.getMessageId());
				//{"deviceId":"CharbelDevice","fw-state":"installed"}
				System.out.println("Data: " + message.getData().toStringUtf8());
				if(!GCP_OTA.FW_VIA_COMMAND) {
					updateHawkbitStatus(message);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (subscriber != null) {
				subscriber.stopAsync();
			}
		}
	}



	public static void updateHawkbitStatus(PubsubMessage message){
		JsonObject payloadJson = gson.fromJson(message.getData()
				.toStringUtf8(), JsonObject.class);
		if(payloadJson.has("fw-state") && payloadJson.has("deviceId")) {
			String deviceId = payloadJson.get("deviceId").getAsString();
			String fw_state = payloadJson.get("fw-state").getAsString();

			if(deviceId != null && fw_state != null) {
				UpdateStatus updateStatus = null;

				switch (fw_state) {
				case "msg-received" :
					updateStatus = new UpdateStatus(ResponseStatus.RUNNING, "Message sent to initiate fw update!");
					sendUpate(deviceId, updateStatus);
					break;			
				case "installing" :
					updateStatus = new UpdateStatus(ResponseStatus.DOWNLOADED, "Payload installing");
					sendUpate(deviceId, updateStatus);
					break;
				case "downloading" :
					updateStatus = new UpdateStatus(ResponseStatus.DOWNLOADING, "Payload downloading");
					sendUpate(deviceId, updateStatus);
					break;
				case "installed":
					updateStatus = new UpdateStatus(ResponseStatus.SUCCESSFUL, "Payload installed");
					sendUpate(deviceId, updateStatus);

					//remove device and callback
					mapCallbacks.remove(deviceId);
					mapDevices.remove(deviceId);

					break;
				default: 
					LOGGER.error("Unknown fw-state: "+fw_state);
					updateStatus = new UpdateStatus(ResponseStatus.ERROR, "Unknown State");
					sendUpate(deviceId, updateStatus);
					break;
				}

			} else {
				LOGGER.error("state: %s, deviceId %s", fw_state, deviceId);

				//Device never connected
				if(!GCP_IoTHandler.atLeastOnceConnected(deviceId)) {
					LOGGER.error(deviceId+" : device was never connected");
					sendUpate(deviceId, new UpdateStatus(ResponseStatus.ERROR, "Device was never connected"));
				}
			}
		} else {
			LOGGER.debug("Ignoring message");
		}

	}

	private static void sendAsyncFwUpgrade(String deviceId, String artifactName) {
		String data = GCPBucketHandler.getFirmwareInfoBucket(artifactName);
		if(data != null) {
			long configVersion = GCP_IoTHandler.getLatestConfig(deviceId, GCP_OTA.PROJECT_ID, GCP_OTA.CLOUD_REGION,
					GCP_OTA.REGISTRY_NAME);
			LOGGER.info("Sending Configuration Message to %s with data:\n%s", deviceId, data);
			GCP_IoTHandler.setDeviceConfiguration(deviceId, GCP_OTA.PROJECT_ID, GCP_OTA.CLOUD_REGION,
					GCP_OTA.REGISTRY_NAME, data, configVersion);

			LOGGER.info("Writing to Firestore ");
			GCP_FireStore.addDocument(deviceId, GCPBucketHandler.getFirmwareInfoBucket_Map(artifactName));
		}
		else LOGGER.error(artifactName+" not found in bucket for device "+deviceId);
	}  


	private static void sendUpate(String deviceId, UpdateStatus updateStatus) {
		AbstractSimulatedDevice device = mapDevices.get(deviceId);
		UpdaterCallback callback = mapCallbacks.get(deviceId);
		if(device != null && callback != null) {
			device.setUpdateStatus(updateStatus);
			callback.sendFeedback(device);
		} else {
			if(device == null) {
				LOGGER.error("Map didnt find device on "+ updateStatus.getResponseStatus().toString());
			} 
			if(callback == null) {
				LOGGER.error("Map didnt find callback on "+ updateStatus.getResponseStatus().toString());
			}
		}
	}

	public static void updateDevice(AbstractSimulatedDevice device, UpdaterCallback callback,
			List<DmfSoftwareModule> modules, EventTopic actionType) {

		LOGGER.info("Update device with eventTopic: "+actionType);

		//if the device is still updating, wait until it is finished
		if (actionType == EventTopic.DOWNLOAD_AND_INSTALL 
				|| actionType == EventTopic.DOWNLOAD) {
			if(!mapDevices.containsKey(device.getId())) {
				LOGGER.info("ActionType "+actionType);

				List<String> fwNameList = modules.stream().flatMap(mod -> mod.getArtifacts().stream())
						.map(art -> art.getFilename())
						.collect(Collectors.toList());

				fwNameList.forEach(fw -> sendAsyncFwUpgrade(device.getId(), fw));

				mapCallbacks.put(device.getId(), callback);
				mapDevices.put(device.getId(), device);
			} else {
				LOGGER.error("Device ID already exist on actionType: "+actionType);
				sendUpate(device.getId(), new UpdateStatus(ResponseStatus.RUNNING, "Payload Reached"));
			}
		}
		else {
			LOGGER.error("Unsupported actionType: "+actionType);
			sendUpate(device.getId(), new UpdateStatus(ResponseStatus.ERROR, "Unsupported Action"));

		}
	}
}
