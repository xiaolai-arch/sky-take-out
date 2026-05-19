package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCatMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCatMapper shoppingCatMapper;


    /*
    * 用户下单
    * @param ordersSubmitDTO
    * @return
    * */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理各种业务（地址薄为空不能下单，购物车为空不能下单）

        // 处理各种异常情况
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null){
            // 抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }


        // 检查当前用户购物车数据是否为空
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCatMapper.list(shoppingCart);

        if (shoppingCartList == null || isEmpty(shoppingCartList)) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2. 需要向订单表中插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now()); // 创建订单时间
        orders.setPayStatus(Orders.UN_PAID); // 支付状态设置

        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 时间戳作为订单号

        orders.setPhone(addressBook.getPhone()); //通过地址薄获取手机号
        orders.setConsignee(addressBook.getConsignee()); // 通过地址薄获取收货人

        orders.setUserId(userId);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetails = new ArrayList<>();
        // 3. 向订单明细表插入n条数据
        for (ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail(); // 订单明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 设置当前订单明细关联的订单id
            orderDetails.add(orderDetail);
        }
        // 批量插入数据
        orderDetailMapper.insertBatch(orderDetails);

        // 4.下单成功购物车数据需要清空
        shoppingCatMapper.delete(userId);

        // 5.封装VO返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();


        return orderSubmitVO;
    }
}
