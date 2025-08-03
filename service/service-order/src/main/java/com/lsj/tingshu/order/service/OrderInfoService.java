package com.lsj.tingshu.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.vo.order.OrderInfoVo;
import com.lsj.tingshu.vo.order.TradeVo;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算页
     *
     * @param tradeVo
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo);


    /**
     * 提交订单
     *
     * @param orderInfoVo
     * @return
     */
    Map<String, Object> submitOrder(OrderInfoVo orderInfoVo);


    /**
     * 处理支付成功之后
     */

    void processPaySuccess(String orderNo, Long userId);


    /**
     * 根据订单编号查询订单信息（基本信息 减免信息 详情信息）
     */
    OrderInfo getOrderInfoAndDetailByOrderNo(String orderNo);


    OrderInfo saveOrderInfo(String orderNo, OrderInfoVo orderInfoVo, Long userId);

    /**
     * 根据用户id 和订单编号查询订单的基本信息
     *
     * @param userId
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfoByOrderNoAndUserId(Long userId, String orderNo);

    IPage<OrderInfo> findUserPage(IPage<OrderInfo> page, Long userId);

}