package com.zliang19.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Service class for S3 operations
 */
public class S3Service {
    
    private final S3Client s3Client;
    
    public S3Service() {
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }
    
    /**
     * Reads the content of an S3 object
     * @param bucketName The name of the S3 bucket
     * @param objectKey The key of the S3 object
     * @return The content of the object as a string
     * @throws IOException If there's an error reading the object
     */
    public String readObjectContent(String bucketName, String objectKey) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s3Object, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            String result = content.toString().trim();
            System.out.println("Successfully read object content from s3://" + bucketName + "/" + objectKey);
            System.out.println("Content preview: " + result.substring(0, Math.min(100, result.length())) + "...");
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error reading S3 object: " + e.getMessage());
            throw new IOException("Failed to read S3 object: " + e.getMessage(), e);
        }
    }
    
    /**
     * Close the S3 client
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
