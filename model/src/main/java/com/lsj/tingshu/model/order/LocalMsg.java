package com.lsj.tingshu.model.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;


@TableName("t_local_msg")
@Data
public class LocalMsg {


    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "msg_content")
    private String msgContent;
    @TableField(value = "status")
    private Integer status;

    @TableField("create_time")
    private Date createTime;   //  Mon Sep 02 10:38:08 CST 2024

    @JsonIgnore // 不会参与序列化
    @TableField("update_time")
    private Date updateTime;


}
