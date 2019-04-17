package org.eclipse.hawkbit.google.gcp;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class GcpProperties {
	
	public static void parseCLI(String[] args) {
		// create Options object
		Options options = new Options();
		Option keys = Option.builder().argName("KEYS").hasArg().required()
				.longOpt("KEYS").desc("Keys file path from a GCP project Service Account").build();
		Option gcpProjectID = Option.builder().argName("PROJECT_ID").hasArg().required()
				.longOpt("PROJECT_ID").desc("GCP PROJECT_ID").build();
		Option gcpCloudRegion = Option.builder().argName("PROJECT_ID").hasArg().required()
				.longOpt("CLOUD_REGION").desc("GCP CLOUD_REGION").build();
		Option gcpRegistryName = Option.builder().argName("REGISTRY_NAME").hasArg().required()
				.longOpt("REGISTRY_NAME").desc("GCP REGISTRY_NAME").build();
		Option gcpBucketName = Option.builder().argName("BUCKET_NAME").hasArg().required()
				.longOpt("BUCKET_NAME").desc("GCP BUCKET_NAME").build();

		options.addOption(keys);
		options.addOption(gcpProjectID);
		options.addOption(gcpCloudRegion);
		options.addOption(gcpRegistryName);
		options.addOption(gcpBucketName);

		DefaultParser defaultParser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();

		CommandLine line;
		try {
			line = defaultParser.parse(options, args);

			if (line.hasOption("PROJECT_ID")) {
				GcpOTA.PROJECT_ID = line.getOptionValue("PROJECT_ID");
			}
			if (line.hasOption("CLOUD_REGION")) {
				GcpOTA.CLOUD_REGION = line.getOptionValue("CLOUD_REGION");
			}
			if (line.hasOption("REGISTRY_NAME")) {
				GcpOTA.REGISTRY_NAME = line.getOptionValue("REGISTRY_NAME");
			}
			if (line.hasOption("BUCKET_NAME")) {
				GcpOTA.BUCKET_NAME = line.getOptionValue("BUCKET_NAME");
			}
			if (line.hasOption("KEYS")) {
				GcpCredentials.setKeysFilePath(line.getOptionValue("KEYS"));
			}

		} catch (IllegalArgumentException | ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("help", options);
			System.exit(0);
		}
	}

}
