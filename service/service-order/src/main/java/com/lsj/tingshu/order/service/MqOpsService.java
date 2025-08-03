package com.lsj.tingshu.order.service;

public interface MqOpsService {

    /**
     * 修改本地消息表状态
     *
     * @param content
     */
    void localMsgStatusUpdate(String content);

    void closeOrder(String content);

    void wxPaidSuccess(String content);

}