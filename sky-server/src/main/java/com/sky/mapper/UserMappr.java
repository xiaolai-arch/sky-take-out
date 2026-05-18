package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMappr {

    /*
    * 根据openid查询用户
    * */
    @Select("select * from user where openid=#{openid}")
    User getByOpenid(String openid);

    /*
    * 插入用户数据
    * @param user需要返回主键值，后续还需要使用
    * */
    void insert(User user);
}
