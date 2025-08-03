package com.lsj.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.result.ResultCodeEnum;
import com.lsj.tingshu.common.service.constant.PublicConstant;
import com.lsj.tingshu.common.service.constant.RedisConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.common.util.MongoUtil;
import com.lsj.tingshu.model.user.*;
import com.lsj.tingshu.user.mapper.UserInfoMapper;
import com.lsj.tingshu.user.mapper.UserPaidAlbumMapper;
import com.lsj.tingshu.user.mapper.UserPaidTrackMapper;
import com.lsj.tingshu.user.service.UserInfoService;
import com.lsj.tingshu.vo.album.TrackListVo;
import com.lsj.tingshu.vo.user.UserCollectVo;
import com.lsj.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RsaSigner rsaSigner;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;


    @Override
    public Map<String, Object> wxLogin(String code) {


        // 0.定义一个Map
        Map<String, Object> result = new HashMap<>();

        String openId = "";

        // 1.校验code码
        if (StringUtils.isEmpty(code)) {
            throw new TingShuException(500, "code码为空");
        }

        // 2.调用微信服务端的API获取OpenId
        try {
            WxMaUserService userService = wxMaService.getUserService();
            WxMaJscode2SessionResult sessionInfo = userService.getSessionInfo(code);
            openId = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            throw new TingShuException(500, "微信服务端调用失败");
        }

        if (StringUtils.isEmpty(openId)) {
            throw new TingShuException(500, "openId不存在");
        }
        // 3.根据获取到的OpenId查询user_info是否有该用户信息
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getWxOpenId, openId);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

        // 3.1 如果没有，才保存进去
        if (null == userInfo) {
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openId); // 微信openId
            userInfo.setNickname("【hzk】-" + System.currentTimeMillis());
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            userInfo.setIsVip(0); // 用户身份标识 不是vip
            userInfo.setVipExpireTime(new Date());
            int insert = userInfoMapper.insert(userInfo);
            if (insert > 0) {
                log.info("注册用户信息成功");
            } else {
                log.error("注册用户信息失败");
            }
            rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, userInfo.getId().toString());
            log.info("上游用户微服务发送消息{}到队列...", userInfo.getId());

        }
        // 3.2 如果有，则无需保存

        // thought:如果直接用uuid作为令牌
        // 1) 没有代表性--无状态  ---将uuid存到服务端Redis中【分布式组件】
        // 2) jwt:json web token (令牌)---自定义用户信息（载荷：定义信息）

        String accessAndRefreshToken = getJsonWebToken(openId, userInfo.getId().toString());

        // 4.将用户身份令牌保存到Map中
        // 4.1 将Token存储到Redis分布式组件中:常用的数据类型：String、List 、 Set 、ZSet 、Hash[双层Map]
        String accessTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openId;
        redisTemplate.opsForValue().set(accessTokenKey, accessAndRefreshToken, 30, TimeUnit.DAYS); // 测试使用
//        redisTemplate.opsForValue().set(accessTokenKey, accessAndRefreshToken, 30, TimeUnit.MINUTES); //（线上使用） 访问令牌（限制能不能访问受保护资源）
        String refreshTokenKey = RedisConstant.USER_LOGIN_REFRESH_KEY_PREFIX + openId;
        redisTemplate.opsForValue().set(refreshTokenKey, accessAndRefreshToken, 7, TimeUnit.DAYS); // 访问令牌（限制能不能访问受保护资源）


        // 令牌的时间到底如何设置？大：不安全  小：频繁登录【用户体验】
        // 使用双token---accessToken(保证安全性，短"分钟级别")  refreshToken（保证用户体验，长"天或者月级别"）
        // 在公司中真正的编码
