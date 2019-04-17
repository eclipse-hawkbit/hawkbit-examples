/**
 * Copyright 2019 Google LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.google.gcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.eclipse.hawkbit.simulator.DeviceSimulatorUpdater.UpdaterCallback;
import org.eclipse.hawkbit.simulator.AbstractSimulatedDevice;
import org.eclipse.hawkbit.simulator.UpdateStatus;
import org.eclipse.hawkbit.simulator.UpdateStatus.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

public class GcpSubscriber {

	private static final BlockingQueue<PubsubMessage> messages = new LinkedBlockingDeque<>();
	private static Map<String, UpdaterCallback> mapCallbacks = new HashMap<String, UpdaterCallback>();

	private static Map<String, AbstractSimulatedDevice> mapDevices = new HashMap<String, AbstractSimulatedDevice>();

	private static Gson gson = new Gson();

	private static final Logger LOGGER = LoggerFactory.getLogger(GcpSubscriber.class);

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
				GcpOTA.PROJECT_ID, GcpOTA.SUBSCRIPTION_STATE_ID);
		Subscriber subscriber = null;
		try {
			
			// create a subscriber bound to the asynchronous message receiver
			subscriber =
					Subscriber.newBuilder(subscriptionName, new StateMessageReceiver()).setCredentialsProvider(GcpCredentials.getCredentialProvider()).build();
			subscriber.startAsync().awaitRunning();
			// Continue to listen to messages
			while (true) {
				PubsubMessage message = messages.take();
				if(!GcpOTA.FW_VIA_COMMAND) {
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
		if(message.getData().toStringUtf8().contains(GcpOTA.SUBSCRIPTION_FW_STATE)) {
			JsonObject payloadJson = gson.fromJson(message.getData()
					.toStringUtf8(), JsonObject.class);
			if(payloadJson.has(GcpOTA.SUBSCRIPTION_FW_STATE)) {
				String deviceId =  message.getAttributesMap().get(GcpOTA.DEVICE_ID);
				String fw_state = payloadJson.get(GcpOTA.SUBSCRIPTION_FW_STATE).getAsString();

				if(deviceId != null && fw_state != null) {
					UpdateStatus updateStatus = null;
					LOGGER.info("====> New state received "+fw_state+ " from device "+deviceId);
					switch (fw_state) {
					case GcpOTA.FW_MSG_RECEIVED :
						updateStatus = new UpdateStatus(ResponseStatus.RUNNING, "Message sent to initiate fw update!");
						sendUpate(deviceId, updateStatus);
						break;			
					case GcpOTA.FW_DOWNLOADING:
						updateStatus = new UpdateStatus(ResponseStatus.DOWNLOADING, "Payload downloading");
						sendUpate(deviceId, updateStatus);
						break;
					case GcpOTA.FW_INSTALLING :
						updateStatus = new UpdateStatus(ResponseStatus.DOWNLOADED, "Payload installing");
						sendUpate(deviceId, updateStatus);
						break;
					case GcpOTA.FW_INSTALLED:
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
					if(!GcpIoTHandler.atLeastOnceConnected(deviceId)) {
						LOGGER.error(deviceId+" : device was never connected");
						sendUpate(deviceId, new UpdateStatus(ResponseStatus.ERROR, "Device was never connected"));
					}
				}
			} else {
				LOGGER.info("Ignoring message");
			}
		} else {
			LOGGER.info("Ignoring message");
		}

	}


	private static String getStringFromListMap(Map<String, List<Map<String, String>>> listMap) {
		JsonObject fw_update = new JsonObject();
		JsonArray fw_update_list = new JsonArray();

		listMap.get(GcpOTA.FW_UPDATE).forEach(map -> {
			JsonObject mapJsonObject = new JsonObject();
			mapJsonObject.addProperty(GcpOTA.OBJECT_NAME, map.get(GcpOTA.OBJECT_NAME));
			mapJsonObject.addProperty(GcpOTA.URL, map.get(GcpOTA.URL));
			mapJsonObject.addProperty(GcpOTA.MD5HASH, map.get(GcpOTA.MD5HASH));
			fw_update_list.add(mapJsonObject);
		});
		fw_update.add(GcpOTA.FW_UPDATE,fw_update_list);
		return gson.toJson(fw_update);
	}



	private static void sendAsyncFwUpgradeList(String deviceId, List<DmfSoftwareModule> softwareModuleList) {
		Map<String,List<Map<String,String>>> data = 
				GcpBucketHandler.getFirmwareInfoBucket_MapList(softwareModuleList);
		if(data != null) {
			long configVersion = GcpIoTHandler.getLatestConfig(deviceId, GcpOTA.PROJECT_ID, GcpOTA.CLOUD_REGION,
					GcpOTA.REGISTRY_NAME);
			LOGGER.info("Sending Configuration Message to %s with data:\n%s", deviceId, data);
			GcpIoTHandler.setDeviceConfiguration(deviceId, GcpOTA.PROJECT_ID, GcpOTA.CLOUD_REGION,
					GcpOTA.REGISTRY_NAME, getStringFromListMap(data), configVersion);

			LOGGER.info("Writing to Firestore ");
			GcpFireStore.addDocumentMapList(deviceId
					, GcpBucketHandler.getFirmwareInfoBucket_MapList(softwareModuleList));
		}
		else LOGGER.error("Artifacts is empty for device "+deviceId);
	} 




	@SuppressWarnings("unused")
	private static void sendAsyncFwUpgrade(String deviceId, String artifactName) {
		String data = GcpBucketHandler.getFirmwareInfoBucket(artifactName);
		if(data != null) {
			long configVersion = GcpIoTHandler.getLatestConfig(deviceId, GcpOTA.PROJECT_ID, GcpOTA.CLOUD_REGION,
					GcpOTA.REGISTRY_NAME);
			LOGGER.info("Sending Configuration Message to %s with data:\n%s", deviceId, data);
			GcpIoTHandler.setDeviceConfiguration(deviceId, GcpOTA.PROJECT_ID, GcpOTA.CLOUD_REGION,
					GcpOTA.REGISTRY_NAME, data, configVersion);

			LOGGER.info("Writing to Firestore ");
			GcpFireStore.addDocument(deviceId, GcpBucketHandler.getFirmwareInfoBucket_Map(artifactName));
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

				sendAsyncFwUpgradeList(device.getId(), modules);

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
