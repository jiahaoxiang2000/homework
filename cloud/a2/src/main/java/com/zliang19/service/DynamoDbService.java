package com.zliang19.service;

import com.zliang19.model.ProductReview;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Service class for DynamoDB operations related to ProductReview
 */
public class DynamoDbService {
    
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<ProductReview> productReviewTable;
    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "ProductReview";
    
    public DynamoDbService() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();
        
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        
        this.productReviewTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(ProductReview.class));
    }
    
    /**
     * Saves a ProductReview to DynamoDB
     * @param productReview The product review to save
     */
    public void saveProductReview(ProductReview productReview) {
        try {
            productReviewTable.putItem(productReview);
            System.out.println("Successfully saved product review: " + productReview.getIdentifier());
        } catch (Exception e) {
            System.err.println("Error saving product review: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Generates the next identifier by counting existing items and adding 1
     * @return Next available identifier as a string
     */
    public String generateNextIdentifier() {
        try {
            // Scan to count existing items
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .select("COUNT")
                    .build();
            
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            long count = scanResponse.count();
            
            // Generate next identifier (count + 1)
            String nextId = String.valueOf(count + 1);
            System.out.println("Generated next identifier: " + nextId);
            return nextId;
            
        } catch (Exception e) {
            System.err.println("Error generating identifier: " + e.getMessage());
            // Fallback to timestamp-based identifier
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    /**
     * Close the DynamoDB client
     */
    public void close() {
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
        }
    }
}
