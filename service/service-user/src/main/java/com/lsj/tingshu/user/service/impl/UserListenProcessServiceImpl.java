package com.lsj.tingshu.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.common.service.constant.RedisConstant;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.common.util.MongoUtil;
import com.lsj.tingshu.model.user.UserListenProcess;
import com.lsj.tingshu.user.service.UserListenProcessService;
import com.lsj.tingshu.vo.album.TrackStatMqVo;
import com.lsj.tingshu.vo.user.UserListenProcessVo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public BigDecimal getTrackBreakSecond(Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        // 1.构建查询对象和条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(trackId);
        Query query = new Query(criteria);
        // 2.查询
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (userListenProcess == null) {
            return BigDecimal.ZERO;
        }
        // 3.返回
        return userListenProcess.getBreakSecond();
    }

    @Override
    public void updateListenProcess(UserListenProcessVo process) {
        Long userId = AuthContextHolder.getUserId();
        BigDecimal breakSecond = process.getBreakSecond();
        Long trackId = process.getTrackId();
        // 1.构建查询对象和条件对象
        Criteria criteria = Criteria.where("userId").is(userId).and("trackId").is(trackId);
        Query query = new Query(criteria);
        // 2.查询播放记录是否存在
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);

        // 2.1 没播放记录就保存
        if (userListenProcess == null) {
            userListenProcess = new UserListenProcess();
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setAlbumId(process.getAlbumId());
            userListenProcess.setTrackId(trackId);
            userListenProcess.setBreakSecond(breakSecond);
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, collectionName);
        } else {
            // 2.2 有播放记录就修改
            userListenProcess.setBreakSecond(breakSecond);
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, collectionName);
        }
        // 分布式锁实现 bitmap redisson
        // 获取当下年月日
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String bitmapKey = RedisConstant.TRACK_STAT_KEY_PREFIX + simpleDateFormat.format(new Date()) + ":" + process.getTrackId();
        Boolean aBoolean = redisTemplate.opsForValue().setBit(bitmapKey, userId, true);
        if (!aBoolean) {
            redisTemplate.expire(bitmapKey, 1, TimeUnit.DAYS);
            // 3 更新声音关联播放量 远程 异步
            TrackStatMqVo trackStatMqVo = prepareTrackStatMqVo(process, SystemConstant.TRACK_STAT_PLAY);
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_TRACK,
                    MqConst.ROUTING_TRACK_STAT_UPDATE,
                    JSONObject.toJSONString(trackStatMqVo)
            );
        }

    }

    @Override
    public Map<String, Object> getLatelyTrack() {
        Long userId = AuthContextHolder.getUserId();
        // 1.构建查询对象和条件对象
        Criteria criteria = Criteria.where("userId").is(userId);
        Query query = new Query(criteria);
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        Query queryWithSort = query.with(sort);
        // 2.查询播放记录是否存在
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        UserListenProcess userListenProcess = mongoTemplate.findOne(queryWithSort, UserListenProcess.class, collectionName);
        if (userListenProcess == null) {
            return new HashMap<String, Object>();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("albumId", userListenProcess.getAlbumId());
        map.put("trackId", userListenProcess.getTrackId());
        return map;
    }

    private TrackStatMqVo prepareTrackStatMqVo(UserListenProcessVo process, String albumStatPlay) {
        TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
        trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replace("-", ""));
        trackStatMqVo.setAlbumId(process.getAlbumId());
        trackStatMqVo.setTrackId(process.getTrackId());
        trackStatMqVo.setStatType(albumStatPlay);
        trackStatMqVo.setCount(1);
        return trackStatMqVo;
    }
}
