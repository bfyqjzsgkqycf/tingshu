package com.lsj.tingshu.album.service.impl;

import com.lsj.tingshu.album.mapper.*;
import com.lsj.tingshu.album.service.BaseCategoryService;
import com.lsj.tingshu.model.album.BaseAttribute;
import com.lsj.tingshu.model.album.BaseCategory1;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.model.album.BaseCategoryView;
import com.lsj.tingshu.vo.category.CategoryVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;


    @Override
    public List<CategoryVo> getBaseCategoryList() {
        // return getBaseCategoryListV1();
        return getBaseCategoryListV2();
    }

    @Override
    public List<BaseAttribute> findAttributeList(Long c1Id) {
        return baseAttributeMapper.findAttributeList(c1Id);
    }

    @Override
    public BaseCategoryView getCategoryInfoByAlbumId(long albumId) {
        return baseCategoryViewMapper.getCategoryInfoByAlbumId(albumId);
    }

    @Override
    public List<BaseCategory3> findTopBaseCategory3(Long c1Id) {
        return baseCategory3Mapper.findTopBaseCategory3(c1Id);
    }

    @Override
    public CategoryVo getBaseCategoryListByC1Id(Long c1Id) {
        return baseCategoryViewMapper.getBaseCategoryListByC1Id(c1Id);
    }

    @Override
    public List<Long> getAllCategory1Ids() {
        return baseCategory1Mapper.selectList(null).stream().map(BaseCategory1::getId).collect(Collectors.toList());
    }

    private List<CategoryVo> getBaseCategoryListV1() {
        //1.定义结果对象
        List<CategoryVo> categoryVoList = new ArrayList<>();
        //2.获取一二三级分类所有信息
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //3.1 手动封装 List<CategoryVo>
        //3.1.1 获取一级分类 15个键值对 对应表base_category1
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList
                .stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //3.1.2 遍历一级分类map 构建一级分类返回对象 包括一级id 一级name 一级categoryChild
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {
            CategoryVo category1Vo = new CategoryVo();
            //一级id
            Long category1Id = category1Entry.getKey();
            //一级name
            String category1Name = category1Entry.getValue().get(0).getCategory1Name();
            //一级categoryChild
            List<CategoryVo> category1Child = new ArrayList<>();
            //4.0 获取二级分类
            List<BaseCategoryView> category2List = category1Entry.getValue();
            //4.1遍历二级分类map 构建二级分类返回对象 包括二级id 二级name 二级categoryChild
            Map<Long, List<BaseCategoryView>> category2Map = category2List
                    .stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry : category2Map.entrySet()) {
                CategoryVo category2Vo = new CategoryVo();
                //二级id
                Long category2Id = category2Entry.getKey();
                //二级name
                String category2Name = category2Entry.getValue().get(0).getCategory2Name();
                //二级categoryChild
                List<CategoryVo> category2Child = new ArrayList<>();
                //5.0 获取三级分类
                List<BaseCategoryView> category3List = category2Entry.getValue();
                //5.1遍历三级分类map 构建三级分类返回对象 包括三级id 三级name 三级categoryChild
                Map<Long, List<BaseCategoryView>> category3Map = category3List
                        .stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                for (Map.Entry<Long, List<BaseCategoryView>> category3Entry : category3Map.entrySet()) {
                    CategoryVo category3Vo = new CategoryVo();
                    //三级id
                    Long category3Id = category3Entry.getKey();
                    //三级name
                    String category3Name = category3Entry.getValue().get(0).getCategory3Name();
                    //三级categoryChild
                    List<CategoryVo> category3Child = new ArrayList<>();
                    //组装三级
                    category3Vo.setCategoryId(category3Id);
                    category3Vo.setCategoryName(category3Name);
                    category3Vo.setCategoryChild(category3Child);
                    category2Child.add(category3Vo);
                }
                //组装二级
                category2Vo.setCategoryId(category2Id);
                category2Vo.setCategoryName(category2Name);
                category2Vo.setCategoryChild(category2Child);
                category1Child.add(category2Vo);
            }
            //组装一级
            category1Vo.setCategoryId(category1Id);
            category1Vo.setCategoryName(category1Name);
            category1Vo.setCategoryChild(category1Child);
            //添加一级到最终集合
            categoryVoList.add(category1Vo);
        }
        //4.返回 List<CategoryVo>
        return categoryVoList;
    }

    private List<CategoryVo> getBaseCategoryListV2() {
        List<CategoryVo> categoryList = baseCategory1Mapper.getCategoryList();
        return categoryList;
    }
}
