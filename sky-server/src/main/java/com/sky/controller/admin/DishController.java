package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    //
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品的分页查询
     *
     * */
    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除菜品
     * 可以一次性删除一个，也可以删除多个
     * 在启售的过程中不允许删除菜品
     * 被套餐关联了也不允许删除
     * 删除菜品后，关联的口味数据也需要删除
     * @param ids 字符串中间用逗号
     * */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids){ // @RequestParam解析该字符串
        log.info("菜品批量删除：{}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 设置启售和停售
     * */
    @PostMapping("/status/{status}")
    @ApiOperation("启售停售")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("菜品的启售和停售:,{},{}", status, id);
        dishService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据id查询
     * */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据ID查询菜品,{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     *
     * */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品:{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 更具种类查询全部菜品
     * @param categoryId
     * @return
     */

    @GetMapping("/list")
    @ApiOperation("查询全部菜品")
    public Result<List<Dish>> list(Long categoryId){
        log.info("查询全部菜品:{}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }
}
