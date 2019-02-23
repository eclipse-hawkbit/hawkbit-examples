package org.eclipse.hawkbit.google.gcp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

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




public class BucketHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BucketHandler.class);

	private static Storage storage = null;

	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static Gson gson = new Gson(); 
	
	
	private static Storage getStorage() throws FileNotFoundException, IOException, GeneralSecurityException {
		if(storage == null)
		{
			ClassLoader classLoader = BucketHandler.class.getClassLoader();
			String path = classLoader.getResource("keys.json").getPath();
			GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(path))
					.createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			storage = new Storage.Builder(
					httpTransport,
					JSON_FACTORY,
					credential
					).build();
		}
		return storage;
	}


	private static String getFile(String urlString, String fileName) {
		String data = null;
		try {
			URLConnection uc = new URL(urlString).openConnection();
			String userpass = "admin:admin"; //FIXME: this is bad !
			String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
			uc.setRequestProperty ("Authorization", basicAuth);
			InputStream inputStream = uc.getInputStream();
			Scanner scanner = new Scanner(inputStream, "UTF-8");
			scanner.useDelimiter("\\A").next();
			scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	public static void uploadFirmwareToBucket(String fileUrl, String artifactName, String targetToken) throws FileNotFoundException, IOException, GeneralSecurityException {

		listBuckets();
		Storage gcs = getStorage();
		String data = HawkbitSoftwareModuleHandler.downloadFileData(fileUrl, targetToken);
		if(!checkIfExists(artifactName))
		{
			LOGGER.info("Uploading to GCS artifact: "+artifactName);
			uploadSimple(gcs, GCP_OTA.BUCKET_NAME, artifactName, data);
		}
	}

	private static boolean checkIfExists(String artifactName) throws IOException {

		Storage.Objects.List objectsList = storage.objects().list(GCP_OTA.BUCKET_NAME);
		Objects objects;
		do {
			objects = objectsList.execute();
			List<StorageObject> items = objects.getItems();
			if (items != null) {
				for (StorageObject object : items) {
					if(object.getName().equalsIgnoreCase(artifactName))
					{
						LOGGER.debug(artifactName+" already exists!");
						return true;
					}
				}
			}
			objectsList.setPageToken(objects.getNextPageToken());
		} while (objects.getNextPageToken() != null);
		return false;
	}

	private static String getStorageObjectInfo(String artifactName) throws IOException {
		Storage.Objects.List objectsList = storage.objects().list(GCP_OTA.BUCKET_NAME);
		Objects objects;
		do {
			objects = objectsList.execute();
			List<StorageObject> items = objects.getItems();
			if (items != null) {
				for (StorageObject object : items) {
					if(object.getName().equalsIgnoreCase(artifactName))
					{
						JsonObject jsonObject = new JsonObject();
						LOGGER.debug(artifactName+" exists!");
						jsonObject.addProperty("ObjectName", object.getName());
						jsonObject.addProperty("Url", object.getMediaLink());
						jsonObject.addProperty("Md5Hash", object.getMd5Hash());
						return gson.toJson(jsonObject);
					}
				}
			}
			objectsList.setPageToken(objects.getNextPageToken());
		} while (objects.getNextPageToken() != null);
		LOGGER.warn(artifactName+" not found");
		return null;
	}


		private static void listBuckets() throws FileNotFoundException, IOException, GeneralSecurityException {
			ClassLoader classLoader = BucketHandler.class.getClassLoader();
			String path = classLoader.getResource("keys.json").getPath();
			GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(path))
					.createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

			httpTransport = GoogleNetHttpTransport.newTrustedTransport();


			Storage storage = new Storage.Builder(
					httpTransport,
					JSON_FACTORY,
					credential
					).build();

			Storage.Buckets.List bucketsList = storage.buckets().list(GCP_OTA.PROJECT_ID);
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

			Storage.Objects.List objectsList = storage.objects().list(GCP_OTA.BUCKET_NAME);
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