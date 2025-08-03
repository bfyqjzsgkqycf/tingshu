package com.cache.service;

import com.fasterxml.jackson.core.type.TypeReference;

public interface CacheOpsService {

    /**
     * 读缓存操作（指的类的类型）
     */
    <T> T getDataFromCache(String key, Class<T> returnTypeClass);


    /**
     * 读缓存操作(带泛型的返回值类型)
     */
    Object getDataFromCache(String key, TypeReference<Object> objectTypeReference);


    /**
     * 写缓存操作
     */
    void saveDataToCache(String key, Object object);
}
