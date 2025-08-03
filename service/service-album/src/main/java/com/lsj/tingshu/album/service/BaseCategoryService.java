package com.lsj.tingshu.album.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.model.album.BaseAttribute;
import com.lsj.tingshu.model.album.BaseCategory1;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.model.album.BaseCategoryView;
import com.lsj.tingshu.vo.category.CategoryVo;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {
    List<CategoryVo> getBaseCategoryList();

    List<BaseAttribute> findAttributeList(Long c1Id);


    BaseCategoryView getCategoryInfoByAlbumId(long albumId);

    List<BaseCategory3> findTopBaseCategory3(Long c1Id);

    CategoryVo getBaseCategoryListByC1Id(Long c1Id);

    List<Long> getAllCategory1Ids();
}
