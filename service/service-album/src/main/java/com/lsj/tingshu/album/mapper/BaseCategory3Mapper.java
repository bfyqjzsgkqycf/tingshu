package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.album.BaseCategory3;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseCategory3Mapper extends BaseMapper<BaseCategory3> {
    List<BaseCategory3> findTopBaseCategory3(@Param("c1Id") Long c1Id);
}
