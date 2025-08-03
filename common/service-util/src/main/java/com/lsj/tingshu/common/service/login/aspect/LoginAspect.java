package com.lsj.tingshu.common.service.login.aspect;

import com.alibaba.fastjson.JSON;
import com.lsj.tingshu.common.result.ResultCodeEnum;
import com.lsj.tingshu.common.service.constant.PublicConstant;
import com.lsj.tingshu.common.service.constant.RedisConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Aspect
@Component
public class LoginAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around(value = "@annotation(com.lsj.tingshu.common.login.annotation.TingShuLogin)")
    public Object checkLoginStatus(ProceedingJoinPoint pjp) throws Throwable {

        //1. 获取请求头中header中的token
        String jsonWebToken = getJsonWebToken();

        // 判断是否登录
        Boolean required = getRequired(pjp);
        if (!required) {
            if (StringUtils.isEmpty(jsonWebToken)) {
                return pjp.proceed();
            }
        }

        //2. 校验jsonWebToken
        Long userId = checkTokenAndGetUserId(jsonWebToken);

        AuthContextHolder.setUserId(userId);

        // start stopwatch
        Object retVal;
        try {
            retVal = pjp.proceed();
        } finally {
            AuthContextHolder.removeUserId();
        }
        // stop stopwatch
        return retVal;
    }

    private Boolean getRequired(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        boolean required = signature.getMethod().getAnnotation(TingShuLogin.class).required();
        return required;
    }

    private Long checkTokenAndGetUserId(String jsonWebToken) {
        //1. 校验jsonWebToken是否存在
        if (StringUtils.isEmpty(jsonWebToken)) {
            throw new TingShuException(ResultCodeEnum.LOGIN_AUTH);
        }
        //2. 校验jsonWebToken完整性
        Jwt jwt = JwtHelper.decodeAndVerify(jsonWebToken, new RsaVerifier(PublicConstant.PUBLIC_KEY));
        //3. 获取载荷中的数据
        String claims = jwt.getClaims();
        Map<String, String> map = JSON.parseObject(claims, Map.class);
        String userId = map.get("userId");
        String openId = map.get("openId");
        //4. 判断jsonWebToken是否过期
        String tokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openId;
        String jsonWebTokenFromRedis = redisTemplate.opsForValue().get(tokenKey);
        if (StringUtils.isEmpty(jsonWebTokenFromRedis) || !jsonWebTokenFromRedis.equals(jsonWebToken)) {
            //throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);单token重新登录
            throw new TingShuException(ResultCodeEnum.TOKEN_ERROR);//双token提示令牌失效
        }
        //5. 返回用户id
        return Long.parseLong(userId);
    }

    private static String getJsonWebToken() {
        //1.1 从请求上下文中获取请求属性对象
        //1.2 从请求属性对象中获取请求对象
        //1.3 从请求对象中获取请求头里的token
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("token");
        return token;
    }
}
