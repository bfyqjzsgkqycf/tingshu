package com.lsj.tingshu.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsj.tingshu.account.client.UserAccountFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.model.payment.PaymentInfo;
import com.lsj.tingshu.order.client.OrderInfoFeignClient;
import com.lsj.tingshu.payment.mapper.PaymentInfoMapper;
import com.lsj.tingshu.payment.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;

    @Override
    public PaymentInfo saveOrderPaymentInfo(String tradeType, String orderNo, Long userId) {

        // 1.根据用户id和订单编号查询当前用户的订单支付流水表
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getUserId, userId);
        wrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        if (paymentInfo != null) {
            return paymentInfo;
        }

        // 2.保存
        paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(tradeType); // 支付类型（交易类型）
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN); // 支付方式
        paymentInfo.setOutTradeNo(orderNo);   // 订单对外交易号
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);  // 支付状态
//        paymentInfo.setCallbackTime(new Date());// 不知道
        paymentInfo.setCallbackContent("");  // 不追到


        if ("1301".equals(tradeType)) { // 正常下单
            Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfoByOrderNoAndUserId(userId, orderNo);
            OrderInfo orderInfoData = orderInfoResult.getData();
            Assert.notNull(orderInfoData, "远程调用订单微服务查询订单信息失败");
            BigDecimal orderAmount = orderInfoData.getOrderAmount();
            paymentInfo.setAmount(orderAmount);  // 订单支付多少钱
            paymentInfo.setContent(orderInfoData.getOrderDetailList().get(0).getItemName());  // 备注
        } else {
            Result<BigDecimal> rechargeInfoResult = userAccountFeignClient.getRechargeAmountByOrderNoAndUserId(userId, orderNo);
            BigDecimal rechargeAmount = rechargeInfoResult.getData();
            Assert.notNull(rechargeAmount, "远程调用用户账户微服务查询充值订单信息失败");
            paymentInfo.setAmount(rechargeAmount);  // 充值订单金额
            paymentInfo.setContent("充钱了");  // 备注
        }
        paymentInfoMapper.insert(paymentInfo);
        return paymentInfo;

    }
}
