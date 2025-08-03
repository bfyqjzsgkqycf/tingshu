package com.lsj.tingshu.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.payment.PaymentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentInfoMapper extends BaseMapper<PaymentInfo> {

    @Update("update payment_info set payment_info.payment_status='1402' where payment_info.user_id=#{userId} and payment_info.order_no=#{orderNo} and payment_info.payment_status='1401'")
    void updateOrderPayStatus(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
