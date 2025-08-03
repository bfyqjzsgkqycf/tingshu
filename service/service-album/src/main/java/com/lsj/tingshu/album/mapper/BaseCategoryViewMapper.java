package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.album.BaseCategoryView;
import com.lsj.tingshu.vo.category.CategoryVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    BaseCategoryView getCategoryInfoByAlbumId(@Param("albumId") long albumId);

    CategoryVo getBaseCategoryListByC1Id(@Param("c1Id") Long c1Id);
}
