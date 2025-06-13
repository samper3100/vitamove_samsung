package com.martist.vitamove.models.openfoodfacts;

import com.google.gson.annotations.SerializedName;

public class OpenFoodFactsResponse {
    @SerializedName("code")
    private String code;
    
    @SerializedName("status")
    private int status;
    
    @SerializedName("product")
    private Product product;
    
    public String getCode() {
        return code;
    }
    
    public int getStatus() {
        return status;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public boolean isSuccess() {
        return status == 1 && product != null;
    }
} 