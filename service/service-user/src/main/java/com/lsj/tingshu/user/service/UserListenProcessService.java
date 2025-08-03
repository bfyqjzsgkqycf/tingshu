package com.lsj.tingshu.user.service;

import com.lsj.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取声音播放进度
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long trackId);

    /**
     * 更新播放进度
     * @param userListenProcess
     */
    void updateListenProcess(UserListenProcessVo userListenProcess);

    /**
     * 获取最近播放声音
     * @return
     */
    Map<String, Object> getLatelyTrack();

}
