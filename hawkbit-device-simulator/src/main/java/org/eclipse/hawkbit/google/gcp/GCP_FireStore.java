package org.eclipse.hawkbit.google.gcp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.core.ApiFuture;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.storage.model.StorageObject;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;

public class GCP_FireStore {

	private static Firestore db;

	public static void init() {

		try {
			ClassLoader classLoader = GCPBucketHandler.class.getClassLoader();
			String path = classLoader.getResource("keys.json").getPath();
			GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(path))
					.createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));
			FirestoreOptions firestoreOptions =
					FirestoreOptions.newBuilder().setTimestampsInSnapshotsEnabled(true)
					.setProjectId(GCP_OTA.PROJECT_ID).setCredentials(credentials)
					.build();
			db = firestoreOptions.getService();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void addDocument(String deviceId, StorageObject storageObject) {


		try {
			DocumentReference docRef = db.collection(GCP_OTA.FIRESTORE_STATE_COLLECTION).document(deviceId);
			Map<String, Object> data = new HashMap<>();
			data.put("first", "Ada");
			data.put("last", "Lovelace");
			data.put("born", 1815);
			//asynchronously write data
			ApiFuture<WriteResult> result = docRef.set(data);
			System.out.println("Update time : " + result.get().getUpdateTime());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
