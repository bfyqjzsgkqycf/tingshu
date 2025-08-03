package com.lsj.tingshu.user.mapper;

import com.lsj.tingshu.model.user.UserCollect;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.model.user.UserSubscribe;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {


    UserCollect isCollect(@Param("userId") Long userId, @Param("trackId") Long trackId);

    UserSubscribe isSubscribe(@Param("userId") Long userId, @Param("albumId") Long albumId);

}
