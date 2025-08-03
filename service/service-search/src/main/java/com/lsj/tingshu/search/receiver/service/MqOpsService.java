package com.lsj.tingshu.search.receiver.service;

public interface MqOpsService {

    /**
     * 专辑上架
     * @param content
     */
    void listenAlbumOnSale(String content);

    /**
     * 下架专辑
     * @param content
     */
    void listenAlbumOffSale(String content);

    void elasticSearchAlbumStatUpdate(String content);

}
