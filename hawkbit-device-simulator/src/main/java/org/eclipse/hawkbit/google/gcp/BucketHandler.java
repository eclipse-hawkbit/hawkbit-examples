package org.eclipse.hawkbit.google.gcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;



public class BucketHandler {

	
    private static HttpTransport httpTransport;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	
	public static void init() throws FileNotFoundException, IOException, GeneralSecurityException {
		
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
                     System.out.println(object.getName());
                 }
             }
             objectsList.setPageToken(objects.getNextPageToken());
         } while (objects.getNextPageToken() != null);

		
	}
	
	
}
