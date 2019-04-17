package org.eclipse.hawkbit.google.gcp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.dmf.json.model.DmfSoftwareModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;




public class GcpBucketHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GcpBucketHandler.class);

	private static Storage storage = null;
	static Gson gson = new Gson();

	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();



	private static Storage getStorage() {
		try {
			if(storage == null)
			{
				GoogleCredential credential = GcpCredentials.getCredential()
						.createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

				httpTransport = GoogleNetHttpTransport.newTrustedTransport();
				storage = new Storage.Builder(
						httpTransport,
						JSON_FACTORY,
						credential
						).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return storage;
	}

	public static void uploadFirmwareToBucket(String fileUrl, String artifactName, String targetToken) throws FileNotFoundException, IOException, GeneralSecurityException {

		Storage gcs = getStorage();
		String data = HawkBitSoftwareModuleHandler.downloadFileData(fileUrl, targetToken);
		if(!checkIfExists(artifactName))
		{
			LOGGER.info("Uploading to GCS artifact: "+artifactName);
			uploadSimple(gcs, GcpOTA.BUCKET_NAME, artifactName, data);
		}
	}

	public static String getFirmwareInfoBucket(String artifactName)
	{
		StorageObject storageObject = GcpBucketHandler.getStorageObjectInfo(artifactName);
		if(storageObject != null)
		{
			JsonObject jsonObject = new JsonObject();
			LOGGER.info(artifactName+" exists!");
			jsonObject.addProperty("ObjectName", storageObject.getName());
			jsonObject.addProperty("Url", storageObject.getMediaLink());
			jsonObject.addProperty("Md5Hash", storageObject.getMd5Hash());

			JsonObject jsonConfig = new JsonObject();
			jsonConfig.add("firmware-update", jsonObject);
			return gson.toJson(jsonConfig);
		}
		return null;
	}


	public static Map<String,Map<String,String>> getFirmwareInfoBucket_Map(String artifactName)
	{
		StorageObject storageObject = GcpBucketHandler.getStorageObjectInfo(artifactName);
		if(storageObject != null)
		{
			Map<String,Map<String,String>> fw_update = new HashMap<>(1);
			Map<String, String> mapContent = new HashMap<>(3);
			LOGGER.info(artifactName+" exists!");
			mapContent.put(GcpOTA.OBJECT_NAME, storageObject.getName());
			mapContent.put(GcpOTA.URL, storageObject.getMediaLink());
			mapContent.put(GcpOTA.MD5HASH, storageObject.getMd5Hash());
			fw_update.put(GcpOTA.FW_UPDATE, mapContent);
			return fw_update;
		}
		return null;
	}


	public static Map<String,List<Map<String,String>>> getFirmwareInfoBucket_MapList(List<DmfSoftwareModule> modules)
	{
		Map<String,List<Map<String,String>>> fw_update_Map = 
				new HashMap<String, List<Map<String,String>>>(1);

		
		List<String> fwNameList = modules.stream().flatMap(mod -> mod.getArtifacts().stream())
				.map(art -> art.getFilename())
				.collect(Collectors.toList());			

		List<Map<String,String>> list_fw_update = new ArrayList<>(fwNameList.size());
		
		fwNameList.forEach(artifactName -> {
			StorageObject storageObject = GcpBucketHandler.getStorageObjectInfo(artifactName);
			if(storageObject != null)
			{
				Map<String, String> mapContent = new HashMap<>(3);
				LOGGER.info(artifactName+" exists!");
				mapContent.put(GcpOTA.OBJECT_NAME, storageObject.getName());
				mapContent.put(GcpOTA.URL, storageObject.getMediaLink());
				mapContent.put(GcpOTA.MD5HASH, storageObject.getMd5Hash());
				list_fw_update.add(mapContent);
			}
		});
		fw_update_Map.put(GcpOTA.FW_UPDATE, list_fw_update);
		return fw_update_Map;
	}

	private static boolean checkIfExists(String artifactName) throws IOException {

		Storage.Objects.List objectsList = storage.objects().list(GcpOTA.BUCKET_NAME);
		Objects objects;
		do {
			objects = objectsList.execute();
			List<StorageObject> items = objects.getItems();
			if (items != null) {
				for (StorageObject object : items) {
					if(object.getName().equalsIgnoreCase(artifactName))
					{
						LOGGER.info(artifactName+" already exists!");
						return true;
					}
				}
			}
			objectsList.setPageToken(objects.getNextPageToken());
		} while (objects.getNextPageToken() != null);
		return false;
	}

	public static StorageObject getStorageObjectInfo(String artifactName) {
		try {
			Storage.Objects.List objectsList = getStorage().objects().list(GcpOTA.BUCKET_NAME);
			Objects objects;
			do {
				objects = objectsList.execute();
				List<StorageObject> items = objects.getItems();
				if (items != null) {
					for (StorageObject object : items) {
						if(object.getName().equalsIgnoreCase(artifactName))
						{
							LOGGER.info(artifactName+" exists!");
							return object;
						}
					}
				}
				objectsList.setPageToken(objects.getNextPageToken());
			} while (objects.getNextPageToken() != null);
			LOGGER.warn(artifactName+" not found");
		} catch (Exception e) {
		}
		return null;
	}


	private static void listBuckets() throws FileNotFoundException, IOException, GeneralSecurityException {
		ClassLoader classLoader = GcpBucketHandler.class.getClassLoader();
		String path = classLoader.getResource("keys.json").getPath();
		GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(path))
				.createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

		httpTransport = GoogleNetHttpTransport.newTrustedTransport();


		Storage storage = new Storage.Builder(
				httpTransport,
				JSON_FACTORY,
				credential
				).build();

		Storage.Buckets.List bucketsList = storage.buckets().list(GcpOTA.PROJECT_ID);
		Buckets buckets;
		do {
			buckets = bucketsList.execute();
			List<Bucket> items = buckets.getItems();
			if (items != null) {
				for (Bucket bucket: items) {
					System.out.println("[BucketHandler] BucketName : "+bucket.getName());    
				}
			}
			bucketsList.setPageToken(buckets.getNextPageToken());
		} while (buckets.getNextPageToken() != null);

		Storage.Objects.List objectsList = storage.objects().list(GcpOTA.BUCKET_NAME);
		Objects objects;
		do {
			objects = objectsList.execute();
			List<StorageObject> items = objects.getItems();
			if (items != null) {
				for (StorageObject object : items) {
					System.out.println("[BucketHandler] ObjectName: "+object.getName());
					System.out.println("[BucketHandler] MediaLink: "+object.getMediaLink());
					System.out.println("[BucketHandler] Md5Hash: "+object.getMd5Hash());
				}
			}
			objectsList.setPageToken(objects.getNextPageToken());
		} while (objects.getNextPageToken() != null);
	}

	private static StorageObject uploadSimple(Storage storage, String bucketName, String objectName,
			String data) throws UnsupportedEncodingException, IOException {
		return uploadSimple(storage, bucketName, objectName, new ByteArrayInputStream(
				data.getBytes("UTF-8")), "text/plain");
	}

	private static StorageObject uploadSimple(Storage storage, String bucketName, String objectName,
			File data) throws FileNotFoundException, IOException {
		return uploadSimple(storage, bucketName, objectName, new FileInputStream(data),
				"application/octet-stream");
	}

	private static StorageObject uploadSimple(Storage storage, String bucketName, String objectName,
			InputStream data, String contentType) throws IOException {
		InputStreamContent mediaContent = new InputStreamContent(contentType, data);
		Storage.Objects.Insert insertObject = storage.objects().insert(bucketName, null, mediaContent)
				.setName(objectName);
		// The media uploader gzips content by default, and alters the Content-Encoding accordingly.
		// GCS dutifully stores content as-uploaded. This line disables the media uploader behavior,
		// so the service stores exactly what is in the InputStream, without transformation.
		insertObject.getMediaHttpUploader().setDisableGZipContent(true);
		return insertObject.execute();
	}

	private static StorageObject uploadWithMetadata(Storage storage, StorageObject object,
			InputStream data) throws IOException {
		InputStreamContent mediaContent = new InputStreamContent(object.getContentType(), data);
		Storage.Objects.Insert insertObject = storage.objects().insert(object.getBucket(), object,
				mediaContent);
		insertObject.getMediaHttpUploader().setDisableGZipContent(true);
		return insertObject.execute();
	}
}
