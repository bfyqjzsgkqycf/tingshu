package com.lsj.tingshu.album.service.impl;

import com.lsj.tingshu.album.mapper.AlbumInfoMapper;
import com.lsj.tingshu.album.mapper.TrackInfoMapper;
import com.lsj.tingshu.album.mapper.TrackStatMapper;
import com.lsj.tingshu.album.service.TrackInfoService;
import com.lsj.tingshu.album.service.VodService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.model.album.TrackStat;
import com.lsj.tingshu.query.album.TrackInfoQuery;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.lsj.tingshu.vo.album.AlbumTrackListVo;
import com.lsj.tingshu.vo.album.TrackInfoVo;
import com.lsj.tingshu.vo.album.TrackListVo;
import com.lsj.tingshu.vo.album.TrackMediaInfoVo;
import com.lsj.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private VodService vodService;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Override
    @SneakyThrows
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId) {


        List<Map<String, Object>> result = new ArrayList<>();

        // 1.根据声音id 查询声音信息
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);

        if (trackInfo == null) {
            throw new TingShuException(500, "当前声音不存在");
        }
        Integer currentTrackOrderNum = trackInfo.getOrderNum();
        // 2.根据专辑id 查询专辑信息
        Long albumId = trackInfo.getAlbumId();
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new TingShuException(500, "该声音对应的专辑不存在");
        }
        BigDecimal price = albumInfo.getPrice(); // 单集声音的价格
        Long userId = AuthContextHolder.getUserId();

        // 3.查询当前声音之后的n(50)集声音。
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, albumId);
        wrapper.gt(TrackInfo::getOrderNum, currentTrackOrderNum);
        wrapper.orderByAsc(TrackInfo::getOrderNum);
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(wrapper);

        // 4.将买过的声音过滤掉
        // 4.1 查询当前用户购买过的声音
        Result<Map<Long, String>> isPaidAlbumTrackResult = userInfoFeignClient.getIsPaidAlbumTrack(userId, albumId);
        Map<Long, String> isPaidAlbumTrackMap = isPaidAlbumTrackResult.getData();
        Assert.notNull(isPaidAlbumTrackMap, "远程调用用户微服务获取购买声音失败");

        // 4.2 遍历要分集展示的声音列表
        List<TrackInfo> canTrackPayTrackList = trackInfoList.stream().filter(trackInfo1 -> StringUtils.isEmpty(isPaidAlbumTrackMap.get(trackInfo1.getId()))).limit(50).collect(Collectors.toList());

        // 5.分块展示(当前用户没买的声音)
        // 5.1 构建本集
        Map<String, Object> map = new HashMap<>();
        map.put("name", "本集");
        map.put("price", price);
        map.put("trackCount", 0);  // 本集给一个标识0 （代表本集）
        result.add(map);

        // 5.2 构建其他集
        // 1)计算块数
        int canTrackPaySize = canTrackPayTrackList.size();
        int block = canTrackPaySize % 10 == 0 ? canTrackPaySize / 10 : canTrackPaySize / 10 + 1;
        // 假设：canTrackPaySize:32   block=4
        for (int i = 1; i <= block; i++) {
            int trackCount = i * 10;
            if (trackCount >= canTrackPaySize) {
                Map<String, Object> lastMap = new HashMap<>();
                lastMap.put("name", "后" + canTrackPaySize + "集");
                lastMap.put("price", price.multiply(new BigDecimal(canTrackPaySize))); // 后多少集的价格
                lastMap.put("trackCount", canTrackPaySize);  // 后多少集的集数
                result.add(lastMap);
                break;
            }
            Map<String, Object> currentMap = new HashMap<>();
            currentMap.put("name", "后" + trackCount + "集");
            currentMap.put("price", price.multiply(new BigDecimal(trackCount))); // 后多少集的价格
            currentMap.put("trackCount", trackCount);  // 后多少集的集数
            result.add(currentMap);

        }
        return result;
    }

    @Override
    public List<TrackInfo> getNeedPayTrackList(Long userId, Long currentTrackId, Integer trackCount) {


        List<TrackInfo> trackInfos = new ArrayList<>();

        // 1.根据声音id 查询声音对象
        TrackInfo trackInfo = trackInfoMapper.selectById(currentTrackId);
        if (trackInfo == null) {
            throw new TingShuException(500, "当前声音不存在");
        }
        Integer currentTrackOrderNum = trackInfo.getOrderNum();
        // 2.根据专辑id 查询专辑对象
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        if (albumInfo == null) {
            throw new TingShuException(500, "当前声音对应的专辑不存在");
        }

        // 3.判断trackCount
        // 3.1 购买本集
        if (trackCount == 0) {
            trackInfos.add(trackInfo);
        } else {
            //3.2 后n集 [10 13]
            LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TrackInfo::getAlbumId, albumInfo.getId());
            wrapper.ge(TrackInfo::getOrderNum, currentTrackOrderNum);// 8  gt
            wrapper.orderByAsc(TrackInfo::getOrderNum);
            wrapper.last("limit " + trackCount);
            List<TrackInfo> trackInfoList = trackInfoMapper.selectList(wrapper);  //
            trackInfos = trackInfoList;
        }

        return trackInfos;
    }

    @Override
    public AlbumInfo getAlbumInfoByTrackId(Long currentTrackId) {
        TrackInfo trackInfo = trackInfoMapper.selectById(currentTrackId);
        if (trackInfo == null) {
            throw new TingShuException(500, "当前声音对象不存在");
        }
        Long albumId = trackInfo.getAlbumId();

        return albumInfoMapper.selectById(albumId);
    }

    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        return vodService.uploadTrack(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
        // 1.1保存声音基本信息
        TrackInfo trackInfo = new TrackInfo();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        trackInfo.setUserId(userId);
        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
        // 1.2保存声音orderNum
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrackInfo::getAlbumId, trackInfoVo.getAlbumId());
        queryWrapper.orderByDesc(TrackInfo::getOrderNum);
        TrackInfo latestTrackInfo = trackInfoMapper.selectOne(queryWrapper);
        int orderNum = latestTrackInfo == null ? 1 : latestTrackInfo.getOrderNum() + 1;
        trackInfo.setOrderNum(orderNum);
        // 1.3保存声音媒体信息
        TrackMediaInfoVo trackMediaInfoVo = vodService.getMediainfo(trackInfoVo.getMediaFileId());
        if (trackMediaInfoVo == null) {
            throw new TingShuException(500, "媒体信息不存在");
        }
        trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
        trackInfo.setMediaDuration(new BigDecimal(trackMediaInfoVo.getDuration()));
        trackInfo.setMediaSize(trackMediaInfoVo.getSize());
        trackInfo.setMediaType(trackMediaInfoVo.getType());
        trackInfoMapper.insert(trackInfo);
        // 2.保存声音统计信息
        saveTrackStat(trackInfo.getId());
        // 3.反向修改声音所属专辑的声音总数
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        albumInfoMapper.updateById(albumInfo);
    }

    @Override
    public IPage<TrackListVo> findUserTrackPage(IPage page, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.findUserTrackPage(page, trackInfoQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo) {
        // 1.查询修改之前的声音信息
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        if (trackInfo == null) {
            throw new TingShuException(500, "声音不存在");
        }
        // 2.从trackInfoVo获取媒体资源的文件id
        String newMediaFileId = trackInfoVo.getMediaFileId();
        if (StringUtils.isEmpty(newMediaFileId)) {
            throw new TingShuException(500, "媒体文件不存在");
        }
        // 3.从trackInfo获取媒体资源的文件id
        String oldMediaFileId = trackInfo.getMediaFileId();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        // 4.判断
        if (!newMediaFileId.equals(oldMediaFileId)) {
            TrackMediaInfoVo mediainfo = vodService.getMediainfo(newMediaFileId);
            if (mediainfo != null) {
                trackInfo.setMediaUrl(mediainfo.getMediaUrl());
                trackInfo.setMediaDuration(new BigDecimal(mediainfo.getDuration()));
                trackInfo.setMediaSize(mediainfo.getSize());
                trackInfo.setMediaType(mediainfo.getType());
            } else {
                throw new TingShuException(500, "媒体文件不存在");
            }
        }
        trackInfoMapper.updateById(trackInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long trackId) {
        // 2.反向修改声音所属专辑的声音总数
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);
        // 3.删除媒体资源
        vodService.removeMedia(trackInfo.getMediaFileId());
        // 4.删除声音
        trackInfoMapper.deleteById(trackId);
        // 1.更新声音统计信息
        trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
    }

    @Override
    public IPage<AlbumTrackListVo> findAlbumTrackPage(Long albumId, IPage<AlbumTrackListVo> page) {
        IPage<AlbumTrackListVo> finalAlbumTrackListVo = null;

        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        Assert.notNull(albumInfo, "专辑信息不存在");
        // 付费类型: 0101-免费、0102-vip免费、0103-付费
        String payType = albumInfo.getPayType();
        // 价格类型： 0201-单集 0202-整专辑 【声音购买不支持折扣】
        String priceType = albumInfo.getPriceType();
        // 免费声音集数 默认 5
        Integer tracksForFree = albumInfo.getTracksForFree();

        // 根据 albumId 分页查询当前专辑下的声音列表
        IPage<AlbumTrackListVo> currentAlbumTrackList = trackInfoMapper.findAlbumTrackPage(albumId, page);

        // 根据专辑付费类型修改声音的isShowPaidMark字段
        switch (payType) {
            case "0101": // 免费
                finalAlbumTrackListVo = currentAlbumTrackList;
                break;
            case "0102": // vip免费
                finalAlbumTrackListVo = dealAlbumTypeVip(albumId, currentAlbumTrackList, priceType, tracksForFree, AuthContextHolder.getUserId());
                break;

            case "0103": // 付费
                finalAlbumTrackListVo = dealAlbumTypeNeedPay(albumId, currentAlbumTrackList, priceType, tracksForFree, AuthContextHolder.getUserId());

        }

        return finalAlbumTrackListVo;
    }

    @Override
    public TrackInfoVo getTrackStatVo(Long trackId) {
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        TrackInfoVo trackInfoVo = new TrackInfoVo();
        BeanUtils.copyProperties(trackInfo, trackInfoVo);
        return trackInfoVo;
    }

    @Override
    public List<TrackListVo> getTrackVoListByTrackIds(List<Long> userCollectTrackIdList) {
        List<TrackInfo> trackInfos = trackInfoMapper.selectBatchIds(userCollectTrackIdList);
        List<TrackListVo> collect = trackInfos.stream().map(trackInfo -> {
            TrackListVo trackListVo = new TrackListVo();
            BeanUtils.copyProperties(trackInfo, trackListVo);
            trackListVo.setTrackId(trackInfo.getId());
            return trackListVo;
        }).collect(Collectors.toList());
        return collect;
    }

    private IPage<AlbumTrackListVo> dealAlbumTypeNeedPay(Long albumId, IPage<AlbumTrackListVo> currentAlbumTrackList, String priceType, Integer tracksForFree, Long userId) {
        // 1.1 如果不存在，用户没登录，返回免费五集声音。
        if (null == userId) {
            List<AlbumTrackListVo> noNeedLoginAndAlbumTrack = currentAlbumTrackList.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() <= tracksForFree).collect(Collectors.toList());
            return currentAlbumTrackList.setRecords(noNeedLoginAndAlbumTrack);
        }

        // 1.2 如果存在 修改其它需要付费展示的声音图标
        return getReallyAlbumTrackListVoIPage(albumId, currentAlbumTrackList, priceType, tracksForFree, userId);

    }

    /**
     * 处理vip付费类型的专辑
     *
     * @param albumId
     * @param currentAlbumTrackList
     * @param priceType
     * @param tracksForFree
     * @param userId
     * @return
     */
    @SneakyThrows
    private IPage<AlbumTrackListVo> dealAlbumTypeVip(Long albumId, IPage<AlbumTrackListVo> currentAlbumTrackList, String priceType, Integer tracksForFree, Long userId) {

        // 1.判断用户id 是否存在
        // 1.1 如果不存在，用户没登录，返回免费五集声音。
        if (null == userId) {
            List<AlbumTrackListVo> noNeedLoginAndAlbumTrack = currentAlbumTrackList.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() <= tracksForFree).collect(Collectors.toList());
            return currentAlbumTrackList.setRecords(noNeedLoginAndAlbumTrack);
        }

        // 1.2 如果存在，用户登录
        Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfo(userId.toString());
        UserInfoVo userInfoData = userInfoResult.getData();
        Assert.notNull(userInfoData, "该用户不存在");

        // 2. 判断用户的身份
        Integer isVip = userInfoData.getIsVip();
        Date vipExpireTime = userInfoData.getVipExpireTime();
        if ("0".equals(isVip + "") || ("1").equals(isVip + "") && vipExpireTime.before(new Date())) {
            // 处理 修改付费标识
            // 价格类型是否是单集还是整专辑
            // 2.1 如果不是vip或者是vip但是过期了
            return getReallyAlbumTrackListVoIPage(albumId, currentAlbumTrackList, priceType, tracksForFree, userId);
        } else {
            // 2.2 是vip还没过期  全部直接看 不需要修改付费图标
            return currentAlbumTrackList;
        }

    }

    private IPage<AlbumTrackListVo> getReallyAlbumTrackListVoIPage(Long albumId, IPage<AlbumTrackListVo> currentAlbumTrackList, String priceType, Integer tracksForFree, Long userId) {
        // 1.处理单集类型
        if (priceType.equals("0201")) { // 单集
            // 查询当前用户买过当前专辑下的哪些声音

            Result<Map<Long, String>> isPaidAlbumTrackMapResult = userInfoFeignClient.getIsPaidAlbumTrack(userId, albumId);
            Map<Long, String> isPaidAlbumTrackData = isPaidAlbumTrackMapResult.getData();
            Assert.notNull(isPaidAlbumTrackData, "远程查询用户微服务获取专辑下购买的声音列表失败");

            List<AlbumTrackListVo> collect = currentAlbumTrackList.getRecords().stream().map(albumTrackListVo -> {
                if (StringUtils.isEmpty(isPaidAlbumTrackData.get(albumTrackListVo.getTrackId())) && albumTrackListVo.getOrderNum() > tracksForFree) {
                    albumTrackListVo.setIsShowPaidMark(true);
                }
                return albumTrackListVo;
            }).collect(Collectors.toList());
            return currentAlbumTrackList.setRecords(collect);

        } else {
            // 2.处理单集类型
            // 查询用户是否购买过整专辑
            Result<Boolean> isPaidAlbumResult = userInfoFeignClient.getIsPaidAlbum(userId, albumId);
            Boolean isPaidAlbumData = isPaidAlbumResult.getData();
            Assert.notNull(isPaidAlbumData, "远程查询用户微服务获取用户购买当前专辑失败");
            if (isPaidAlbumData) {
                return currentAlbumTrackList;
            } else {
                List<AlbumTrackListVo> collect = currentAlbumTrackList.getRecords().stream().map(albumTrackListVo -> {
                    if (albumTrackListVo.getOrderNum() > tracksForFree) {
                        albumTrackListVo.setIsShowPaidMark(true);
                    }
                    return albumTrackListVo;
                }).collect(Collectors.toList());
                return currentAlbumTrackList.setRecords(collect);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveTrackStat(Long trackId) {
        List<String> trackStatList = new ArrayList<>();
        trackStatList.add(SystemConstant.TRACK_STAT_PLAY);
        trackStatList.add(SystemConstant.TRACK_STAT_COLLECT);
        trackStatList.add(SystemConstant.TRACK_STAT_PRAISE);
        trackStatList.add(SystemConstant.TRACK_STAT_COMMENT);

        for (String ts : trackStatList) {
            TrackStat trackStat = new TrackStat();
            trackStat.setTrackId(trackId);
            trackStat.setStatType(ts);
            trackStat.setStatNum(0);
            trackStatMapper.insert(trackStat);
        }
    }
}
