//package org.eclipse.hawkbit.google.gcp;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//
//import com.google.api.services.cloudiot.v1.model.Device;
//import com.google.api.services.cloudiot.v1.model.DeviceRegistry;
//
//public class GCP_Init {
//
//	
//	//create a registry with 2 devices
//	
//	
//	private static DeviceRegistry registry;
//
//	public static void init(String projectId, String cloudRegion, String registryName)
//	{
//		try {
//			registry = 
//					GcpRegistryHandler.createRegistry(cloudRegion, projectId, registryName, "HawkBitRegistry");
//			Device charbelk = GcpRegistryHandler.createDeviceWithRs256("charbelDevice",
//					"/Users/charbelk/dev/hawkbit-google/hawkbit-examples/hawkbit-device-simulator/src/main/resources/rsa_cert.pem",
//					projectId,
//					cloudRegion,
//					registry.getName());
//		} catch (GeneralSecurityException | IOException e) {
//			e.printStackTrace();
//		}
//	}
//}
