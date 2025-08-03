package com.lsj.tingshu.cdc.entity;

import lombok.Data;

import javax.persistence.Column;

@Data
public class CdcEntity {

    // 注意Column 注解必须是persistence包下的
    @Column(name = "id")
    private Long id;  // 专辑id (删除缓存)---缓存key(常量+专辑id)
    //....
}
