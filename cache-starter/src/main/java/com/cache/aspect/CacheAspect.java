package com.cache.aspect;

import com.cache.annotaion.CacheAble;
import com.cache.constant.CacheAbleConstant;
import com.cache.service.CacheOpsService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@Aspect
public class CacheAspect {


    @Autowired
    private RBloomFilter rBloomFilter;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheOpsService cacheOpsService;

    /**
     * 环绕通知
     *
     * @return
     */
    @Around(value = "@annotation(com.cache.annotaion.CacheAble)")
    public Object processCacheTask(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1.获取目标方法注解对象
        CacheAble annotation = getTargetMethodAnnotation(joinPoint, CacheAble.class);

        Type methodReturnType = getMethodReturnType(joinPoint);

        // 2.获取缓存key表达式
        String cacheKeyExpression = annotation.cacheKey();

        // 3.获取缓存key
        String cacheKey = computeSpelExpression(cacheKeyExpression, joinPoint, String.class);

        // 4.获取缓存布隆key表达式
        String bloomKeyExpression = annotation.bloomKey();

        // 5.获取缓存布隆key
        Long bloomKey = computeSpelExpression(bloomKeyExpression, joinPoint, Long.class);

        // 6.获取缓存锁key表达式
        String lockKeyExpression = annotation.lockKey();

        // 7.获取锁key
        String lockKey = computeSpelExpression(lockKeyExpression, joinPoint, String.class);


        boolean bloomFlag = annotation.enableDistroBloom();
        // 8.查询布隆过滤器
        if (bloomFlag) {
            boolean bloomFilterFlag = rBloomFilter.contains(bloomKey);
            if (!bloomFilterFlag) {
                return null;
            }
        }

        // 3.查询缓存
        Object dataFromCache = cacheOpsService.getDataFromCache(cacheKey, new TypeReference<Object>() {
            @Override
            public Type getType() {
                return methodReturnType;
            }
        });

        if (dataFromCache != null) {   // 用InstanceOf检验 
            return dataFromCache;
        }
        // 4.加分布式锁
        boolean enableDistroLock = annotation.enableDistroLock();
        if (!enableDistroLock) {
            Object proceed = joinPoint.proceed();  // 目标方法的返回值。
            cacheOpsService.saveDataToCache(cacheKey, proceed);
            return proceed;
        }

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquiredFlag = lock.tryLock();
        if (acquiredFlag) {
            try {
                Object proceed = joinPoint.proceed();  // 目标方法的返回值。
                cacheOpsService.saveDataToCache(cacheKey, proceed);
                return proceed;
            } finally {
                lock.unlock();
            }

        } else {

            Thread.sleep(CacheAbleConstant.SYNC_DATA_TTL); // 同步数据到缓存时间
            Object secondCacheResult = cacheOpsService.getDataFromCache(cacheKey, new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return methodReturnType;
                }
            });
            if (!StringUtils.isEmpty(secondCacheResult)) {
                return secondCacheResult;
            }
            return joinPoint.proceed();
        }
    }

    private Type getMethodReturnType(ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Type genericReturnType = method.getGenericReturnType();
        return genericReturnType;

    }

    private <T> T computeSpelExpression(String expression, ProceedingJoinPoint joinPoint, Class<T> returnType) {

        Object[] args = joinPoint.getArgs();

        // 1.创建spel表达式解析器对象
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

        // 2.定义计算上下文对象（存放变量）
        StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
        standardEvaluationContext.setVariable("args", args);


        // 3.定义解析上下文
        TemplateParserContext templateParserContext = new TemplateParserContext();


        Expression expression1 = spelExpressionParser.parseExpression(expression, templateParserContext);

        // 5.获取结果
        T value = expression1.getValue(standardEvaluationContext, returnType);


        return value;
    }

    private <T extends Annotation> T getTargetMethodAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationType) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Method method = signature.getMethod();

        T annotation = (T) method.getAnnotation(annotationType);

        return annotation;
    }


}
