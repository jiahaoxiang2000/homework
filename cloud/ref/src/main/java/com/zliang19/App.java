package com.zliang19;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Main application class for counting file types in S3 bucket and generating HTML report.
 * This application connects to AWS S3, analyzes files in specified bucket,
 * counts files by type, and generates a static HTML page with the results.
 */
public class App {
    
    // Mapping of file extensions to their display categories
    private static final Map<String, String> FILE_TYPE_MAPPINGS = new HashMap<>();
    static {
        FILE_TYPE_MAPPINGS.put("html", "Web");
        FILE_TYPE_MAPPINGS.put("txt", "Text");
        FILE_TYPE_MAPPINGS.put("jpg", "Image");
        FILE_TYPE_MAPPINGS.put("xlsx", "Excel");
    }

    /**
     * Main entry point for the application
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Configuration constants
        final String targetBucket = "zliang19a1bucket";
        final Region awsRegion = Region.US_EAST_1; // Sydney region
        
        System.out.println("Starting S3 File Counter Application...");
        
        try {
            // Initialize S3 client with specified region
            S3Client s3 = S3Client.builder()
                .region(awsRegion)
                .build();
            
            // Step 1: Analyze bucket contents and count file types
            Map<String, Integer> typeCounts = analyzeBucketContents(s3, targetBucket);
            
            // Step 2: Generate HTML content from the analysis
            String htmlPage = createHtmlReport(typeCounts);
            
            // Step 3: Upload generated HTML to S3 bucket
            updateBucketWebsite(s3, targetBucket, htmlPage);
            
            System.out.println("Operation completed successfully. Website updated.");
            
        } catch (SdkException awsError) {
            System.err.println("AWS Service Error: " + awsError.getMessage());
            awsError.printStackTrace();
        } catch (Exception unexpectedError) {
            System.err.println("Unexpected Error: " + unexpectedError.getMessage());
            unexpectedError.printStackTrace();
        }
    }

    /**
     * Analyzes files in S3 bucket and counts them by type
     * @param s3Client Initialized S3 client object
     * @param bucketName Name of bucket to analyze
     * @return Map containing counts for each file category
     */
    public static Map<String, Integer> analyzeBucketContents(S3Client s3Client, String bucketName) {
        // Initialize counters for all known file types
        Map<String, Integer> counters = initializeCounters();
        
        try {
            // Create request to list all objects in bucket
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
            
            // Execute listing operation
            ListObjectsV2Response listing = s3Client.listObjectsV2(listRequest);
            
            // Process each object in the bucket
            for (S3Object object : listing.contents()) {
                String objectKey = object.key();
                String fileExtension = extractFileExtension(objectKey);
                
                // Determine file category based on extension
                String fileCategory = FILE_TYPE_MAPPINGS.getOrDefault(
                    fileExtension != null ? fileExtension.toLowerCase() : "", 
                    "Other"
                );
                
                // Increment appropriate counter
                counters.put(fileCategory, counters.get(fileCategory) + 1);
            }
            
        } catch (SdkException listError) {
            System.err.println("Failed to list bucket contents: " + listError.getMessage());
            throw listError;
        }
        
        return counters;
    }

    /**
     * Creates HTML content displaying file type counts
     * @param counts Map containing file type counts
     * @return String containing complete HTML document
     */
    public static String createHtmlReport(Map<String, Integer> counts) {
        StringBuilder html = new StringBuilder();
        
        // HTML document structure
        html.append("<!DOCTYPE html>\n")
           .append("<html lang='en'>\n")
           .append("<head>\n")
           .append("    <meta charset='UTF-8'>\n")
           .append("    <title>S3 Bucket File Analysis</title>\n")
           .append("    <style>\n")
           .append("        body { font-family: 'Segoe UI', Tahoma, sans-serif; line-height: 1.6; margin: 2rem; }\n")
           .append("        h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 0.5rem; }\n")
           .append("        ul { margin: 1rem 0; padding: 0; }\n")
           .append("        li { background: #ecf0f1; margin: 0.5rem 0; padding: 0.75rem; border-radius: 4px; }\n")
           .append("        li:nth-child(odd) { background: #d6eaf8; }\n")
           .append("    </style>\n")
           .append("</head>\n")
           .append("<body>\n")
           .append("    <h1>File Type Analysis Report</h1>\n")
           .append("    <ul>\n");
        
        // Add list items for each file type count
        counts.forEach((type, count) -> {
            html.append("        <li><strong>").append(type).append(":</strong> ").append(count).append("</li>\n");
        });
        
        // Close HTML tags
        html.append("    </ul>\n")
           .append("</body>\n")
           .append("</html>");
        
        return html.toString();
    }

    /**
     * Uploads HTML content to S3 bucket as website index
     * @param s3Client Initialized S3 client
     * @param bucketName Target bucket name
     * @param htmlContent HTML content to upload
     */
    public static void updateBucketWebsite(S3Client s3Client, String bucketName, String htmlContent) {
        try {
            // Configure upload request
            PutObjectRequest uploadRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("home.html") // Main website page
                .contentType("text/html")
                .build();
            
            // Execute upload
            s3Client.putObject(uploadRequest, RequestBody.fromString(htmlContent));
            
        } catch (SdkException uploadError) {
            System.err.println("Failed to upload HTML content: " + uploadError.getMessage());
            throw uploadError;
        }
    }

    /**
     * Initializes counter map with all possible file categories
     * @return Pre-populated counter map
     */
    private static Map<String, Integer> initializeCounters() {
        Map<String, Integer> counters = new HashMap<>();
        counters.put("Web", 0);
        counters.put("Text", 0);
        counters.put("Image", 0);
        counters.put("Excel", 0);
        counters.put("Other", 0);
        return counters;
    }

    /**
     * Extracts file extension from object key
     * @param fileName Complete file name/path
     * @return File extension without dot, or null if no extension
     */
    private static String extractFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return null;
    }
}
