package com.martist.vitamove.utils;


@FunctionalInterface
public interface Callback<T> {
    
    
    void call(T param);
} 