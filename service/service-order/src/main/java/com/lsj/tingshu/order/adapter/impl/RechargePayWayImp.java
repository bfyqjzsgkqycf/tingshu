package com.lsj.tingshu.order.adapter.impl;

import com.lsj.tingshu.account.client.UserAccountFeignClient;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.result.ResultCodeEnum;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.model.order.LocalMsg;
import com.lsj.tingshu.order.adapter.PayWay;
import com.lsj.tingshu.order.mapper.LocalMsgMapper;
import com.lsj.tingshu.order.mapper.OrderDerateMapper;
import com.lsj.tingshu.order.mapper.OrderDetailMapper;
import com.lsj.tingshu.order.mapper.OrderInfoMapper;
import com.lsj.tingshu.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import com.lsj.tingshu.vo.account.AccountLockVo;
import com.lsj.tingshu.vo.order.OrderInfoVo;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RechargePayWayImp implements PayWay {

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private LocalMsgMapper localMsgMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;
//    @Autowired
//    private RechargePayWayImp rechargePayWayImp;


    @Autowired
    private OrderInfoService orderInfoService;

    @Override
    public boolean supportPayWay(String payWay) {
        return "1103".equals(payWay);
    }


    /**
     * 定时任务
     *
     * @param
     * @param
     * @param
     */

    @Scheduled(fixedRate = 1000 * 60 * 60) // 每一个小时进行一次定时任务重发【时间可以调整】
    public void sendRetrySendMsg() {

        LambdaQueryWrapper<LocalMsg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocalMsg::getStatus, 0);
        List<LocalMsg> localMsgs = localMsgMapper.selectList(wrapper);
        for (LocalMsg localMsg : localMsgs) {

            // 1.发送消息给解锁队列
            String unLockRetryMsgKey = "msg:retry:unlock:" + localMsg.getMsgContent();
            String unLockRetryMsgContent = redisTemplate.opsForValue().get(unLockRetryMsgKey);
            if (!StringUtils.isEmpty(unLockRetryMsgContent)) {
                rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_UNLOCK, localMsg.getMsgContent());
            }
            String minusRetryMsgKey = "msg:retry:minus:" + localMsg.getMsgContent();
            String minusRetryMsgContent = redisTemplate.opsForValue().get(minusRetryMsgKey);
            if (!StringUtils.isEmpty(minusRetryMsgContent)) {
                // 2.发送消息给消费队列
                rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_MINUS, localMsg.getMsgContent());
            }

        }
    }


    @Override
    public void payWay(String orderNo, OrderInfoVo orderInfoVo, Long userId) {


        RechargePayWayImp proxy = (RechargePayWayImp) AopContext.currentProxy();
        // 零钱支付逻辑
        // 1.检查账户余额是否充足(1.1 检查账户余额 1.2 锁账户余额)
        // 1.1 构建检查和锁定余额的对象
        AccountLockVo accountLockVo = prepareUserAccount(orderNo, userId, orderInfoVo);
        Result<AccountLockResultVo> accountLockResult = userAccountFeignClient.checkAndLockUserAccount(accountLockVo);

        Integer code = accountLockResult.getCode();
        if (code != 200) {
            throw new TingShuException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
        }

        try {
            // 2.初始化本地消息表：目标对象--->代理对象
            initLocalMsg(orderNo);
            // 3.保存订单信息
            orderInfoService.saveOrderInfo(orderNo, orderInfoVo, userId); // this 但不是代理对象
            // 4.真正扣减账户余额 // 发消息[发消息失败【重试】？消费消息失败怎么办【重试】] or  rpc
            // 消费：真正扣钱(-total_amount -lock_amount +total_pay_amount)
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_MINUS, orderNo);

            // 5.支付成功之后的事情处理

            orderInfoService.processPaySuccess(orderNo, userId);

        } catch (TingShuException e) {
            // 解锁：(-lock_amount +available_amount)
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_UNLOCK, orderNo);
            String unLockRetryMsgKey = "msg:retry:unlock:" + orderNo;
            redisTemplate.opsForValue().set(unLockRetryMsgKey, orderNo);

        } catch (Exception e1) {
            rabbitService.sendMessage(MqConst.EXCHANGE_ACCOUNT, MqConst.ROUTING_ACCOUNT_MINUS, orderNo);
            String minusRetryMsgKey = "msg:retry:minus:" + orderNo;
            redisTemplate.opsForValue().set(minusRetryMsgKey, orderNo);
        }

    }

    private void initLocalMsg(String orderNo) {
        LocalMsg localMsg = new LocalMsg();
        localMsg.setMsgContent(orderNo);
        localMsg.setStatus(0);  // 每一条消息（订单编号）状态
        localMsgMapper.insert(localMsg);

    }


    private AccountLockVo prepareUserAccount(String orderNo, Long userId, OrderInfoVo orderInfoVo) {

        AccountLockVo accountLockVo = new AccountLockVo();
        accountLockVo.setOrderNo(orderNo);
        accountLockVo.setUserId(userId);
        accountLockVo.setAmount(orderInfoVo.getOrderAmount());
        accountLockVo.setContent(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        return accountLockVo;
    }
}
