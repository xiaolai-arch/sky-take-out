package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface CategoryMapper {

    /**
     * 插入菜品数据
     * */
    @Insert("insert into category (type, name, sort, status, create_time, update_time, create_user, update_user)" +
            "values " +
            "(#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Category category);

    /**
     * 分页查询种类
     * 动态查询，使用xml文件映射
     * */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 种类的启动和禁用
     * */
    void update(Category category);

    /**
     * 种类的删除
     * */
    @Delete("delete from category where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据类型查询种类
     * */
    List<Category> list(Integer type);

    @Select("select * from category where id = #{id}")
    Category getById(Long id);
}
