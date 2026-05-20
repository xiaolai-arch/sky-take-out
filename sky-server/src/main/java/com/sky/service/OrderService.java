package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {

    /*
    * 用户下单
    * */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);


    /*
    * 查询历史订单
    *
    * */
    PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /*
    * 根据订单id查询订单详情
    * */
    OrderVO orderDetail(Long id);

    /*
    * 取消订单
    * */
    void cancel(Long id);

    void repetition(Long id);

    /*
    * 订单分页查询
    * */
    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);
}
