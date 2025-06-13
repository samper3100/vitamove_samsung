package com.martist.vitamove.utils;


public interface SupabaseCallback<T> {
    

    void onSuccess(T result);
    

    void onFailure(Exception e);
} 