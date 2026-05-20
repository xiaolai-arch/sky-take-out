package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.enumeration.OperationType;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Orders orders);

    /*
    * 查询历史订单
    * */
    Page<Orders> pageQuery(OrdersPageQueryDTO dto);

    /*
    * 根据id查询订单详情
    * */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /*
    * 查询全部的订单
    * 订单分页查询
    * */
    Page<OrderVO> pageQuery1(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders")
    List<Orders> list();
}
