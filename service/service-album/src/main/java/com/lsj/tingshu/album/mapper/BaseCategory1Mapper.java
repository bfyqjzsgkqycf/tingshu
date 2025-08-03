package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.album.BaseCategory1;
import com.lsj.tingshu.vo.category.CategoryVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BaseCategory1Mapper extends BaseMapper<BaseCategory1> {
    /**
     * 获取一级分类
     * @return
     */
    List<CategoryVo> getCategoryList();
}
