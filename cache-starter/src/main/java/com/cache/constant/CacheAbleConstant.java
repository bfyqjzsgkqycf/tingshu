package com.cache.constant;

public class CacheAbleConstant {

    public static String CACHE_PROTOC = "redis://";
    public static String CACHE_PROTOC_SPLIT = ":";
    public static String DISTRO_BLOOM_FILTER_NAME = "albumInfoIdsBloomFilter";
    public static String DISTRO_BLOOM_FILTER_LOCK_KEY = "distro:lock:albumInfoIdsBloomFilter";
    public static String DISTRO_BLOOM_FILTER_LOCK_VALUE = "x";
    public static Long DISTRO_BLOOM_INSERT = 1000000L;
    public static Double DISTRO_BLOOM_FPP = 0.01d;
    public static Long SYNC_DATA_TTL = 200L;
    public static Long HAS_DATA_TTL = 60 * 60 * 24 * 7L;
    public static Long NO_DATA_TTL = 60 * 60 * 2L;

}
