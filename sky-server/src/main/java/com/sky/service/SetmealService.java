package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;
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

    /**
     * 修改套餐数据
     * @param setmealDTO
     * */
    void update(SetmealDTO setmealDTO);

    /**
     * 根据Id查询套餐
     * @param id
     * @return
     * */
    SetmealVO getByIdWithDish(Long id);

}
