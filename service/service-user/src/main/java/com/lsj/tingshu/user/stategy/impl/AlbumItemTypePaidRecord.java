package com.lsj.tingshu.user.stategy.impl;

import com.lsj.tingshu.model.user.UserPaidAlbum;
import com.lsj.tingshu.user.mapper.UserPaidAlbumMapper;
import com.lsj.tingshu.user.stategy.DiffItemTypePaidRecordProcess;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component(value = "1001")
public class AlbumItemTypePaidRecord implements DiffItemTypePaidRecordProcess {


    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public void processPaidRecord(UserPaidRecordVo userPaidRecordVo) {
        // 1.根据订单编号查询付款项是专辑支付流水
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo());
        wrapper.eq(UserPaidAlbum::getUserId, userPaidRecordVo.getUserId());
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
        if (userPaidAlbum != null) {
            log.error("付款项类型是专辑的支付流水已经存在。");
            return;
        }
        // 2.插入
        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbumMapper.insert(userPaidAlbum);

    }
}
