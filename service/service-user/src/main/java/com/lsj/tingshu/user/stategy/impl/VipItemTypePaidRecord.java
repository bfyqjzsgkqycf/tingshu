package com.lsj.tingshu.user.stategy.impl;

import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.model.user.UserVipService;
import com.lsj.tingshu.model.user.VipServiceConfig;
import com.lsj.tingshu.user.mapper.UserInfoMapper;
import com.lsj.tingshu.user.mapper.UserVipServiceMapper;
import com.lsj.tingshu.user.mapper.VipServiceConfigMapper;
import com.lsj.tingshu.user.stategy.DiffItemTypePaidRecordProcess;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Slf4j
@Component(value = "1003")
public class VipItemTypePaidRecord implements DiffItemTypePaidRecordProcess {


    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void processPaidRecord(UserPaidRecordVo userPaidRecordVo) {
        // 1.根据订单编号查询付款项是vip支付流水
        LambdaQueryWrapper<UserVipService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo());
        wrapper.eq(UserVipService::getUserId, userPaidRecordVo.getUserId());
        UserVipService userVipService = userVipServiceMapper.selectOne(wrapper);
        if (userVipService != null) {
            log.error("付款项类型是vip的支付流水已经存在。");
            return;
        }

        // 2.构建对象
        userVipService = new UserVipService();
        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipService.setUserId(userPaidRecordVo.getUserId());
        userVipService.setStartTime(new Date()); // vip套餐的开始时间


        // 2.1 根据用户id 查询用户对象
        UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        if (userInfo == null) {
            log.error("用户不存在");
            throw new TingShuException(500, "用户信息不存在");
        }
        Integer isVip = userInfo.getIsVip();
        Date vipExpireTime = userInfo.getVipExpireTime();

        // 2.2 根据套餐id 获取套餐信息
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
        if (vipServiceConfig == null) {
            log.error("套餐不存在");
            throw new TingShuException(500, "套餐不存在");
        }
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        Calendar instance = Calendar.getInstance();  // 得到日历对象

        // 2.3 判断用户vip身份和
        if ("1".equals(isVip + "") && vipExpireTime.after(new Date())) {
            instance.setTime(vipExpireTime);
        } else {
            instance.setTime(new Date());
        }

        instance.add(Calendar.MONTH, serviceMonth);
        userVipService.setExpireTime(instance.getTime());  // vip套餐到期时间

        // 2.4 记录流水
        userVipServiceMapper.insert(userVipService);

        userInfo.setIsVip(1); // 用户的身份
        userInfo.setVipExpireTime(userVipService.getExpireTime());
        // 2.5 修改用户信息（vip身份 vip过期时间）
        userInfoMapper.updateById(userInfo);
    }
}
