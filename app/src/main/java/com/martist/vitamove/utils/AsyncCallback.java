package com.martist.vitamove.utils;


public interface AsyncCallback<T> {
    
    
    void onSuccess(T result);
    
    
    void onFailure(Exception e);
} 