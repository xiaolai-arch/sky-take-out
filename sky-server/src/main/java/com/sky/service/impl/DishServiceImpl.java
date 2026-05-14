package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;


    /**
     * 需要插入两张表，需要保持原子统一性
     * 要不全部成功，要不全部失败
     * 新增菜品和口味
     * */
    @Transactional // 保证方法是原子性的
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        // 属性命名一致才能进行拷贝
        BeanUtils.copyProperties(dishDTO,dish);

        // 向菜品表插入一条数据
        dishMapper.insert(dish);

        // 获取Insert语句生成的主键值
        Long id = dish.getId();

        // 向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()){
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(id);
            }
            // 插入n条数据
            dishFlavorMapper.insertBatch(flavors);

        }

    }
}
