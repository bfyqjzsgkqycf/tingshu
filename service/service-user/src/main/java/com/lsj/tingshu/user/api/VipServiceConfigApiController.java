package com.lsj.tingshu.user.api;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.user.VipServiceConfig;
import com.lsj.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user/vipServiceConfig")
@SuppressWarnings({"unchecked", "rawtypes"})
public class VipServiceConfigApiController {

    @Autowired
    private VipServiceConfigService vipServiceConfigService;

    // Request URL: http://localhost:8500/api/user/vipServiceConfig/findAll
    @GetMapping("/findAll")
    public Result findAll() {
        List<VipServiceConfig> list = vipServiceConfigService.findAll();
        return Result.ok(list);
    }

}

