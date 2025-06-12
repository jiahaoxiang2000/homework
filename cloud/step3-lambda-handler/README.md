# zliang19a2app - Lambda Handler for Product Reviews

This is a Cloud9 application that serves as a Lambda function handler for processing S3 upload events and storing product review data in DynamoDB.

## Overview

The application implements the following functionality:

- **S3 Event Trigger**: Gets notified when files (JSON or TXT) are uploaded to the S3 bucket
- **Logging**: Stores the names of the bucket and uploaded file into a log file
- **Content Parsing**: Reads and parses JSON/text content to retrieve product review records
- **DynamoDB Storage**: Computes identifier field values and inserts records into the ProductReview table

## Architecture

### Components

1. **LambdaHandler**: Main entry point that handles S3 events
2. **ProductReview**: DynamoDB entity model
3. **S3Service**: Handles reading file content from S3
4. **DynamoDbService**: Manages DynamoDB operations
5. **ParsingService**: Parses JSON and text content

### Data Model

The ProductReview table contains:

- **Identifier** (String, Partition Key): Auto-generated unique identifier
- **ProductName** (String): Name of the product
- **Price** (Number): Price in AUD
- **ReviewComment** (String): Customer review text
- **Rating** (Number): Rating out of 5

## File Format Support

### JSON Format

```json
[
  {
    "ProductName": "Sony TV",
    "Price": 12000,
    "Review": "I loved this product and have been using for 5 years and no issue",
    "Rating": 4.85
  }
]
```

### Text Format

```
ProductName: Sony TV, Price: 12000, Review: I loved this product and have been using for 5 years and no issue, Rating: 4.85; ProductName: Bravia Microwave, Price: 350, Review: Good value and does the job, Rating: 4.12
```

## Prerequisites

1. **AWS CLI**: Configure with appropriate credentials
2. **Maven**: For building the Java application
3. **Java 11**: Runtime environment
4. **AWS Resources**:
   - S3 bucket named `yourusernamea2bucket`
   - DynamoDB table named `ProductReview`
   - Lambda execution role with appropriate permissions

## Building the Application

1. Navigate to the project directory:

```bash
cd /home/isomo/misc/homework/cloud/step3-lambda-handler
```

2. Build the project with Maven:

```bash
mvn clean package
```

This will create a JAR file in the `target/` directory that can be uploaded to AWS Lambda.

## Deployment Steps

### 1. Create DynamoDB Table

```bash
aws dynamodb create-table \
    --table-name ProductReview \
    --attribute-definitions \
        AttributeName=Identifier,AttributeType=S \
    --key-schema \
        AttributeName=Identifier,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --region us-east-1
```

### 2. Create S3 Bucket

```bash
aws s3 mb s3://yourusernamea2bucket --region us-east-1
```

### 3. Create Lambda Execution Role

```bash
aws iam create-role --role-name lambda-s3-dynamodb-role --assume-role-policy-document '{
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

### 4. Attach Policies to Role

```bash
# Basic Lambda execution
aws iam attach-role-policy --role-name lambda-s3-dynamodb-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# S3 read access
aws iam attach-role-policy --role-name lambda-s3-dynamodb-role --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess

# DynamoDB full access (or create a more restrictive policy)
aws iam attach-role-policy --role-name lambda-s3-dynamodb-role --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess
```

### 5. Create Lambda Function

```bash
aws lambda create-function \
    --function-name yourusernamea2app \
    --runtime java11 \
    --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-s3-dynamodb-role \
    --handler com.zliang19.LambdaHandler::handleRequest \
    --zip-file fileb://target/zliang19a2app-1.0.0.jar \
    --timeout 60 \
    --memory-size 512 \
    --region us-east-1
```

### 6. Add S3 Trigger

```bash
aws lambda add-permission \
    --function-name yourusernamea2app \
    --principal s3.amazonaws.com \
    --action lambda:InvokeFunction \
    --statement-id s3-trigger \
    --source-arn arn:aws:s3:::yourusernamea2bucket

aws s3api put-bucket-notification-configuration \
    --bucket yourusernamea2bucket \
    --notification-configuration '{
        "LambdaFunctionConfigurations": [
            {
                "Id": "ProcessUpload",
                "LambdaFunctionArn": "arn:aws:lambda:us-east-1:YOUR_ACCOUNT_ID:function:yourusernamea2app",
                "Events": ["s3:ObjectCreated:*"],
                "Filter": {
                    "Key": {
                        "FilterRules": [
                            {
                                "Name": "suffix",
                                "Value": ".json"
                            }
                        ]
                    }
                }
            },
            {
                "Id": "ProcessUploadTxt",
                "LambdaFunctionArn": "arn:aws:lambda:us-east-1:YOUR_ACCOUNT_ID:function:yourusernamea2app",
                "Events": ["s3:ObjectCreated:*"],
                "Filter": {
                    "Key": {
                        "FilterRules": [
                            {
                                "Name": "suffix",
                                "Value": ".txt"
                            }
                        ]
                    }
                }
            }
        ]
    }'
```

## Testing

### Upload Test Files

```bash
# Upload JSON test file
aws s3 cp sample-data.json s3://yourusernamea2bucket/

# Upload TXT test file
aws s3 cp sample-data.txt s3://yourusernamea2bucket/
```

### Check DynamoDB Table

```bash
aws dynamodb scan --table-name ProductReview --region us-east-1
```

### View Lambda Logs

```bash
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/yourusernamea2app
aws logs get-log-events --log-group-name /aws/lambda/yourusernamea2app --log-stream-name LATEST_LOG_STREAM
```

## Troubleshooting

1. **Permission Issues**: Ensure the Lambda execution role has proper permissions for S3 read and DynamoDB write
2. **Timeout Issues**: Increase Lambda timeout if processing large files
3. **Memory Issues**: Increase Lambda memory allocation if processing large JSON files
4. **Parsing Errors**: Check CloudWatch logs for detailed error messages

## File Structure

```
src/
├── main/
│   └── java/
│       └── com/
│           └── zliang19/
│               ├── LambdaHandler.java          # Main Lambda handler
│               ├── model/
│               │   └── ProductReview.java      # DynamoDB entity
│               └── service/
│                   ├── DynamoDbService.java    # DynamoDB operations
│                   ├── ParsingService.java     # Content parsing
│                   └── S3Service.java          # S3 operations
├── sample-data.json                            # Test JSON file
├── sample-data.txt                             # Test text file
└── pom.xml                                     # Maven configuration
```
