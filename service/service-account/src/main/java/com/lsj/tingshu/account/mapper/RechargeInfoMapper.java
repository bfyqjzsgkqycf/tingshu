package com.lsj.tingshu.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.account.RechargeInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RechargeInfoMapper extends BaseMapper<RechargeInfo> {

    @Update("update  recharge_info  set  recharge_status='0902' where user_id=#{userId} and order_no=#{orderNo} and recharge_status='0901'")
    void updateRechargeStatus(@Param("userId") String userId, @Param("orderNo") String orderNo);

}
