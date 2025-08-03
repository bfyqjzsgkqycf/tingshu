package com.lsj.tingshu.album.api;

import com.lsj.tingshu.album.service.BaseCategoryService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.BaseAttribute;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.vo.category.CategoryVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping(value = "/api/album/category")
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseCategoryApiController {

    @Autowired
    private BaseCategoryService baseCategoryService;

    // http://localhost:8500/api/album/category/getBaseCategoryList
    @GetMapping("/getBaseCategoryList")
    public Result getBaseCategoryList() {
        List<CategoryVo> categoryList = baseCategoryService.getBaseCategoryList();
        return Result.ok(categoryList);
    }

    // Request URL: http://localhost:8500/api/album/category/findAttribute/2
    @GetMapping("/findAttribute/{c1Id}")
    public Result findAttribute(@PathVariable Long c1Id) {
        List<BaseAttribute> albumAttributeValueList = baseCategoryService.findAttributeList(c1Id);
        return Result.ok(albumAttributeValueList);
    }

    // Request URL: http://localhost:8500/api/album/category/findTopBaseCategory3/1
    @GetMapping("/findTopBaseCategory3/{c1Id}")
    public Result findTopBaseCategory3(@PathVariable Long c1Id) {
        List<BaseCategory3> topBaseCategory3List = baseCategoryService.findTopBaseCategory3(c1Id);
        return Result.ok(topBaseCategory3List);
    }

    // Request URL: http://localhost:8500/api/album/category/getBaseCategoryList/2
    @GetMapping("/getBaseCategoryList/{c1Id}")
    public Result getBaseCategoryListByC1Id(@PathVariable Long c1Id) {
        CategoryVo categoryVo = baseCategoryService.getBaseCategoryListByC1Id(c1Id);
        return Result.ok(categoryVo);
    }
}

