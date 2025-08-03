package com.cache.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheAble {


    String cacheKey() default "";

    String bloomKey() default "";


    String lockKey() default "";


    boolean enableDistroLock() default false;


    boolean enableDistroBloom() default false;

}
