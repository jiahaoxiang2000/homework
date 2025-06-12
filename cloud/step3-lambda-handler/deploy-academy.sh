#!/bin/bash

# AWS Academy compatible deployment script for zliang19a2app Lambda function
# This script works with the limited permissions available in AWS Learner Lab

# Configuration variables
BUCKET_NAME="zliang19a2bucket"
LAMBDA_FUNCTION_NAME="zliang19a2app"
TABLE_NAME="ProductReview"
REGION="us-east-1"

echo "Starting AWS Academy compatible deployment of zliang19a2app..."

# Get AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "AWS Account ID: $ACCOUNT_ID"

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

# Step 3: Try to use LabRole (common in AWS Academy)
echo "Looking for existing Lambda execution role..."
LAB_ROLE_ARN="arn:aws:iam::$ACCOUNT_ID:role/LabRole"

# Check if LabRole exists
if aws iam get-role --role-name LabRole > /dev/null 2>&1; then
    ROLE_ARN=$LAB_ROLE_ARN
    echo "Using existing LabRole: $ROLE_ARN"
else
    echo "LabRole not found, trying to create custom role..."
    
    # Try to create custom role (may fail in some AWS Academy environments)
    ROLE_NAME="lambda-s3-dynamodb-role"
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
    }' 2>/dev/null
    
    if [ $? -eq 0 ]; then
        # Attach policies if role creation succeeded
        aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole 2>/dev/null
        aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess 2>/dev/null
        aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess 2>/dev/null
        
        ROLE_ARN="arn:aws:iam::$ACCOUNT_ID:role/$ROLE_NAME"
        echo "Created custom role: $ROLE_ARN"
        sleep 10
    else
        echo "ERROR: Cannot create IAM role. Please use AWS Console to:"
        echo "1. Go to IAM > Roles > Create Role"
        echo "2. Select 'Lambda' as the service"
        echo "3. Attach these policies:"
        echo "   - AWSLambdaBasicExecutionRole"
        echo "   - AmazonS3ReadOnlyAccess" 
        echo "   - AmazonDynamoDBFullAccess"
        echo "4. Name the role 'lambda-s3-dynamodb-role'"
        echo "5. Then re-run this script"
        exit 1
    fi
fi

# Step 4: Create Lambda function
echo "Creating Lambda function: $LAMBDA_FUNCTION_NAME"
echo "Using Role ARN: $ROLE_ARN"

aws lambda create-function \
    --function-name $LAMBDA_FUNCTION_NAME \
    --runtime java11 \
    --role $ROLE_ARN \
    --handler com.zliang19.LambdaHandler::handleRequest \
    --zip-file fileb://target/zliang19a2app-1.0.0.jar \
    --timeout 60 \
    --memory-size 512 \
    --region $REGION

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to create Lambda function. Check that:"
    echo "1. The JAR file exists: target/zliang19a2app-1.0.0.jar"
    echo "2. The IAM role has the correct permissions"
    echo "3. You have Lambda creation permissions in AWS Academy"
    exit 1
fi

# Step 5: Add S3 permission to Lambda
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
echo "Lambda ARN: $LAMBDA_ARN"

# Step 6: Configure S3 bucket notification
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

echo ""
echo "✅ Deployment completed successfully!"
echo ""
echo "Resources created:"
echo "- DynamoDB Table: $TABLE_NAME"
echo "- S3 Bucket: $BUCKET_NAME" 
echo "- Lambda Function: $LAMBDA_FUNCTION_NAME"
echo "- IAM Role: Using $ROLE_ARN"
echo ""
echo "🧪 Test your deployment:"
echo "aws s3 cp sample-data.json s3://$BUCKET_NAME/"
echo "aws s3 cp sample-data.txt s3://$BUCKET_NAME/"
echo ""
echo "📊 Check results:"
echo "aws dynamodb scan --table-name $TABLE_NAME --region $REGION"
