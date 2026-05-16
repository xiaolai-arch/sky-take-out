package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     * 需要保存原子一致性
     * 需要对两张表进行操作
     * setmeal setmeal_dish
     * */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO){

        Setmeal setmeal = new Setmeal();

        // 将setmealDTO中的属性拷贝到setmeal中
        BeanUtils.copyProperties(setmealDTO,setmeal);

        // 保存套餐数据
        setmealMapper.insert(setmeal);

        // 获取Insert语句生成的主键值
        Long id = setmeal.getId();

        /*
         * 为套餐关联的菜品设置套餐ID
         * 将新生成的套餐主键赋值给每个菜品对象，建立套餐与菜品的关联关系
         */
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }

        // 向套餐表中插入n条菜品
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /*
    * 套餐分页查询
    * */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        // 创建分页对象
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 套餐的启售停售
     * */
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /*
    * 批量删除套餐
    * */
    public void deleteBatch(List<Long> ids){

        // 判断当前套餐是否在售
        for (Long id : ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE){
                // 当前套餐处于启售中，不能删除
                throw new DeletionNotAllowedException("当前套餐处于启售中，不能删除");
            }
        }
        // 先删除关联菜品
        for (Long setmealId : ids){
            // 删除套餐表中的数据
            setmealMapper.deleteById(setmealId);

            // 删除套餐菜品关联表中数据
            setmealDishMapper.deleteBySetmealId(setmealId);
        }
    }
}
