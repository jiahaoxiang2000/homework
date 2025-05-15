package com.example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Slf4j
public class App {
	private final S3Client s3;

	public static void main(String[] args) {
		// Create S3 client
		S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
		log.info("S3 Client created successfully!");
		// Map of file types and their corresponding extensions
		Map<String, String> typesMap = Map.of(
				"Web", "html",
				"Text", "txt",
				"Image", "jpg",
				"Excel", "xlsx"
		);
		App app = new App(s3);
		// Pull files from S3 bucket
		List<String> files = app.pullAllFiles();
		// Count the number of each type
		if (files != null) {
			Map<String, Integer> typeCount = app.typeCount(files, typesMap);
			app.updateHtml(typeCount);
		} else {
			log.error("Error occurred while pulling files from S3 bucket");
		}
	}

	public App(S3Client s3) {
		this.s3 = s3;
	}

	/**
	 * Pull files from S3 bucket
	 *
	 * @return List of file names
	 */
	private List<String> pullAllFiles() {
		try {
			// Instantiate ListObjectsRequest
			ListObjectsRequest listObjects = ListObjectsRequest.builder()
					.bucket("jyan17a1bucket")
					.build();
			// Collect s3 object keys
			return s3.listObjects(listObjects).contents().stream()
					.map(s3Object -> s3Object.key())
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error occurred while listing objects in S3 bucket", e);
			return null;
		}
	}

	/**
	 * Accumulate the count of each file type according to the extension
	 *
	 * @param files    List of files
	 * @param typesMap Map of file types and their corresponding extensions
	 * @return Map of file types and their counts
	 */
	private Map<String, Integer> typeCount(List<String> files, Map<String, String> typesMap) {
		Map<String, Integer> typeCount = typesMap.keySet().stream()
				.collect(Collectors.toMap(type -> type, type -> 0));
		// Count the number of each type
		for (String file : files) {
			// Get the extension of the file
			String type = file.substring(file.lastIndexOf(".") + 1);
			// For each file type, increment the count if the extension matches
			for (Map.Entry<String, String> entry : typesMap.entrySet()) {
				if (entry.getValue().equals(type)) {
					typeCount.put(entry.getKey(), typeCount.get(entry.getKey()) + 1);
				}
			}
		}
		// Add count for "Other" type
		typeCount.put("Other", files.size() - typeCount.values().stream().reduce(0, Integer::sum));
		return typeCount;
	}

	/**
	 * Update the HTML file with the file types and their counts
	 *
	 * @param typeCount Map of file types and their counts
	 */
	private void updateHtml(Map<String, Integer> typeCount) {
		StringBuilder html = new StringBuilder();
		// Appedn static HTML content
		html.append("<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"  <title>File Types and Counts</title>\n");
		html.append("  <style>\n" +
				"    table {\n" +
				"      border-collapse: collapse;\n" +
				"    }\n" +
				"    th, td {\n" +
				"      border: 1px solid black;\n" +
				"      padding: 8px;\n" +
				"      text-align: center;\n" +
				"    }\n" +
				"  </style>");
		html.append("</head>\n");
		html.append("<body>\n" +
				"  <h1>File Types and Counts</h1>\n" +
				"  <table>\n" +
				"    <tr>\n" +
				"      <th>Type</th>\n" +
				"      <th>Count</th>\n" +
				"    </tr>\n");
		// Append dynamic HTML content
		for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
			html.append("    <tr>\n" +
					"      <td>").append(entry.getKey()).append("</td>\n" +
					"      <td>").append(entry.getValue()).append("</td>\n" +
					"    </tr>\n");
		}
		// Append closing tags
		html.append("  </table>\n" +
				"</body>\n" +
				"</html>");
		// Create PutObjectRequest and RequestBody for uploading HTML content to S3 bucket
		PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("jyan17a1bucket").contentType("text/html").key("home.html").build();
		RequestBody requestBody = RequestBody.fromString(html.toString());
		// Upload HTML
		PutObjectResponse putObjectResponse = s3.putObject(putObjectRequest, requestBody);
		log.info("Put object response: {}", putObjectResponse);
	}

}
