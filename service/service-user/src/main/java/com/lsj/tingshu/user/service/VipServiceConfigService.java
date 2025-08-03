package com.lsj.tingshu.user.service;

import com.lsj.tingshu.model.user.VipServiceConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface VipServiceConfigService extends IService<VipServiceConfig> {

    List<VipServiceConfig> findAll();

}
