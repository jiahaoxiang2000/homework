# Fish Shell Deployment Guide for zliang19a2app

This guide provides fish shell specific commands for deploying the Lambda handler.

## Quick Deployment

### Option 1: Use the Automated Script

```fish
cd /home/isomo/misc/homework/cloud/step3-lambda-handler
./deploy.sh
```

### Option 2: Manual Step-by-Step Deployment

#### Prerequisites

```fish
# Check if AWS CLI is configured
aws sts get-caller-identity
```

#### 1. Build the Application

```fish
cd /home/isomo/misc/homework/cloud/step3-lambda-handler
mvn clean package
```

#### 2. Set Variables (Fish Shell Syntax)

```fish
set BUCKET_NAME "zliang19a2bucket"
set LAMBDA_FUNCTION_NAME "zliang19a2app"
set TABLE_NAME "ProductReview"
set ROLE_NAME "lambda-s3-dynamodb-role"
set REGION "us-east-1"
```

#### 3. Create DynamoDB Table

```fish
aws dynamodb create-table \
    --table-name $TABLE_NAME \
    --attribute-definitions AttributeName=Identifier,AttributeType=S \
    --key-schema AttributeName=Identifier,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region $REGION
```

#### 4. Create S3 Bucket

```fish
aws s3 mb s3://$BUCKET_NAME --region $REGION
```

#### 5. Create IAM Role

```fish
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
```

#### 6. Attach Policies

```fish
aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
aws iam attach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess
```

#### 7. Get Account ID and Create Lambda Function

```fish
set ACCOUNT_ID (aws sts get-caller-identity --query Account --output text)
set ROLE_ARN "arn:aws:iam::$ACCOUNT_ID:role/$ROLE_NAME"

# Wait for role to be available
sleep 10

aws lambda create-function \
    --function-name $LAMBDA_FUNCTION_NAME \
    --runtime java11 \
    --role $ROLE_ARN \
    --handler com.zliang19.LambdaHandler::handleRequest \
    --zip-file fileb://target/zliang19a2app-1.0.0.jar \
    --timeout 60 \
    --memory-size 512 \
    --region $REGION
```

#### 8. Configure S3 Trigger

```fish
# Add permission for S3 to invoke Lambda
aws lambda add-permission \
    --function-name $LAMBDA_FUNCTION_NAME \
    --principal s3.amazonaws.com \
    --action lambda:InvokeFunction \
    --statement-id s3-trigger \
    --source-arn arn:aws:s3:::$BUCKET_NAME \
    --region $REGION

# Get Lambda ARN
set LAMBDA_ARN (aws lambda get-function --function-name $LAMBDA_FUNCTION_NAME --query Configuration.FunctionArn --output text --region $REGION)

# Configure bucket notification
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
```

## Testing Commands (Fish Shell)

### Upload Test Files

```fish
cd /home/isomo/misc/homework/cloud/step3-lambda-handler

# Upload JSON file
aws s3 cp sample-data.json s3://$BUCKET_NAME/test-(date +%s).json

# Upload TXT file
aws s3 cp sample-data.txt s3://$BUCKET_NAME/test-(date +%s).txt

# Upload extended sample files
aws s3 cp extended-sample-data.json s3://$BUCKET_NAME/
aws s3 cp extended-sample-data.txt s3://$BUCKET_NAME/
```

### Check Results

```fish
# Check DynamoDB table
aws dynamodb scan --table-name $TABLE_NAME --region $REGION

# Check S3 bucket contents
aws s3 ls s3://$BUCKET_NAME/

# Check Lambda logs
set LOG_GROUP "/aws/lambda/$LAMBDA_FUNCTION_NAME"
aws logs describe-log-streams --log-group-name $LOG_GROUP --order-by LastEventTime --descending --limit 1 --region $REGION
```

### Automated Testing

```fish
./test.sh
```

## Cleanup Commands (Fish Shell)

### Quick Cleanup

```fish
./cleanup.sh
```

### Manual Cleanup

```fish
# Empty and delete S3 bucket
aws s3 rm s3://$BUCKET_NAME --recursive
aws s3 rb s3://$BUCKET_NAME --force

# Delete Lambda function
aws lambda delete-function --function-name $LAMBDA_FUNCTION_NAME --region $REGION

# Delete DynamoDB table
aws dynamodb delete-table --table-name $TABLE_NAME --region $REGION

# Delete IAM role
aws iam detach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam detach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
aws iam detach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess
aws iam delete-role --role-name $ROLE_NAME
```

## Useful Fish Shell Functions

Add these to your fish config for easier management:

```fish
# Add to ~/.config/fish/config.fish

function aws-lambda-logs
    set LOG_GROUP "/aws/lambda/$argv[1]"
    set LOG_STREAM (aws logs describe-log-streams --log-group-name $LOG_GROUP --order-by LastEventTime --descending --limit 1 --query 'logStreams[0].logStreamName' --output text --region us-east-1)
    aws logs get-log-events --log-group-name $LOG_GROUP --log-stream-name $LOG_STREAM --limit 50 --region us-east-1 --query 'events[*].message' --output table
end

function aws-dynamodb-count
    aws dynamodb scan --table-name $argv[1] --select "COUNT" --region us-east-1 --query "Count" --output text
end

function aws-s3-upload-test
    set timestamp (date +%s)
    aws s3 cp $argv[1] s3://$argv[2]/test-$timestamp-(basename $argv[1])
end
```

Then use them like:

```fish
aws-lambda-logs zliang19a2app
aws-dynamodb-count ProductReview
aws-s3-upload-test sample-data.json zliang19a2bucket
```
