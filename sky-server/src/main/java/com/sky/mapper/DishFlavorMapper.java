package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入菜品口味数据
     * @param dishFlavors
     * @return
     * */
    void insertBatch(List<DishFlavor> dishFlavors);
}
