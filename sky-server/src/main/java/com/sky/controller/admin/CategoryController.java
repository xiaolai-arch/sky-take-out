package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * @param categoryDTO
     * @return
     * */
    @PostMapping
    @ApiOperation(value = "新增种类")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增种类：{}",categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @param
     * */
    @GetMapping("/page")
    @ApiOperation(value = "分页查询")
    public Result page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 种类的启动和禁用
     * */
    @PostMapping("/status/{status}")
    @ApiOperation(value = "种类的启动和禁用")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("种类的启动和禁用：{}",id);
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据ID删除种类
     * */
    @DeleteMapping
    @ApiOperation(value = "根据ID删除种类")
    public Result delete(Long id){
        log.info("根据ID删除种类：{}",id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }

    /**
     * 修改种类
     * */
    @PutMapping
    @ApiOperation(value = "修改种类")
    public Result update(@RequestBody CategoryDTO categoryDTO){
        log.info("修改种类：{}",categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();
    }
    // 根据ID查询种类，数据回显
    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询种类")
    public Result getById(@PathVariable Long id){
        log.info("根据ID查询种类：{}",id);
        Category category = categoryService.getById(id);
        return Result.success(category);
    }

}
