package org.eclipse.hawkbit.google.gcp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

public class GCP_FireStore {

	private static Firestore db;

	public static void init() {

		try {
			//			ClassLoader classLoader = GCPBucketHandler.class.getClassLoader();
			//			String path = classLoader.getResource("firestoreKeys.json").getPath();
			GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

			//			GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(path))
			//					.createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));
			//			
			FirebaseOptions options = new FirebaseOptions.Builder()
					.setCredentials(credentials)
					.setProjectId(GCP_OTA.PROJECT_ID)
					.build();
			FirebaseApp.initializeApp(options);

			db = FirestoreClient.getFirestore();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void addDocumentMapList(String deviceId, Map<String, List<Map<String, String>>> mapList) {
		try {
			DocumentReference docRef = db
					.collection(GCP_OTA.FIRESTORE_DEVICES_COLLECTION)
					.document(deviceId)
					.collection(GCP_OTA.FIRESTORE_CONFIG_COLLECTION)
					.document(deviceId);
			ApiFuture<WriteResult> result = docRef.set(mapList, SetOptions.merge());
			System.out.println("Update time : " + result.get().getUpdateTime());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public static void addDocument(String deviceId, Map<String, Map<String, String>> map) {
		try {
			DocumentReference docRef = db
					.collection(GCP_OTA.FIRESTORE_DEVICES_COLLECTION)
					.document(deviceId)
					.collection(GCP_OTA.FIRESTORE_CONFIG_COLLECTION)
					.document(deviceId);
			ApiFuture<WriteResult> result = docRef.set(map);
			System.out.println("Update time : " + result.get().getUpdateTime());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}


}
