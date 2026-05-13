package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.entity.Category;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api(tags = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 新增种类
     * @param category
     * @return
     * */

    @PostMapping
    @ApiOperation(value = "新增种类")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增种类：{}",categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }
}
