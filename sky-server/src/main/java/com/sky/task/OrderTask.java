package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component // 放到spring容器中管理
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /*
    * 该方法在发开端使用，为了将待支付的状态改成已支付状态
    * */
    @Scheduled(cron = "*/5 * * * * *")
    public void processOrderPaySuccess(){
        log.info("待支付改成已经支付：{}", LocalDateTime.now());

        // 当前时间
        LocalDateTime time = LocalDateTime.now();

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, time);
        if (ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList){
                orders.setStatus(Orders.TO_BE_CONFIRMED);
                orderMapper.update(orders);
            }
        }
    }

    /*
    * 处理超时订单
    * */
    @Scheduled(cron = "0 */10 * * * *")
    public void processTimeOutOrder(){
        log.info("每分钟处理一下订单,{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        List<Orders> orderList = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, time);

        if (orderList != null && !orderList.isEmpty()){
            for(Orders order : orderList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /*
    * 处理一直处于派送中的订单
    * 每天凌晨一点触发
    * */
    @Scheduled(cron = "0 0 1 * * *")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList){
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
