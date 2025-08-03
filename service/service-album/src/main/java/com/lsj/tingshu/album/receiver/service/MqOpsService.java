package com.lsj.tingshu.album.receiver.service;

public interface MqOpsService {

    void albumAndTrackStatUpdate(String content);

    void albumStatUpdate(String content);

}
