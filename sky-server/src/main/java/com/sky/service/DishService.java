package com.sky.service;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DishService {

    /**
     * 新增菜品
     * */
    void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品的分页查询
     * */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品的批量删除
     * */
    void deleteBatch(List<Long> ids);

    /**
     * 菜品的起售停售
     * */
    void startOrStop(Integer status, Long id);

    /**
     * 根据ID查询菜品和对应的口味
     *
     * */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品和口味
     * */
    void updateWithFlavor(DishDTO dishDTO);
}
