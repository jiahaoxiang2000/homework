#!/bin/bash

# Test script for zliang19a2app Lambda function
# This script tests the deployment by uploading sample files and checking results

# Configuration variables
BUCKET_NAME="zliang19a2bucket"
LAMBDA_FUNCTION_NAME="zliang19a2app"
TABLE_NAME="ProductReview"
REGION="us-east-1"

echo "Testing zliang19a2app deployment..."

# Test 1: Upload JSON file
echo "Test 1: Uploading JSON sample data..."
aws s3 cp sample-data.json s3://$BUCKET_NAME/test-$(date +%s).json
if [ $? -eq 0 ]; then
    echo "✓ JSON file uploaded successfully"
else
    echo "✗ Failed to upload JSON file"
    exit 1
fi

# Wait for Lambda processing
echo "Waiting 10 seconds for Lambda processing..."
sleep 10

# Test 2: Upload TXT file
echo "Test 2: Uploading TXT sample data..."
aws s3 cp sample-data.txt s3://$BUCKET_NAME/test-$(date +%s).txt
if [ $? -eq 0 ]; then
    echo "✓ TXT file uploaded successfully"
else
    echo "✗ Failed to upload TXT file"
    exit 1
fi

# Wait for Lambda processing
echo "Waiting 10 seconds for Lambda processing..."
sleep 10

# Test 3: Check DynamoDB table for data
echo "Test 3: Checking DynamoDB table for inserted data..."
ITEM_COUNT=$(aws dynamodb scan --table-name $TABLE_NAME --select "COUNT" --region $REGION --query "Count" --output text)

if [ "$ITEM_COUNT" -gt 0 ]; then
    echo "✓ Found $ITEM_COUNT items in DynamoDB table"
    
    # Show sample data
    echo "Sample data from DynamoDB:"
    aws dynamodb scan --table-name $TABLE_NAME --region $REGION --max-items 3
else
    echo "✗ No items found in DynamoDB table"
fi

# Test 4: Check Lambda logs
echo "Test 4: Checking Lambda function logs..."
LOG_GROUP="/aws/lambda/$LAMBDA_FUNCTION_NAME"

# Get latest log stream
LOG_STREAM=$(aws logs describe-log-streams \
    --log-group-name $LOG_GROUP \
    --order-by LastEventTime \
    --descending \
    --limit 1 \
    --query 'logStreams[0].logStreamName' \
    --output text \
    --region $REGION)

if [ "$LOG_STREAM" != "None" ] && [ "$LOG_STREAM" != "" ]; then
    echo "✓ Lambda logs available in stream: $LOG_STREAM"
    echo "Recent log entries:"
    aws logs get-log-events \
        --log-group-name $LOG_GROUP \
        --log-stream-name $LOG_STREAM \
        --limit 20 \
        --region $REGION \
        --query 'events[*].message' \
        --output table
else
    echo "⚠ No Lambda log streams found (function may not have been invoked yet)"
fi

# Test 5: List S3 bucket contents
echo "Test 5: Listing S3 bucket contents..."
aws s3 ls s3://$BUCKET_NAME/

echo ""
echo "Testing completed!"
echo ""
echo "If you see items in DynamoDB and successful file uploads, the deployment is working correctly."
echo "You can also check the AWS Console for more detailed information:"
echo "- Lambda: https://console.aws.amazon.com/lambda/home?region=$REGION#/functions/$LAMBDA_FUNCTION_NAME"
echo "- DynamoDB: https://console.aws.amazon.com/dynamodb/home?region=$REGION#tables:selected=$TABLE_NAME"
echo "- S3: https://console.aws.amazon.com/s3/buckets/$BUCKET_NAME?region=$REGION"
