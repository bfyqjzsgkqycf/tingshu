package com.lsj.tingshu.album.service.impl;

import com.lsj.tingshu.album.config.VodProperties;
import com.lsj.tingshu.album.service.VodService;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.UploadFileUtil;
import com.lsj.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class VodServiceImpl implements VodService {

    @Autowired
    private VodProperties vodProperties;

    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        Map<String, Object> map = new HashMap<>();
        // 初始化一个上传客户端对象
        VodUploadClient client = new VodUploadClient(vodProperties.getSecretId(), vodProperties.getSecretKey());
        // 构造上传请求对象
        VodUploadRequest request = new VodUploadRequest();
        String filePath = UploadFileUtil.uploadTempPath(vodProperties.getTempPath(), file);
        request.setMediaFilePath(filePath);
        request.setSubAppId(vodProperties.getAppId());
        // 调用上传
        try {
            VodUploadResponse response = client.upload(vodProperties.getRegion(), request);
            String fileId = response.getFileId();
            String mediaUrl = response.getMediaUrl();
            map.put("mediaFileId", fileId);
            map.put("mediaUrl", mediaUrl);
            log.info("音频上传成功");
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("音频上传失败!{}", e.getMessage());
            throw new TingShuException(500, "音频上传失败");
        }
        return map;
    }

    @Override
    public TrackMediaInfoVo getMediainfo(String mediaFileId) {
        try {
            Credential cred = new Credential(vodProperties.getSecretId(), vodProperties.getSecretKey());
            // 实例化要请求产品的client对象
            VodClient client = new VodClient(cred, vodProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);
            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
            TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
            MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
            if (mediaInfoSet != null && mediaInfoSet.length > 0) {
                trackMediaInfoVo.setMediaUrl(mediaInfoSet[0].getBasicInfo().getMediaUrl());
                trackMediaInfoVo.setDuration(mediaInfoSet[0].getMetaData().getAudioDuration());
                trackMediaInfoVo.setSize(mediaInfoSet[0].getMetaData().getSize());
                trackMediaInfoVo.setType(mediaInfoSet[0].getBasicInfo().getType());
                return trackMediaInfoVo;
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @Override
    public void removeMedia(String mediaFileId) {
        try{
            Credential cred = new Credential(vodProperties.getSecretId(), vodProperties.getSecretKey());
            // 实例化要请求产品的client对象
            VodClient client = new VodClient(cred, vodProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            DeleteMediaResponse resp = client.DeleteMedia(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
    }
}
