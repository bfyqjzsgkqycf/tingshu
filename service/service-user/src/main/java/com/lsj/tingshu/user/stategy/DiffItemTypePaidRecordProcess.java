package com.lsj.tingshu.user.stategy;


import com.lsj.tingshu.vo.user.UserPaidRecordVo;

public interface DiffItemTypePaidRecordProcess {


    /**
     * 根据对应的付款项类型 执行对应的逻辑
     */
    void processPaidRecord(UserPaidRecordVo userPaidRecordVo);

}
