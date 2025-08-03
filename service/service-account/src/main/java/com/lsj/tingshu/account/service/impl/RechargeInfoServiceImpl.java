package com.lsj.tingshu.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsj.tingshu.account.mapper.RechargeInfoMapper;
import com.lsj.tingshu.account.mapper.UserAccountMapper;
import com.lsj.tingshu.account.service.RechargeInfoService;
import com.lsj.tingshu.account.service.UserAccountService;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.model.account.RechargeInfo;
import com.lsj.tingshu.vo.account.RechargeInfoVo;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;
    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountService userAccountService;

    @Override
    public BigDecimal getRechargeAmountByOrderNoAndUserId(Long userId, String orderNo) {


        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo).eq(RechargeInfo::getUserId, userId));

        if (rechargeInfo != null) {
            return rechargeInfo.getRechargeAmount();
        }
        throw new TingShuException(500, "该用户不存在该笔订单的充值");
    }

    @Override
    public Map<String, Object> submitRecharge(RechargeInfoVo rechargeInfoVo) {

        // 1.保存充值订单信息
        Long userId = AuthContextHolder.getUserId();
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setUserId(userId);
        String rechargeOrderNo = RandomStringUtils.random(12, true, true);
        rechargeInfo.setOrderNo(rechargeOrderNo);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());

        rechargeInfoMapper.insert(rechargeInfo);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("orderNo", rechargeOrderNo);
        return map;
    }
}
