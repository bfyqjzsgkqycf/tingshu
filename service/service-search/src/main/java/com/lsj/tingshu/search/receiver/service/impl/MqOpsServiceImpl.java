package com.lsj.tingshu.search.receiver.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.cache.exception.TingShuException;
import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.search.receiver.service.MqOpsService;
import com.lsj.tingshu.search.repository.AlbumInfoIndexRepository;
import com.lsj.tingshu.search.service.ItemService;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;


    @Override
    public void listenAlbumOnSale(String albumId) {
        try {
            itemService.onSaleAlbum(Long.parseLong(albumId));
        } catch (TingShuException e) {
            throw new TingShuException(500, "专辑上架到Es中失败");
        }
    }

    @Override
    public void listenAlbumOffSale(String albumId) {

        try {
            itemService.onOffAlbum(Long.parseLong(albumId));
        } catch (TingShuException e) {
            throw new TingShuException(500, "专辑从Es中下架失败");
        }
    }

    @Override
    @SneakyThrows
    public void elasticSearchAlbumStatUpdate(String content) {
        // 1.反序列化
        UserPaidRecordVo userPaidRecordVo = JSONObject.parseObject(content, UserPaidRecordVo.class);

        // 2.获取属性
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String itemType = userPaidRecordVo.getItemType();
        Long albumId = 0L;
        // 3.判断付款项类型
        if ("1001".equals(itemType)) {
            albumId = itemIdList.get(0);

        } else if ("1002".equals(itemType)) {
            Long trackId = itemIdList.get(0);
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoByTrackId(trackId);
            AlbumInfo albumInfoData = albumInfoResult.getData();
            Assert.notNull(albumInfoData, "远程调用专辑微服务失败");
            albumId = albumInfoData.getId();
        } else {
            return;
        }

        albumInfoIndexRepository.findById(albumId).ifPresent(albumInfoIndex -> {
            Integer buyStatNum = albumInfoIndex.getBuyStatNum();
            albumInfoIndex.setBuyStatNum(buyStatNum + itemIdList.size());
            albumInfoIndexRepository.save(albumInfoIndex);
        });
    }
}
