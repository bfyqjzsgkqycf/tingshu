package com.lsj.tingshu.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.vo.user.UserCollectVo;
import com.lsj.tingshu.vo.user.UserInfoVo;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {


    /**
     * 根据code码进行微信登录
     *
     * @param code
     * @return
     */
    Map<String, Object> wxLogin(String code);


    /**
     * 根据令牌获取用户信息
     *
     * @return
     */
    UserInfoVo getUserInfo(Long userId);


    /**
     * 更新用户信息
     *
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);


    /**
     * 用refreshToken换取新的accessToken
     *
     * @return
     */
    Map<String, Object> getNewAccessToken(String refreshToken);


    /**
     * 当前登录用户是否购买过当前专辑
     *
     * @param userId
     * @param albumId
     * @return
     */
    Boolean getIsPaidAlbum(Long userId, Long albumId);


    /**
     * 当前登录用户是否购买过当前专辑下的声音
     *
     * @param userId
     * @param albumId
     * @return
     */
    Map<Long, String> getIsPaidAlbumTrack(Long userId, Long albumId);

    /**
     * 是否收藏该声音
     *
     * @param trackId
     * @return
     */
    Boolean isCollect(Long trackId);


    /**
     * 是否订阅该声音
     *
     * @param albumId
     * @return
     */
    Boolean isSubscribe(Long albumId);


    /**
     * 分页查询用户收藏声音列表
     *
     * @param userCollectVoPage
     * @return
     */
    IPage<UserCollectVo> findUserCollectPage(IPage<UserCollectVo> userCollectVoPage);


    /**
     * 根据openId查询用户信息
     *
     * @param openId
     * @return
     */
    UserInfo getUserInfoByOpenId(String openId);


    /**
     * 更新用户身份
     */
    void updateExpireTimeVip();


}
