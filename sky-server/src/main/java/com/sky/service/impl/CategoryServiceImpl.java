package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增种类
     * */
    public void save(CategoryDTO categoryDTO){
        Category category = new Category();

        // 属性copy
        BeanUtils.copyProperties(categoryDTO, category);

        // 设置状态
        category.setStatus(StatusConstant.ENABLE);

        // 设置创建时间，和更新时间
        //category.setCreateTime(LocalDateTime.now());
        //category.setUpdateTime(LocalDateTime.now());

        // 设置创建人ID
        //category.setCreateUser(BaseContext.getCurrentId());
        //category.setUpdateUser(BaseContext.getCurrentId());

        // 插入数据库
        categoryMapper.insert(category);
    }

    /**
     * 种类分页查询
     *
     * */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO){
        // 底层基于limit关键字封装
        PageHelper.startPage(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());

        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);

        long total = page.getTotal();
        List<Category> records = page.getResult();
        PageResult pageResult = new PageResult(total, records);
        return pageResult;
    }

    /**
     * 种类的启动和禁用
     * */
    public void startOrStop(Integer status, Long id){
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.update(category);
    }

    /**
     * 根据ID删除种类
     * */
    public void deleteById(Long id){
        // 查询当前分类是否关联了菜品
        Integer count = dishMapper.countByCategoryId(id);
        if (count > 0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        // 查询当前分类是否关联了套餐
        count = setmealMapper.countByCategoryId(id);
        if (count > 0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 根据类型查询分页种类
     * */
    public List<Category> list(Integer type){
        return categoryMapper.list(type);
    }

    /**
     *修改数据
     * */
    public void update(CategoryDTO categoryDTO){
        Category category = Category.builder()
                .id(categoryDTO.getId())
                .type(categoryDTO.getType())
                //.updateTime(LocalDateTime.now())
                //.updateUser(BaseContext.getCurrentId())
                .name(categoryDTO.getName())
                .sort(categoryDTO.getSort())
                .build();
        categoryMapper.update(category);
    }

    /**
     * 根据ID查询种类
     * */
    public Category getById(Long id){
        return categoryMapper.getById(id);
    }
}
