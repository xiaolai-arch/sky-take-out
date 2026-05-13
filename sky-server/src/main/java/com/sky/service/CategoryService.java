package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.service.impl.CategoryServiceImpl;

import java.util.List;

public interface CategoryService {

    /**
     * 新增种类
     * */
    void save(CategoryDTO categoryDTO);

    /**
     * 种类分页查询
     * */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 种类的启动和禁用
     * */
    void startOrStop(Integer status, Long id);

    /**
     * 根据ID删除种类
     * */
    void deleteById(Long id);

    /*
    * 修改数据
    * */
    void update(CategoryDTO categoryDTO);

    /*
    * 根据ID查询数据
    * */
    Category getById(Long id);

    /**
     * 根据种类查询
     * */
    List<Category> list(Integer type);
}
