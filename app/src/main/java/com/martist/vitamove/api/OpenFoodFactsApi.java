package com.martist.vitamove.api;

import com.martist.vitamove.models.openfoodfacts.OpenFoodFactsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    Call<OpenFoodFactsResponse> getProductByBarcode(@Path("barcode") String barcode);
} 