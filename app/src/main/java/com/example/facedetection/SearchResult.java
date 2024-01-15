package com.example.facedetection;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;

public class SearchResult {
    private String label;
    private float confidence;
    private String productDetails; // You might want to add more details as needed

    public SearchResult(String label, float confidence, String productDetails) {
        this.label = label;
        this.confidence = confidence;
        this.productDetails = productDetails;
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getProductDetails() {
        return productDetails;
    }
}


