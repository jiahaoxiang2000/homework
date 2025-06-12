package com.zliang19.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

/**
 * DynamoDB entity class for ProductReview table
 */
@DynamoDbBean
public class ProductReview {
    
    private String identifier;
    private String productName;
    private Double price;
    private String reviewComment;
    private Double rating;
    
    // Default constructor required by DynamoDB Enhanced Client
    public ProductReview() {}
    
    public ProductReview(String identifier, String productName, Double price, String reviewComment, Double rating) {
        this.identifier = identifier;
        this.productName = productName;
        this.price = price;
        this.reviewComment = reviewComment;
        this.rating = rating;
    }
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute("Identifier")
    public String getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    @DynamoDbAttribute("ProductName")
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    @DynamoDbAttribute("Price")
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    @DynamoDbAttribute("ReviewComment")
    public String getReviewComment() {
        return reviewComment;
    }
    
    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
    
    @DynamoDbAttribute("Rating")
    public Double getRating() {
        return rating;
    }
    
    public void setRating(Double rating) {
        this.rating = rating;
    }
    
    @Override
    public String toString() {
        return "ProductReview{" +
                "identifier='" + identifier + '\'' +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                ", reviewComment='" + reviewComment + '\'' +
                ", rating=" + rating +
                '}';
    }
}
