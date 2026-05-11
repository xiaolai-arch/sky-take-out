package com.sky.mapper;

import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    // sql比较简单的时候直接使用注解
    // 如果sql比较复杂，使用xml文件，映射
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

}
