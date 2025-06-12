#!/bin/bash

# Cleanup script for zliang19a2app Lambda function
# This script removes all AWS resources created for the homework assignment

# Configuration variables
BUCKET_NAME="zliang19a2bucket"
LAMBDA_FUNCTION_NAME="zliang19a2app"
TABLE_NAME="ProductReview"
ROLE_NAME="lambda-s3-dynamodb-role"
REGION="us-east-1"

echo "Starting cleanup of zliang19a2app resources..."

# Step 1: Delete all objects from S3 bucket
echo "Emptying S3 bucket: $BUCKET_NAME"
aws s3 rm s3://$BUCKET_NAME --recursive

# Step 2: Delete S3 bucket
echo "Deleting S3 bucket: $BUCKET_NAME"
aws s3 rb s3://$BUCKET_NAME --force

# Step 3: Delete Lambda function
echo "Deleting Lambda function: $LAMBDA_FUNCTION_NAME"
aws lambda delete-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION

# Step 4: Delete DynamoDB table
echo "Deleting DynamoDB table: $TABLE_NAME"
aws dynamodb delete-table --table-name $TABLE_NAME --region $REGION

# Step 5: Skip IAM cleanup in AWS Academy (restricted permissions)
echo "Skipping IAM role cleanup (AWS Academy environment)"
echo "Note: In AWS Academy, IAM roles are managed by the lab environment"
echo "The role '$ROLE_NAME' will be cleaned up when the lab session ends"

echo "Cleanup completed!"
echo ""
echo "Resources cleaned up:"
echo "- DynamoDB Table: $TABLE_NAME (deleted)"
echo "- S3 Bucket: $BUCKET_NAME (deleted)"
echo "- Lambda Function: $LAMBDA_FUNCTION_NAME (deleted)"
echo "- IAM Role: $ROLE_NAME (will be cleaned up when lab session ends)"
