package com.lsj.tingshu.account.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsj.tingshu.account.mapper.UserAccountDetailMapper;
import com.lsj.tingshu.account.mapper.UserAccountMapper;
import com.lsj.tingshu.account.service.UserAccountDetailService;
import com.lsj.tingshu.account.service.UserAccountService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.result.ResultCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.model.account.UserAccount;
import com.lsj.tingshu.model.account.UserAccountDetail;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import com.lsj.tingshu.vo.account.AccountLockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserAccountDetailService userAccountDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<AccountLockResultVo> checkAndLockUserAccount(AccountLockVo accountLockVo) {

        // 1.定义变量
        BigDecimal amount = accountLockVo.getAmount();
        Long userId = accountLockVo.getUserId();
        String content = accountLockVo.getContent();
        String orderNo = accountLockVo.getOrderNo();
        String accountLockDataKey = "user:account:data:" + orderNo;
        String accountLockKey = "user:account:lock:" + orderNo;

        // 2.查询缓存(幂等性保证)
        String accountLockDataStr = redisTemplate.opsForValue().get(accountLockDataKey);
        if (!StringUtils.isEmpty(accountLockDataStr)) {
            return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_REPEAT);
        }
        // 3.加分布式锁(幂等性保证)
        Boolean lockFlag = redisTemplate.opsForValue().setIfAbsent(accountLockKey, "1");
        if (!lockFlag) {  // 第一个人抢到了锁，在执行逻辑【检查余额和锁定余额】
            return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_REPEAT);
        }

        // CAS【比较并交换】思想：当前的值和内存的值判断是否一致（比较） 才修改内存中的值为新值（交换）
        AccountLockResultVo accountLockResultVo = null;
        // 7.锁定账户余额
        try {
            int count = userAccountMapper.checkAndLockAmount(userId, amount);   // SQLException
            if (count == 0) {
                return Result.build(null, ResultCodeEnum.ACCOUNT_LOCK_ERROR);  // 余额不够了。
            }
        } catch (Exception e) {
            redisTemplate.delete(accountLockKey);
            throw new TingShuException(400, "服务内部处理错误");
        }
        try {
            // 4. 构建返回对象
            accountLockResultVo = new AccountLockResultVo();
            accountLockResultVo.setUserId(userId);
            accountLockResultVo.setAmount(amount);
            accountLockResultVo.setContent(content);
            // 5.将构建的返回对象存储到Redis【解锁或者【消费】真正扣钱的时候方便获取到某一个用户当时下单花了多少钱】
            redisTemplate.opsForValue().set(accountLockDataKey, JSONObject.toJSONString(accountLockResultVo));
            // 6.记录用户账户流水(写入)
            log(userId, "锁定:" + content, "1202", amount, orderNo);
            redisTemplate.delete(accountLockKey);// 不删和删都可以
        } catch (Exception e) {
            throw new TingShuException(400, "服务内部处理错误");  // OpenFeign重试了
            // 1)网络抖动、故障[时间超了]。 2)业务方手动抛出异常(TODO)
            // Request request = Request.create(Request.HttpMethod.POST, "localhost://192.168.200.140:8700/xxx", new HashMap<>(), bytes, StandardCharsets.UTF_8);
            // OpenFeign重试：默认不重试，带了一个默认的重试器。
            // throw new RetryableException(400,"xxx", Request.HttpMethod.POST,e,new Date(),request);  // 重试的异常
        }

        // 9.返回构建对象
        return Result.ok(accountLockResultVo);
    }

    @Override
    public void log(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {

        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }

    @Override
    public BigDecimal getAvailableAmount(Long userId) {

        UserAccount userAccount = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId));
        return userAccount.getAvailableAmount();
    }

    @Override
    public Page<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> detailPage, Long userId) {

        LambdaQueryWrapper<UserAccountDetail> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(UserAccountDetail::getUserId, userId);
        wrapper.eq(UserAccountDetail::getTradeType, "1204");
        Page<UserAccountDetail> result = userAccountDetailService.page(detailPage, wrapper);
        return result;
    }

    @Override
    public Page<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> detailPage, Long userId) {
        LambdaQueryWrapper<UserAccountDetail> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(UserAccountDetail::getUserId, userId);
        wrapper.eq(UserAccountDetail::getTradeType, "1201");
        Page<UserAccountDetail> result = userAccountDetailService.page(detailPage, wrapper);
        return result;
    }


}
