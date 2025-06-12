# AWS Academy Deployment Guide

## ⚠️ Important: AWS Academy Limitations

AWS Learner Lab has restricted permissions. You cannot create/delete IAM roles and policies directly. Here's how to work around this:

## 🚀 Option 1: Use Automated Academy Script

```fish
cd /home/isomo/misc/homework/cloud/step3-lambda-handler
./deploy-academy.sh
```

This script will try to use the existing `LabRole` or guide you through manual IAM setup.

## 🚀 Option 2: Manual Setup with AWS Console

If the automated script fails at IAM role creation, follow these steps:

### Step 1: Create IAM Role via AWS Console

1. Go to AWS Console → IAM → Roles
2. Click "Create role"
3. Select "Lambda" as the trusted entity
4. Attach these policies:
   - `AWSLambdaBasicExecutionRole`
   - `AmazonS3ReadOnlyAccess`
   - `AmazonDynamoDBFullAccess`
5. Name the role: `lambda-s3-dynamodb-role`
6. Create the role

### Step 2: Deploy with Manual Role

```fish
cd /home/isomo/misc/homework/cloud/step3-lambda-handler

# Set your account ID (get this from AWS Console)
set ACCOUNT_ID (aws sts get-caller-identity --query Account --output text)
set ROLE_ARN "arn:aws:iam::$ACCOUNT_ID:role/lambda-s3-dynamodb-role"

# Create DynamoDB table
aws dynamodb create-table \
    --table-name ProductReview \
    --attribute-definitions AttributeName=Identifier,AttributeType=S \
    --key-schema AttributeName=Identifier,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-1

# Create S3 bucket
aws s3 mb s3://zliang19a2bucket --region us-east-1

# Create Lambda function
aws lambda create-function \
    --function-name zliang19a2app \
    --runtime java11 \
    --role $ROLE_ARN \
    --handler com.zliang19.LambdaHandler::handleRequest \
    --zip-file fileb://target/zliang19a2app-1.0.0.jar \
    --timeout 60 \
    --memory-size 512 \
    --region us-east-1

# Add S3 permission
aws lambda add-permission \
    --function-name zliang19a2app \
    --principal s3.amazonaws.com \
    --action lambda:InvokeFunction \
    --statement-id s3-trigger \
    --source-arn arn:aws:s3:::zliang19a2bucket \
    --region us-east-1

# Get Lambda ARN
set LAMBDA_ARN (aws lambda get-function --function-name zliang19a2app --query Configuration.FunctionArn --output text --region us-east-1)

# Configure S3 notification
aws s3api put-bucket-notification-configuration \
    --bucket zliang19a2bucket \
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

## 🧪 Testing

```fish
# Upload test files
aws s3 cp sample-data.json s3://zliang19a2bucket/
aws s3 cp sample-data.txt s3://zliang19a2bucket/

# Check DynamoDB
aws dynamodb scan --table-name ProductReview --region us-east-1

# Check Lambda logs
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/zliang19a2app --region us-east-1
```

## 🧹 Cleanup (AWS Academy Compatible)

```fish
./cleanup.sh
```

Note: The cleanup script now skips IAM role deletion since AWS Academy doesn't allow it.

## 🔍 Troubleshooting AWS Academy Issues

### "Access Denied" for IAM operations

- **Solution**: Use the AWS Console to create the IAM role manually
- **Alternative**: Try using the existing `LabRole` if available

### Lambda function creation fails

- **Check**: Ensure the JAR file exists: `target/zliang19a2app-1.0.0.jar`
- **Check**: IAM role has correct permissions
- **Solution**: Try using AWS Console to create the Lambda function

### S3 bucket already exists

- **Solution**: Use a unique bucket name like `yourusername-a2bucket-timestamp`

### DynamoDB table already exists

- **Solution**: Use a different table name or delete the existing one first

## 📱 AWS Console URLs (for us-east-1)

- **Lambda**: https://console.aws.amazon.com/lambda/home?region=us-east-1
- **DynamoDB**: https://console.aws.amazon.com/dynamodb/home?region=us-east-1
- **S3**: https://console.aws.amazon.com/s3/
- **IAM**: https://console.aws.amazon.com/iam/
- **CloudWatch Logs**: https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups

## 💡 Pro Tips for AWS Academy

1. **Use unique names**: Add your username/timestamp to avoid conflicts
2. **Check existing resources**: Other students might have used similar names
3. **Use AWS Console**: When CLI fails due to permissions, use the web console
4. **Monitor costs**: Even in Academy, be mindful of resource usage
5. **Clean up**: Always clean up resources when done testing
