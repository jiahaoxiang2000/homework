// Lambda function for listing S3 objects and counting file types
// Filename: listS3ObjectsLambda.js

import { S3 } from '@aws-sdk/client-s3';

export const handler = async (event) => {
    // Initialize the S3 client
    const s3 = new S3();
    
    // Set your bucket name here
    const bucketName = 'YOUR-BUCKET-NAME';
    
    try {
        // Initialize counters for different file types
        let htmlCount = 0;
        let txtCount = 0;
        let jpgCount = 0;
        let xlsxCount = 0;
        let otherCount = 0;
        
        // List all objects in the bucket
        const data = await s3.listObjectsV2({ Bucket: bucketName });
        
        // Count files by extension
        data.Contents.forEach(item => {
            const key = item.Key.toLowerCase();
            
            if (key.endsWith('.html')) {
                htmlCount++;
            } else if (key.endsWith('.txt')) {
                txtCount++;
            } else if (key.endsWith('.jpg') || key.endsWith('.jpeg')) {
                jpgCount++;
            } else if (key.endsWith('.xlsx')) {
                xlsxCount++;
            } else if (key.endsWith('.pdf') || key.endsWith('.xml') || 
                      key.indexOf('.') > -1) { // Any other file with extension
                otherCount++;
            }
        });
        
        // Prepare response with CORS headers for browser access
        const response = {
            statusCode: 200,
            headers: {
                'Access-Control-Allow-Origin': '*', // Adjust this in production
                'Access-Control-Allow-Credentials': true,
            },
            body: JSON.stringify({
                htmlCount,
                txtCount,
                jpgCount,
                xlsxCount,
                otherCount,
                totalFiles: data.Contents.length
            }),
        };
        
        return response;
    } catch (error) {
        console.error('Error listing objects:', error);
        
        return {
            statusCode: 500,
            headers: {
                'Access-Control-Allow-Origin': '*', // Adjust this in production
                'Access-Control-Allow-Credentials': true,
            },
            body: JSON.stringify({ error: 'Failed to retrieve bucket statistics' }),
        };
    }
};
