package com.example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class App {
    private final S3Client s3;

    /**
     * Main entry point for the application.
     *
     * This method creates an S3 client, defines the mapping of file types to their extensions,
     * pulls all files from the S3 bucket, counts the number of each file type, and updates the
     * HTML file in the S3 bucket with the statistics. If an error occurs while pulling files,
     * it prints an error message to standard error.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        // Create S3 client
        S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
        System.out.println("S3 Client created successfully!");
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
            System.err.println("Error occurred while pulling files from S3 bucket");
        }
    }

    /**
     * Constructor for the App class.
     *
     * Initializes the App instance with the provided S3 client.
     *
     * @param s3 The S3Client instance to use for S3 operations
     */
    public App(S3Client s3) {
        this.s3 = s3;
    }

    /**
     * Pull files from the S3 bucket.
     *
     * This method lists all objects in the specified S3 bucket and returns a list of their keys (file names).
     * If an error occurs during the listing, it prints an error message and returns null.
     *
     * @return List of file names in the S3 bucket, or null if an error occurs
     */
    private List<String> pullAllFiles() {
        try {
            // Instantiate ListObjectsRequest
            ListObjectsRequest listObjects = ListObjectsRequest.builder()
                    .bucket("zliang19a1bucket")
                    .build();
            // Collect s3 object keys
            return s3.listObjects(listObjects).contents().stream()
                    .map(s3Object -> s3Object.key())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error occurred while listing objects in S3 bucket: " + e.getMessage());
            return null;
        }
    }

    /**
     * Accumulate the count of each file type according to the extension.
     *
     * This method iterates through the list of file names and counts how many files
     * belong to each type as defined in the typesMap. The typesMap maps a human-readable
     * type name (e.g., "Web", "Text") to a file extension (e.g., "html", "txt").
     *
     * For each file, the method extracts the extension and increments the count for the
     * corresponding type. If a file's extension does not match any in the typesMap, it is
     * counted as "Other". The final map includes all types from typesMap and an "Other"
     * category, with their respective counts.
     *
     * @param files    List of file names (e.g., ["index.html", "doc.txt", "img.jpg"])
     * @param typesMap Map of type names to file extensions (e.g., {"Web": "html", ...})
     * @return Map of type names to their counts, including "Other"
     */
    private Map<String, Integer> typeCount(List<String> files, Map<String, String> typesMap) {
        // Initialize the count map with all types set to 0
        Map<String, Integer> typeCount = typesMap.keySet().stream()
                .collect(Collectors.toMap(type -> type, type -> 0));
        // Count the number of each type by matching file extensions
        for (String file : files) {
            // Extract the file extension (substring after the last dot)
            String type = file.substring(file.lastIndexOf(".") + 1);
            // For each type, increment the count if the extension matches
            for (Map.Entry<String, String> entry : typesMap.entrySet()) {
                if (entry.getValue().equals(type)) {
                    typeCount.put(entry.getKey(), typeCount.get(entry.getKey()) + 1);
                }
            }
        }
        // Count files that do not match any known type as "Other"
        typeCount.put("Other", files.size() - typeCount.values().stream().reduce(0, Integer::sum));
        return typeCount;
    }

    /**
     * Update the HTML file with the file types and their counts.
     *
     * This method generates an HTML page that displays the statistics of file types in the S3 bucket.
     * The generated HTML includes a styled table with counts for each file type, a refresh button,
     * and JavaScript to dynamically update the statistics. The HTML is then uploaded to the S3 bucket
     * as "home.html".
     *
     * @param typeCount Map of file types and their counts
     */
    private void updateHtml(Map<String, Integer> typeCount) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>S3 Bucket File Statistics</title>\n");
        html.append("    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            line-height: 1.6;\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 20px;\n" +
                "            color: #333;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            color: #0066cc;\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .stats-container {\n" +
                "            background-color: #f4f4f4;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 20px;\n" +
                "            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);\n" +
                "        }\n" +
                "        .file-type {\n" +
                "            margin-bottom: 15px;\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            border-bottom: 1px solid #ddd;\n" +
                "            padding-bottom: 10px;\n" +
                "        }\n" +
                "        .file-type:last-child {\n" +
                "            border-bottom: none;\n" +
                "        }\n" +
                "        .file-count {\n" +
                "            font-weight: bold;\n" +
                "            font-size: 18px;\n" +
                "        }\n" +
                "        .refresh-button {\n" +
                "            background-color: #0066cc;\n" +
                "            color: white;\n" +
                "            border: none;\n" +
                "            padding: 10px 15px;\n" +
                "            border-radius: 4px;\n" +
                "            cursor: pointer;\n" +
                "            font-size: 16px;\n" +
                "            margin: 20px 0;\n" +
                "            display: block;\n" +
                "            margin-left: auto;\n" +
                "            margin-right: auto;\n" +
                "        }\n" +
                "        .refresh-button:hover {\n" +
                "            background-color: #0055aa;\n" +
                "        }\n" +
                "        .last-updated {\n" +
                "            text-align: center;\n" +
                "            font-size: 14px;\n" +
                "            color: #666;\n" +
                "            font-style: italic;\n" +
                "        }\n" +
                "    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>S3 Bucket File Statistics</h1>\n");
        html.append("    <div class=\"stats-container\">\n");
        html.append("        <div class=\"file-type\">\n");
        html.append("            <span>Web Files (.html)</span>\n");
        html.append("            <span class=\"file-count\" id=\"html-count\">")
            .append(typeCount.getOrDefault("Web", null) == null ? "-" : typeCount.get("Web")).append("</span>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"file-type\">\n");
        html.append("            <span>Text Files (.txt)</span>\n");
        html.append("            <span class=\"file-count\" id=\"txt-count\">")
            .append(typeCount.getOrDefault("Text", null) == null ? "-" : typeCount.get("Text")).append("</span>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"file-type\">\n");
        html.append("            <span>Image Files (.jpg)</span>\n");
        html.append("            <span class=\"file-count\" id=\"jpg-count\">")
            .append(typeCount.getOrDefault("Image", null) == null ? "-" : typeCount.get("Image")).append("</span>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"file-type\">\n");
        html.append("            <span>Excel Files (.xlsx)</span>\n");
        html.append("            <span class=\"file-count\" id=\"xlsx-count\">")
            .append(typeCount.getOrDefault("Excel", null) == null ? "-" : typeCount.get("Excel")).append("</span>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"file-type\">\n");
        html.append("            <span>Other Files (.pdf, .xml, etc.)</span>\n");
        html.append("            <span class=\"file-count\" id=\"other-count\">")
            .append(typeCount.getOrDefault("Other", null) == null ? "-" : typeCount.get("Other")).append("</span>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("    <button class=\"refresh-button\" onclick=\"fetchFileStats()\">Refresh Stats</button>\n");
        html.append("    <p class=\"last-updated\" id=\"last-updated\"></p>\n");
        html.append("    <!-- Import Axios from CDN -->\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js\"></script>\n");
        html.append("    <script>\n" +
                "        function fetchFileStats() {\n" +
                "            const apiUrl = 'https://krrjsw60q3.execute-api.us-east-1.amazonaws.com/publish';\n" +
                "            axios.get(apiUrl)\n" +
                "                .then(response => {\n" +
                "                    const responseData = response.data;\n" +
                "                    const data = JSON.parse(responseData.body);\n" +
                "                    document.getElementById('html-count').textContent = data.htmlCount || 0;\n" +
                "                    document.getElementById('txt-count').textContent = data.txtCount || 0;\n" +
                "                    document.getElementById('jpg-count').textContent = data.jpgCount || 0;\n" +
                "                    document.getElementById('xlsx-count').textContent = data.xlsxCount || 0;\n" +
                "                    document.getElementById('other-count').textContent = data.otherCount || 0;\n" +
                "                    const now = new Date();\n" +
                "                    document.getElementById('last-updated').textContent = \n" +
                "                        `Last updated: ${now.toLocaleDateString()} ${now.toLocaleTimeString()}`;\n" +
                "                })\n" +
                "                .catch(error => {\n" +
                "                    console.error('Error fetching file statistics:', error);\n" +
                "                    alert('Failed to load file statistics. Please try again later.');\n" +
                "                });\n" +
                "        }\n" +
                "        document.addEventListener('DOMContentLoaded', fetchFileStats);\n" +
                "    </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("zliang19a1bucket").contentType("text/html").key("home.html").build();
        RequestBody requestBody = RequestBody.fromString(html.toString());
        PutObjectResponse putObjectResponse = s3.putObject(putObjectRequest, requestBody);
        System.out.println("Put object response: " + putObjectResponse);
    }

}
