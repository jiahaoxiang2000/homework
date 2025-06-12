#!/bin/bash

# Deployment script for zliang19a2app Lambda function
# This script sets up all AWS resources needed for the homework assignment

# Configuration variables
BUCKET_NAME="zliang19a2bucket"
LAMBDA_FUNCTION_NAME="zliang19a2app"
TABLE_NAME="ProductReview"
ROLE_NAME="lambda-s3-dynamodb-role"
REGION="us-east-1"

echo "Starting deployment of zliang19a2app..."

# Step 1: Create DynamoDB table
echo "Creating DynamoDB table: $TABLE_NAME"
aws dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions \
        AttributeName=Identifier,AttributeType=S \
    --key-schema \
        AttributeName=Identifier,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION

echo "Waiting for DynamoDB table to be active..."
aws dynamodb wait table-exists --table-name $TABLE_NAME --region $REGION

# Step 2: Create S3 bucket
echo "Creating S3 bucket: $BUCKET_NAME"
aws s3 mb s3://$BUCKET_NAME --region $REGION

# Step 3: Create IAM role for Lambda
echo "Creating IAM role: $ROLE_NAME"
aws iam create-role --role-name $ROLE_NAME --assume-role-policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}'

# Step 4: Attach policies to the role
echo "Attaching policies to IAM role..."
aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess

# Wait for role to be available
echo "Waiting for IAM role to be available..."
sleep 10

# Get AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ROLE_ARN="arn:aws:iam::$ACCOUNT_ID:role/$ROLE_NAME"

echo "Using Role ARN: $ROLE_ARN"

# Step 5: Create Lambda function
echo "Creating Lambda function: $LAMBDA_FUNCTION_NAME"
aws lambda create-function \
    --function-name $LAMBDA_FUNCTION_NAME \
    --runtime java11 \
    --role $ROLE_ARN \
    --handler com.zliang19.LambdaHandler::handleRequest \
    --zip-file fileb://target/zliang19a2app-1.0.0.jar \
    --timeout 60 \
    --memory-size 512 \
    --region $REGION

# Step 6: Add S3 permission to Lambda
echo "Adding S3 permission to Lambda function..."
aws lambda add-permission \
    --function-name $LAMBDA_FUNCTION_NAME \
    --principal s3.amazonaws.com \
    --action lambda:InvokeFunction \
    --statement-id s3-trigger \
    --source-arn arn:aws:s3:::$BUCKET_NAME \
    --region $REGION

# Get Lambda function ARN
LAMBDA_ARN=$(aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --query Configuration.FunctionArn --output text --region $REGION)

echo "Using Lambda ARN: $LAMBDA_ARN"

# Step 7: Configure S3 bucket notification
echo "Configuring S3 bucket notification..."
aws s3api put-bucket-notification-configuration \
    --bucket $BUCKET_NAME \
    --notification-configuration "{
        \"LambdaConfigurations\": [
            {
                \"Id\": \"ProcessJSONUpload\",
                \"LambdaFunctionArn\": \"$LAMBDA_ARN\",
                \"Events\": [\"s3:ObjectCreated:*\"],
                \"Filter\": {
                    \"Key\": {
                        \"FilterRules\": [
                            {
                                \"Name\": \"suffix\",
                                \"Value\": \".json\"
                            }
                        ]
                    }
                }
            },
            {
                \"Id\": \"ProcessTXTUpload\",
                \"LambdaFunctionArn\": \"$LAMBDA_ARN\",
                \"Events\": [\"s3:ObjectCreated:*\"],
                \"Filter\": {
                    \"Key\": {
                        \"FilterRules\": [
                            {
                                \"Name\": \"suffix\",
                                \"Value\": \".txt\"
                            }
                        ]
                    }
                }
            }
        ]
    }"

echo "Deployment completed successfully!"
echo ""
echo "Resources created:"
echo "- DynamoDB Table: $TABLE_NAME"
echo "- S3 Bucket: $BUCKET_NAME"
echo "- IAM Role: $ROLE_NAME"
echo "- Lambda Function: $LAMBDA_FUNCTION_NAME"
echo ""
echo "You can now test by uploading JSON or TXT files to the S3 bucket:"
echo "aws s3 cp sample-data.json s3://$BUCKET_NAME/"
echo "aws s3 cp sample-data.txt s3://$BUCKET_NAME/"
