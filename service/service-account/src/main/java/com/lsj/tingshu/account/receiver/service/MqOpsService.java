package com.lsj.tingshu.account.receiver.service;

public interface MqOpsService {


    public  void  registerUserAccount(String  message);

    void userAccountMinus(String content);

    void userAccountUnlock(String content);

    void rechargePaidSuccess(String content);


}
