package org.eclipse.hawkbit.google.gcp;

public class GcpOTA {

	//TODO: Configurations to take outside of here same for keys.json
	public final static String PROJECT_ID = "ota-iot-231619";
	public final static String CLOUD_REGION = "us-central1";
	public final static String REGISTRY_NAME = "OTA-DeviceRegistry";
	public final static String BUCKET_NAME = "ota-iot-231619.appspot.com";
	
//	public final static String PROJECT_ID = "ikea-homesmart-workshop";
//	public final static String CLOUD_REGION = "europe-west1";
//	public final static String REGISTRY_NAME = "tradfri";
//	public final static String BUCKET_NAME = "ikea-homesmart-workshop.appspot.com"; //TODO:
	
	public final static String FW_MSG_RECEIVED = "msg-received";
	public final static String FW_INSTALLING = "installing";
	public final static String FW_DOWNLOADING = "downloading";
	public final static String FW_INSTALLED = "installed";
	
	public final static String SUBSCRIPTION_STATE_ID = "state";
	public final static String SUBSCRIPTION_FW_STATE = "fw-state";
	public final static String DEVICE_ID = "deviceId";
	
	
	public final static String FIRESTORE_DEVICES_COLLECTION = "devices";
	public final static String FIRESTORE_CONFIG_COLLECTION = "config";
	public final static String FW_UPDATE = "firmware-update";
	
	public final static String OBJECT_NAME = "ObjectName";
	public final static String URL = "Url";
	public final static String MD5HASH = "Md5Hash";
	
	public final static boolean FW_VIA_COMMAND = false;
}

