package com.cache.utils;

import com.cache.exception.TingShuException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Jsons {


    static Logger logger = LoggerFactory.getLogger(Jsons.class);

    static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 序列化操作: 将对象转换为字符串
     */
    public static String objToStr(Object obj) {

        try {

            // 1.（任意：User AlbumInfo Map List  Set）对象有值
            // 2. （任意：User AlbumInfo Map List  Set）对象没有值
            // 2.1 obj=null----> "null"
            // 2.2 Map List Set []没有数据---->双列的对象没有数据"{}" 单列的对象没有数据"[]"
            String serializeResult = objectMapper.writeValueAsString(obj);
            return serializeResult;
        } catch (JsonProcessingException e) {
            logger.error("对象{}序列化失败,原因:{}", obj, e.getMessage());
            throw new TingShuException(500, "对象序列化失败");
        }

    }

    /**
     * 反序列化操作
     */


    public static <T> T strToObj(String content, Class<T> returnType) {

        try {
            // User  Map/ "{}" "null" "[]"
            T t = objectMapper.readValue(content, returnType);
            return t;
        } catch (JsonProcessingException e) {
            logger.error("字符串{}反序列化失败,原因:{}", content, e.getMessage());
            throw new TingShuException(500, "字符串反序列化失败");
        }
    }


    public static Object strToObj(String content, TypeReference<Object> returnType) {

        try {
            // User  Map/ "{}" "null" "[]"
            Object o = objectMapper.readValue(content, returnType);
            return o;
        } catch (JsonProcessingException e) {
            logger.error("字符串{}反序列化失败,原因:{}", content, e.getMessage());
            throw new TingShuException(500, "字符串反序列化失败");
        }
    }

    /**
     * 要判断的特殊标记存入到集合中
     */

    public static List<String> getAllRegex() {
        List<String> objects = new ArrayList<>();
        objects.add("^\\{\\}$");
        objects.add("^null$");
        objects.add("^\\[\\]$");
        return objects;
    }

    /**
     * 正则的判断
     */
    public static boolean verdictRegular(String ruleRegex, String compareContent) {


        return Pattern.matches(ruleRegex, compareContent);
    }
}
