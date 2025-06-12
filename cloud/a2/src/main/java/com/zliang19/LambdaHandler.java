package com.zliang19;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.zliang19.model.ProductReview;
import com.zliang19.service.DynamoDbService;
import com.zliang19.service.ParsingService;
import com.zliang19.service.S3Service;

/**
 * AWS Lambda function handler for processing S3 upload events
 * This handler processes JSON and TXT files uploaded to S3 bucket,
 * parses product review data, and stores it in DynamoDB
 */
public class LambdaHandler implements RequestHandler<S3Event, String> {
    
    private final S3Service s3Service;
    private final DynamoDbService dynamoDbService;
    private final ParsingService parsingService;
    private final String logFilePath;
    
    public LambdaHandler() {
        this.s3Service = new S3Service();
        this.dynamoDbService = new DynamoDbService();
        this.parsingService = new ParsingService();
        this.logFilePath = "/tmp/s3_upload_log.txt"; // Lambda temp directory
    }
    
    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        String result = "Processing completed";
        
        try {
            // Process each S3 event record
            for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {
                
                // Extract bucket and object information
                String bucketName = record.getS3().getBucket().getName();
                String objectKey = record.getS3().getObject().getKey();
                String eventName = record.getEventName();
                
                context.getLogger().log("Processing S3 event: " + eventName + " for object: " + objectKey);
                
                // Log the upload event
                logUploadEvent(bucketName, objectKey);
                
                // Only process object creation events
                if (eventName.startsWith("ObjectCreated")) {
                    processUploadedFile(bucketName, objectKey, context);
                }
            }
            
        } catch (Exception e) {
            context.getLogger().log("Error processing S3 event: " + e.getMessage());
            e.printStackTrace();
            result = "Error: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * Processes an uploaded file from S3
     * @param bucketName The S3 bucket name
     * @param objectKey The S3 object key
     * @param context Lambda context for logging
     */
    private void processUploadedFile(String bucketName, String objectKey, Context context) {
        try {
            context.getLogger().log("Processing uploaded file: " + objectKey);
            
            // Check if file type is supported
            if (!isValidFileType(objectKey)) {
                context.getLogger().log("Skipping unsupported file type: " + objectKey);
                return;
            }
            
            // Read file content from S3
            String fileContent = s3Service.readObjectContent(bucketName, objectKey);
            
            // Parse content based on file type
            List<ProductReview> reviews = parsingService.parseContent(fileContent, objectKey);
            
            // Store each review in DynamoDB
            for (ProductReview review : reviews) {
                // Generate identifier for the review
                String identifier = dynamoDbService.generateNextIdentifier();
                review.setIdentifier(identifier);
                
                // Save to DynamoDB
                dynamoDbService.saveProductReview(review);
                
                context.getLogger().log("Saved review with ID: " + identifier + " for product: " + review.getProductName());
            }
            
            context.getLogger().log("Successfully processed " + reviews.size() + " reviews from file: " + objectKey);
            
        } catch (Exception e) {
            context.getLogger().log("Error processing file " + objectKey + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if the file type is supported (JSON or TXT)
     * @param fileName The file name
     * @return true if supported, false otherwise
     */
    private boolean isValidFileType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".json") || lowerCaseFileName.endsWith(".txt");
    }
    
    /**
     * Logs upload events to a log file
     * @param bucketName The S3 bucket name
     * @param objectKey The S3 object key
     */
    private void logUploadEvent(String bucketName, String objectKey) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logEntry = String.format("[%s] File uploaded to bucket: %s, object: %s%n", 
                    timestamp, bucketName, objectKey);
            
            // Write to log file
            File logFile = new File(logFilePath);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(logEntry);
                writer.flush();
            }
            
            System.out.println("Logged upload event: " + logEntry.trim());
            
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
    
    /**
     * Clean up resources (called when Lambda execution environment is recycled)
     */
    public void cleanup() {
        if (s3Service != null) {
            s3Service.close();
        }
        if (dynamoDbService != null) {
            dynamoDbService.close();
        }
    }
}
