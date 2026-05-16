package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SetmealService {

    /**
     * 新增套餐接口
     * */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 套餐的启售停售
     * */
    void startOrStop(Integer status, Long id);

    /**
     * 批量删除套餐
     * */
    void deleteBatch(List<Long> ids);
}
