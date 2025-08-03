package com.lsj.tingshu.account.receiver.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.account.mapper.RechargeInfoMapper;
import com.lsj.tingshu.account.mapper.UserAccountMapper;
import com.lsj.tingshu.account.receiver.service.MqOpsService;
import com.lsj.tingshu.account.service.UserAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.model.account.RechargeInfo;
import com.lsj.tingshu.model.account.UserAccount;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;


    @Override
    public void registerUserAccount(String message) {

        // 1.处理消息
        Long userId = Long.parseLong(message);

        // 2.注册用户账户
        try {
            UserAccount userAccount = new UserAccount();
            userAccount.setUserId(userId);
            int insert = userAccountMapper.insert(userAccount);
            log.info("注册用户账户{}", insert > 0 ? "success" : "fail");
        } catch (Exception e) {
            throw new TingShuException(500, "数据库操作出现了异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userAccountMinus(String orderNo) {

        // 1.从Redis中获取用户锁定记录
        String accountLockDataKey = "user:account:data:" + orderNo;
        String minusKey = "user:minus:lock:" + orderNo;
        String accountLockStr = redisTemplate.opsForValue().get(accountLockDataKey);

        // 2.如果Redis中没有用户账户的锁定记录，直接返回
        if (StringUtils.isEmpty(accountLockStr)) {
            return;
        }

        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(minusKey, "1");
        if (aBoolean) {
            try {
                // 3.反序列化
                AccountLockResultVo accountLockResultVo = JSONObject.parseObject(accountLockStr, AccountLockResultVo.class);
                Long userId = accountLockResultVo.getUserId();
                BigDecimal amount = accountLockResultVo.getAmount();
                String content = accountLockResultVo.getContent();
                // 4.消费
                userAccountMapper.minus(userId, amount);
                // 5.记录消费的流水
                userAccountService.log(userId, "消费:" + content, "1204", amount, orderNo);
                // 6.删除用户锁定的流水
                redisTemplate.delete(accountLockDataKey);
            } catch (Exception e) {
                redisTemplate.delete(minusKey);
                throw new TingShuException(500, "数据库操作失败");
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userAccountUnlock(String orderNo) {
        // 1.从Redis中获取用户锁定记录
        String accountLockDataKey = "user:account:data:" + orderNo;
        String unLockKey = "user:unlock:lock:" + orderNo;

        String accountLockStr = redisTemplate.opsForValue().get(accountLockDataKey);
        // 2.如果Redis中没有用户账户的锁定记录，直接返回
        if (StringUtils.isEmpty(accountLockStr)) {
            return;
        }
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(unLockKey, "1");
        if (aBoolean) {
            try {
                // 3.反序列化
                AccountLockResultVo accountLockResultVo = JSONObject.parseObject(accountLockStr, AccountLockResultVo.class);
                Long userId = accountLockResultVo.getUserId();
                BigDecimal amount = accountLockResultVo.getAmount();
                String content = accountLockResultVo.getContent();

                // 4.解锁
                userAccountMapper.unLock(userId, amount);

                // 5.记录消费的流水
                userAccountService.log(userId, "解锁:" + content, "1203", amount, orderNo);

                // 6.删除用户锁定的流水
                redisTemplate.delete(accountLockDataKey);
            } catch (Exception e) {
                redisTemplate.delete(unLockKey);
                throw new TingShuException(500, "数据库操作失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargePaidSuccess(String content) {

        // 1. 反序列化
        JSONObject jsonObject = JSONObject.parseObject(content, JSONObject.class);
        String userId = (String) jsonObject.get("userId");
        String orderNo = (String) jsonObject.get("orderNo");

        LambdaQueryWrapper<RechargeInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeInfo::getUserId, userId);
        wrapper.eq(RechargeInfo::getOrderNo, orderNo);

        // 2. 查询充值订单
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(wrapper);
        if (rechargeInfo == null) {
            return;
        }
        // 3. 修改"充钱"订单状态为已支付
        rechargeInfoMapper.updateRechargeStatus(userId, orderNo);

        // 4. 修改用户的账户余额（total_amount=total_amount+充值 available_amount=available_amount+充值 total_income_amount=total_income_amount+充值）
        userAccountMapper.updateUserAmount(userId, rechargeInfo.getRechargeAmount());

        // 5. 记录用户的账户的流水信息
        userAccountService.log(Long.parseLong(userId),
                "充值:" + rechargeInfo.getRechargeAmount(),
                "1201",
                rechargeInfo.getRechargeAmount(), orderNo);


    }
}