//        result.put("accessToken", accessAndRefreshToken); // 访问令牌（用户登录后的自定义状态）
//        result.put("freshToken", accessAndRefreshToken); // 刷新令牌（用户登录后的自定义状态）
        // 4.2 将令牌返回给前端--现有编码
        result.put("token", accessAndRefreshToken);
        // 5.返回Map
        return result;
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {

        UserInfo userInfo = userInfoMapper.selectById(userId);

        if (null == userInfo) {
            throw new TingShuException(500, "该用户不存在");
        }

        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);

        return userInfoVo;
    }

    @Override
    public void updateUser(UserInfoVo userInfoVo) {

        Long userId = AuthContextHolder.getUserId();
        UserInfo userInfo = userInfoMapper.selectById(userId);

        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());

        userInfoMapper.updateById(userInfo);


    }

    @Override
    public Map<String, Object> getNewAccessToken(String refreshToken) {

        // 1.解析jwt
        Jwt jwt = JwtHelper.decodeAndVerify(refreshToken, new RsaVerifier(PublicConstant.PUBLIC_KEY));

        // 2.获取Jwt的载荷数据
        String claims = jwt.getClaims();
        Map payLoad = JSONObject.parseObject(claims, Map.class);
        Object openId = payLoad.get("openId");
        Object userId = payLoad.get("userId");

        // 3.获取refreshToken的key
        String refreshTokenKey = RedisConstant.USER_LOGIN_REFRESH_KEY_PREFIX + openId;
        String refreshTokenValue = redisTemplate.opsForValue().get(refreshTokenKey);

        // 4.判断刷新令牌是否过期
        if (StringUtils.isEmpty(refreshTokenValue)) {
            // 刷新令牌过期 换取不到新accessToken
            throw new TingShuException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 5.没过期 可以换
        String newAccessToken = getJsonWebToken(openId.toString(), userId.toString());

        // 6.存储到Redis中
        String accessTokenKey = RedisConstant.USER_LOGIN_KEY_PREFIX + openId;
        redisTemplate.opsForValue().set(accessTokenKey, newAccessToken, 30, TimeUnit.MINUTES); // 访问令牌（限制能不能访问受保护资源）

        // 旋转refreshToken(作用 相比用之前的，更加的安全)任意一个token被泄露了 都只会在30min之内的窗口攻击。
        redisTemplate.opsForValue().set(refreshTokenKey, newAccessToken, 7, TimeUnit.DAYS); // 访问令牌（限制能不能访问受保护资源）

        // 7.将新的accessToken返回前端
        Map<String, Object> newAccessTokenMap = new HashMap<>();

//        newAccessTokenMap.put("accessToken",newAccessToken); //真实在公司
        newAccessTokenMap.put("token", newAccessToken);
        return newAccessTokenMap;
    }

    @Override
    public Boolean getIsPaidAlbum(Long userId, Long albumId) {


        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getUserId, userId);
        wrapper.eq(UserPaidAlbum::getAlbumId, albumId);

        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
        return userPaidAlbum != null;
    }

    @Override
    public Map<Long, String> getIsPaidAlbumTrack(Long userId, Long albumId) {


        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getAlbumId, albumId);
        wrapper.eq(UserPaidTrack::getUserId, userId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(wrapper);

        Map<Long, String> isPaidAlbumTrack = userPaidTrackList.stream().collect(Collectors.toMap(UserPaidTrack::getTrackId, v -> "1"));
        return isPaidAlbumTrack;
    }

    @Override
    public Boolean isCollect(Long trackId) {

        Long userId = AuthContextHolder.getUserId();
        // 1.构建查询和条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(trackId);
        Query query = new Query(criteria);

        // 2.查询用户收藏记录是否存在

        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId);

        long count = mongoTemplate.count(query, UserCollect.class, collectionName);

        return count > 0;
    }

    @Override
    public Boolean isSubscribe(Long albumId) {


        Long userId = AuthContextHolder.getUserId();
        // 1.构建查询和条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("albumId").is(albumId);
        Query query = new Query(criteria);

        // 2.查询用户订阅记录是否存在

        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId);

        long count = mongoTemplate.count(query, UserSubscribe.class, collectionName);

        return count > 0;
    }

    @Override
    @SneakyThrows
    public IPage<UserCollectVo> findUserCollectPage(IPage<UserCollectVo> userCollectVoPage) {
        // 从mongodb中查询数据 自己完成分页   IPage只是用。

        // 1.构建查询以及条件对象
        Criteria criteria = Criteria.where("userId").is(AuthContextHolder.getUserId());
        Query query = new Query(criteria);


        // 2.总记录查询
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, AuthContextHolder.getUserId());
        long count = mongoTemplate.count(query, UserCollect.class, collectionName);


        // 3.查询数据(用户收藏的声音列表)
        List<UserCollect> userCollects = mongoTemplate.findAll(UserCollect.class, collectionName);

        if (CollectionUtils.isEmpty(userCollects)) {
            return userCollectVoPage;
        }
        // 4.根据用户收藏的声音列表查询声音集合对象

        List<Long> trackListIds = userCollects.stream().map(UserCollect::getTrackId).collect(Collectors.toList());

        Result<List<TrackListVo>> trackListVoResult = albumInfoFeignClient.getTrackVoListByTrackIds(trackListIds);
        List<TrackListVo> trackListVoListData = trackListVoResult.getData();
        Assert.notNull(trackListVoListData, "远程查询专辑微服务获取声音列表失败");

        Map<Long, TrackListVo> trackIdAndTrackVoMap = trackListVoListData.stream().collect(Collectors.toMap(TrackListVo::getTrackId, v -> v));

        List<UserCollectVo> userCollectVoList = userCollects.stream().map(userCollect -> {
            UserCollectVo userCollectVo = new UserCollectVo();
            TrackListVo trackListVo = trackIdAndTrackVoMap.get(userCollect.getTrackId());
            userCollectVo.setAlbumId(trackListVo.getAlbumId());
            userCollectVo.setTrackId(userCollect.getTrackId());
            userCollectVo.setCreateTime(userCollect.getCreateTime());
            userCollectVo.setTrackTitle(trackListVo.getTrackTitle()); // 声音标题
            userCollectVo.setCoverUrl(trackListVo.getCoverUrl()); // 声音封面
            return userCollectVo;
        }).collect(Collectors.toList());

        return userCollectVoPage.setRecords(userCollectVoList).setTotal(count);
    }

    @Override
    public UserInfo getUserInfoByOpenId(String openId) {

        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openId));
        return userInfo;
    }

    @Override
    public void updateExpireTimeVip() {
        // 1. 查询
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getIsVip, 1);
        queryWrapper.lt(UserInfo::getVipExpireTime, new Date());
        List<UserInfo> userInfos = userInfoMapper.selectList(queryWrapper);
        // 2.修改
        userInfos.stream().forEach(
                userInfo ->
                { userInfo.setIsVip(0);
                    userInfoMapper.updateById(userInfo);
                });
    }

    private String getJsonWebToken(String openId, String userId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userId);
        jsonObject.put("openId", openId);
        Jwt jwt = JwtHelper.encode(jsonObject.toString(), rsaSigner);
        String jsonWebToken = jwt.getEncoded();
        return jsonWebToken;
    }
}
