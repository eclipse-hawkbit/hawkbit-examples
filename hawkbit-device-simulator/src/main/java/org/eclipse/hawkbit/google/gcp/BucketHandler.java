package org.eclipse.hawkbit.google.gcp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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




public class BucketHandler {


	private static Storage storage = null;

	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

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


	private static Path getFile(String url) {
		ClassLoader classLoader = BucketHandler.class.getClassLoader();
		 URI uri;
		 Path path = null;
		try {
			uri = classLoader.getResource("logback-spring.xml").toURI();
			path = Paths.get(uri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return path;
	}
	
	private static void createFWBucket() throws FileNotFoundException, IOException, GeneralSecurityException {
		Storage gcs = getStorage();
		Stream<String> lines = Files.lines(getFile(null));
	    String data = lines.collect(Collectors.joining("\n"));
	    lines.close();
		System.out.println("[BucketHander] adding file to the bucket "+data);
	
		uploadSimple(gcs, GCP_OTA.BUCKET_NAME, "newfirmware",data);
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
					System.out.println(bucket.getName());    
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
					System.out.println(" ObjectName"+object.getName());
				}
			}
			objectsList.setPageToken(objects.getNextPageToken());
		} while (objects.getNextPageToken() != null);
	}
	
	public static StorageObject uploadSimple(Storage storage, String bucketName, String objectName,
		      String data) throws UnsupportedEncodingException, IOException {
		    return uploadSimple(storage, bucketName, objectName, new ByteArrayInputStream(
		        data.getBytes("UTF-8")), "text/plain");
		  }
		  
		  public static StorageObject uploadSimple(Storage storage, String bucketName, String objectName,
		      File data) throws FileNotFoundException, IOException {
		    return uploadSimple(storage, bucketName, objectName, new FileInputStream(data),
		        "application/octet-stream");
		  }

		  public static StorageObject uploadSimple(Storage storage, String bucketName, String objectName,
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
		  
		  public static StorageObject uploadWithMetadata(Storage storage, StorageObject object,
		      InputStream data) throws IOException {
		    InputStreamContent mediaContent = new InputStreamContent(object.getContentType(), data);
		    Storage.Objects.Insert insertObject = storage.objects().insert(object.getBucket(), object,
		        mediaContent);
		    insertObject.getMediaHttpUploader().setDisableGZipContent(true);
		    return insertObject.execute();
		  }
	
	
	public static void init() throws FileNotFoundException, IOException, GeneralSecurityException {
		createFWBucket();
	}


}
