package com.cache.service.impl;

import com.cache.service.CacheOpsService;
import com.cache.constant.CacheAbleConstant;
import com.cache.utils.Jsons;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CacheOpsServiceImpl implements CacheOpsService {

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Override
    public <T> T getDataFromCache(String key, Class<T> returnTypeClass) {


        String strFromCache = redisTemplate.opsForValue().get(key);

        if (StringUtils.isEmpty(strFromCache)) {
            return null;
        }

        T t = Jsons.strToObj(strFromCache, returnTypeClass);
        return t;
    }

    @Override
    public Object getDataFromCache(String key, TypeReference<Object> objectTypeReference) {


        String strFromCache = redisTemplate.opsForValue().get(key);

        if (StringUtils.isEmpty(strFromCache)) {
            return null;
        }

        Object o = Jsons.strToObj(strFromCache, objectTypeReference);
        return o;
    }

    @Override
    public void saveDataToCache(String key, Object object) {

        // 1.将对象序列化字符串
        String serializeResult = Jsons.objToStr(object);
        Long ttl = CacheAbleConstant.HAS_DATA_TTL;

        // 不正常 "{}" "[]" "null"  正常："{"name":"123","age":18}"  "["1","2"]"
        List<String> allRegex = Jsons.getAllRegex();
        for (String regex : allRegex) {
            if (Jsons.verdictRegular(regex, serializeResult)) {
                ttl = CacheAbleConstant.NO_DATA_TTL;
            }
        }

        // 2.将序列化好的字符串保存到redis中
        redisTemplate.opsForValue().set(key, serializeResult, ttl, TimeUnit.SECONDS);

    }
}
