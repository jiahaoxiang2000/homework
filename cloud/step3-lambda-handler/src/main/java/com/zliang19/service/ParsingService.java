package com.zliang19.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zliang19.model.ProductReview;

/**
 * Service class for parsing JSON and text content into ProductReview objects
 */
public class ParsingService {
    
    private final ObjectMapper objectMapper;
    
    public ParsingService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Parses content based on file extension
     * @param content The file content as string
     * @param fileName The file name to determine parsing method
     * @return List of ProductReview objects
     * @throws IOException If parsing fails
     */
    public List<ProductReview> parseContent(String content, String fileName) throws IOException {
        if (fileName.toLowerCase().endsWith(".json")) {
            return parseJsonContent(content);
        } else if (fileName.toLowerCase().endsWith(".txt")) {
            return parseTextContent(content);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }
    
    /**
     * Parses JSON content into ProductReview objects
     * @param jsonContent The JSON content as string
     * @return List of ProductReview objects
     * @throws IOException If JSON parsing fails
     */
    private List<ProductReview> parseJsonContent(String jsonContent) throws IOException {
        List<ProductReview> reviews = new ArrayList<>();
        
        try {
            // Parse JSON array
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            if (rootNode.isArray()) {
                for (JsonNode reviewNode : rootNode) {
                    ProductReview review = parseJsonReview(reviewNode);
                    if (review != null) {
                        reviews.add(review);
                    }
                }
            } else {
                // Single object
                ProductReview review = parseJsonReview(rootNode);
                if (review != null) {
                    reviews.add(review);
                }
            }
            
            System.out.println("Successfully parsed " + reviews.size() + " reviews from JSON content");
            return reviews;
            
        } catch (Exception e) {
            System.err.println("Error parsing JSON content: " + e.getMessage());
            throw new IOException("Failed to parse JSON content: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a single JSON review node
     * @param reviewNode The JSON node representing a review
     * @return ProductReview object or null if parsing fails
     */
    private ProductReview parseJsonReview(JsonNode reviewNode) {
        try {
            String productName = getJsonString(reviewNode, "ProductName");
            Double price = getJsonDouble(reviewNode, "Price");
            String reviewComment = getJsonString(reviewNode, "Review");
            Double rating = getJsonDouble(reviewNode, "Rating");
            
            if (productName != null && price != null && reviewComment != null && rating != null) {
                return new ProductReview(null, productName, price, reviewComment, rating);
            } else {
                System.err.println("Missing required fields in JSON review: " + reviewNode.toString());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON review: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper method to safely get string value from JSON node
     */
    private String getJsonString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }
    
    /**
     * Helper method to safely get double value from JSON node
     */
    private Double getJsonDouble(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asDouble() : null;
    }
    
    /**
     * Parses text content into ProductReview objects
     * Expected format: ProductName: Sony TV, Price: 12000, Review: I loved this product..., Rating: 4.85;
     * @param textContent The text content as string
     * @return List of ProductReview objects
     * @throws IOException If text parsing fails
     */
    private List<ProductReview> parseTextContent(String textContent) throws IOException {
        List<ProductReview> reviews = new ArrayList<>();
        
        try {
            // Split by semicolon to get individual reviews
            String[] reviewStrings = textContent.split(";");
            
            for (String reviewString : reviewStrings) {
                reviewString = reviewString.trim();
                if (!reviewString.isEmpty()) {
                    ProductReview review = parseTextReview(reviewString);
                    if (review != null) {
                        reviews.add(review);
                    }
                }
            }
            
            System.out.println("Successfully parsed " + reviews.size() + " reviews from text content");
            return reviews;
            
        } catch (Exception e) {
            System.err.println("Error parsing text content: " + e.getMessage());
            throw new IOException("Failed to parse text content: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a single text review string
     * @param reviewString The text string representing a review
     * @return ProductReview object or null if parsing fails
     */
    private ProductReview parseTextReview(String reviewString) {
        try {
            // Regular expressions to extract field values
            String productName = extractField(reviewString, "ProductName");
            String priceStr = extractField(reviewString, "Price");
            String reviewComment = extractField(reviewString, "Review");
            String ratingStr = extractField(reviewString, "Rating");
            
            if (productName != null && priceStr != null && reviewComment != null && ratingStr != null) {
                Double price = Double.parseDouble(priceStr);
                Double rating = Double.parseDouble(ratingStr);
                
                return new ProductReview(null, productName, price, reviewComment, rating);
            } else {
                System.err.println("Missing required fields in text review: " + reviewString);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error parsing text review: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts field value from text using regex
     * @param text The text to search in
     * @param fieldName The field name to extract
     * @return The field value or null if not found
     */
    private String extractField(String text, String fieldName) {
        // Pattern to match "FieldName: value" where value can contain commas but stops at the next field or end
        String pattern = fieldName + ":\\s*([^,]+?)(?=\\s*,\\s*\\w+:|$)";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
