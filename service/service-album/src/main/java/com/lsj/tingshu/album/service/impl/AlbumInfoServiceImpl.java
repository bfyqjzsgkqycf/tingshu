package com.lsj.tingshu.album.service.impl;

import com.lsj.tingshu.album.mapper.*;
import com.lsj.tingshu.album.service.AlbumAttributeValueService;
import com.lsj.tingshu.album.service.AlbumInfoService;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.model.album.*;
import com.lsj.tingshu.query.album.AlbumInfoQuery;
import com.lsj.tingshu.vo.album.AlbumAttributeValueVo;
import com.lsj.tingshu.vo.album.AlbumInfoVo;
import com.lsj.tingshu.vo.album.AlbumListVo;
import com.lsj.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {
        // 三个保存

        // 1.基本信息表 album_info
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        Long userId = AuthContextHolder.getUserId();
        albumInfo.setUserId(userId);
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        String payType = albumInfoVo.getPayType();
        if (!payType.equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) {
            albumInfo.setTracksForFree(5);
        }
        int insert = albumInfoMapper.insert(albumInfo);
        log.info("保存专辑基本信息:{}", insert > 0 ? "成功" : "失败");

        // 2.标签信息表 album_attribute_value
        for (AlbumAttributeValueVo albumAttributeValueVo : albumInfoVo.getAlbumAttributeValueVoList()) {
            AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            albumAttributeValue.setAlbumId(albumInfo.getId());
            albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
            albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
            int insert2 = albumAttributeValueMapper.insert(albumAttributeValue);
            log.info("保存专辑标签信息:{}", insert2 > 0 ? "成功" : "失败");
        }

        // 3.数据统计表 album_stat
        saveAlbumStat(albumInfo.getId());

        // 4.上架专辑到ES
        if ("1".equals(albumInfo.getIsOpen())) {
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfo.getId());
        }
    }

    @Override
    public IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
        return albumInfoMapper.findUserAlbumPage(albumListVoPage, albumInfoQuery);
    }

    @Override
    public AlbumInfo getAlbumInfo(Long albumId) {
        // 1.查询基本信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new TingShuException(500, "专辑不存在");
        }
        // 2.查询标签信息
        List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper
                .selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
        return albumInfo;
    }

    @Override
    public void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo) {
        // 1.修改基本信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        if (albumInfo == null) {
            throw new TingShuException(500, "专辑不存在");
        }
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        if (albumInfoVo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) {
            albumInfo.setPrice(new BigDecimal(0.00));
            albumInfo.setDiscount(new BigDecimal(0.00));
            albumInfo.setVipDiscount(new BigDecimal(0.00));
        } else {
            albumInfo.setTracksForFree(5);
        }
        albumInfoMapper.updateById(albumInfo);
        // 2.修改标签信息 修改中间表 策略删除原来信息再添加
        albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        List<AlbumAttributeValue> albumAttributeValueList = albumInfoVo.getAlbumAttributeValueVoList().stream().map(albumAttributeValueVo -> {
            AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
            albumAttributeValue.setAlbumId(albumId);
            albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
            albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
            return albumAttributeValue;
        }).collect(Collectors.toList());
        if (!StringUtils.isEmpty(albumAttributeValueList)) {
            albumAttributeValueService.saveBatch(albumAttributeValueList);
        }
        // 3.专辑统计信息不修改

        // 4.上架专辑到ES
        if ("1".equals(albumInfo.getIsOpen())) {
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfo.getId());
        } else {
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, albumInfo.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeAlbumInfo(Long albumId) {
        // 1.判断 若专辑有声音 不能删除
        Long count = trackInfoMapper.selectCount(new LambdaQueryWrapper<TrackInfo>().eq(TrackInfo::getAlbumId, albumId));
        if (count > 0) {
            throw new TingShuException(500, "专辑有声音，不能删除");
        }
        // 2.删除基本信息
        albumInfoMapper.deleteById(albumId);
        // 3.删除标签信息
        albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        // 4.删除专辑统计信息
        albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        // 5.从ES下架专辑
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, albumId);
    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {

        return albumInfoMapper.selectList(new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId, userId));

    }

    @Override
    public AlbumStatVo getAlbumStatByAlbumId(long albumId) {
        return albumInfoMapper.getAlbumStatByAlbumId(albumId);
    }

    @Override
    public List<BaseCategory3> findTopBaseCategory3List(Long c1Id) {
        return baseCategory3Mapper.findTopBaseCategory3(c1Id);
    }

    @Override
    public void saveAlbumStat(Long albumId) {
        List<String> albumStatList = new ArrayList<>();
        albumStatList.add(SystemConstant.ALBUM_STAT_PLAY);
        albumStatList.add(SystemConstant.ALBUM_STAT_SUBSCRIBE);
        albumStatList.add(SystemConstant.ALBUM_STAT_BROWSE);
        albumStatList.add(SystemConstant.ALBUM_STAT_COMMENT);

        for (String as : albumStatList) {
            AlbumStat albumStat = new AlbumStat();
            albumStat.setAlbumId(albumId);
            albumStat.setStatType(as);
            albumStat.setStatNum(0);
            int insert = albumStatMapper.insert(albumStat);
            log.info("保存专辑统计信息:{}", insert > 0 ? "成功" : "失败");
        }
    }

    @Override
    public List<Long> getAlbumInfoIds() {

        List<AlbumInfo> albumInfos = albumInfoMapper.selectList(null);

        List<Long> ids = albumInfos.stream().map(AlbumInfo::getId).collect(Collectors.toList());
        return ids;
    }
}
