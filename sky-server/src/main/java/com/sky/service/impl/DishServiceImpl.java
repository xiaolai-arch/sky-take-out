package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
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
    @Autowired
    private SetmealDishMapper setmealDishMapper;


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

    /*
    * 菜品分页查询
    *
    * */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());

        // 在视图上展示的是VO所以需要的范性为DishVO属性
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        // page.getResult() 获取当前页的数据 一个list集合
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional // 保证方法执行时，事务是原子的
    public void deleteBatch(List<Long> ids){
        // 判断当前菜品是否在启售中
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                // 在售中
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断当前菜品是否在被其他套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && !setmealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 判断当前菜品是否能删除
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            // 删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        // 更具菜品id集合批量删除
        dishMapper.deleteByIds(ids);

        // 批量删除菜品关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 起售停售
     * */
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }

    /*
    * 根据Id查询菜品和对应的口味数据
    * */
    public DishVO getByIdWithFlavor(Long id) {
        // 根据菜品表里面查询菜品
        Dish dish = dishMapper.getById(id);

        // 再根据菜品表里面的表dishId查询我们的口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 将查询到的数据封装到DishVO中
        DishVO dishVO = new DishVO();

        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }


    /*
    * 修改菜品和口味数据
    * */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 修改菜品表基本信息
        dishMapper.update(dish);

        // 删除原有的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()){
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
            }
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 更具categoryId查询全部菜品数据
     * */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }
}
