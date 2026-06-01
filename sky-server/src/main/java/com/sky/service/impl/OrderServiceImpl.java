package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.controller.notify.SearchHttpAK;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.WeChatProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.beans.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCatMapper shoppingCatMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Value("${sky.shop.address}")
    private String shopAddress;
    @Autowired
    private WebSocketServer webSocketServer;


    /*
    * 用户下单
    * @param ordersSubmitDTO
    * @return
    * */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) throws Exception {

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
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail()); // 拼装完整地址

        // 获取地址，判断当前地址是否支持配送
        String address = addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail();
        Long distance = MaxDistance(address);
        if (distance > 5000){
            log.info("当前地址不支持配送");
            // 该项目不支持支付，直接跳过地址异常错误
            // throw new OrderBusinessException("当前地址不支持配送");
        }

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

        /*
        * 该处开发订单提醒
        * */
        log.info("订单提交成功，开始发送订单提醒");
        Map map = new HashMap<>();
        map.put("type", 1); // 1表示订单提醒
        map.put("orderId", orders.getId());
        map.put("content", "订单号" + orders.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

        return orderSubmitVO;
    }

    /*
    * 调用百度地图Api来判断当前地址的最大路程
    * */
    private Long MaxDistance(String address) throws Exception {
        return new SearchHttpAK().getDistance(address, shopAddress);
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    // ... existing code ...

    /**
     * 查询用户历史订单（分页）
     * <p>
     * 该方法会查询当前登录用户的订单列表，支持按状态筛选。
     * 对于每个订单，还会关联查询其订单详情（菜品信息）。
     * </p>
     *
     * @param ordersPageQueryDTO 分页查询参数，包含：
     *                           - page: 页码
     *                           - pageSize: 每页记录数
     *                           - status: 订单状态（可选，用于筛选）
     * @return PageResult 分页结果，包含：
     *                    - total: 总记录数
     *                    - records: OrderVO列表，每个OrderVO包含订单基本信息和订单详情列表
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {

        // 使用PageHelper插件进行分页，自动拦截SQL并添加LIMIT子句
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize(), true);

        // 构建查询条件：设置当前用户ID和订单状态
//        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
//        dto.setUserId(BaseContext.getCurrentId());
//        dto.setStatus(ordersPageQueryDTO.getStatus());
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        // 执行分页查询，获取订单列表
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 将Orders实体转换为OrderVO，并关联查询每个订单的详情
        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0){
            for (Orders order : page){
                Long orderId = order.getId();
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }


    /*
    * 根据订单id查询订单详情
    * */
    public OrderVO orderDetail(Long id) {

        // 先查询Orders中对印的order
        Orders orders = orderMapper.getById(id);

        // 在根据order中order_id查询OrderDetail
        Long orderId = orders.getId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /*
    * 根据订单Id取消订单
    * @param id
    * */
    public void userCancelById(Long id) throws Exception {
        // 本质上是一个update操作
        // 根据id查询订单
        Orders order = orderMapper.getById(id);

        // 校验订单是否存在
        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (order.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order1 = new Orders();
        order1.setId(order.getId());

        // 订单处于待接单状态，需要进行退款
        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            // 调用微信退款接口
            weChatPayUtil.refund(
                    order.getNumber(), // 商户订单号
                    order.getNumber(), // 商户退款单号
                    new BigDecimal(0.01),// 退款金额单位元
                    new BigDecimal(0.01) // 原订单金额
            );

            // 修改订单状态为 退款
            order1.setStatus(Orders.REFUND);
            order1.setCancelReason("用户取消");
            order1.setCancelTime(LocalDateTime.now());
            orderMapper.update(order1);
        }
    }

    /*
    * 再来一单
    * 就是将原来订单内到数据，全部再存放到购物车中
    * */
    // TODO 可以进行优化进行批量插入购物车中
    public void repetition(Long id) {
        // 再来一单，先获取原来订单全部内容
        Orders order = orderMapper.getById(id);

        // 将获取到的内容，再添加到购物车中
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        ShoppingCart shoppingCart = new ShoppingCart();

        for (OrderDetail orderDetail : orderDetailList) {
            // 获取菜品Id或套餐Id
            Long dishId = orderDetail.getDishId();
            Long setmealId = orderDetail.getSetmealId();

            shoppingCart.setUserId(BaseContext.getCurrentId());
            // 再根据菜品Id或套餐Id查询对应的商品，再添加到购物车中
            if (dishId != null){
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else if (setmealId != null) {
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCatMapper.insert(shoppingCart);
        }
    }

    /*
    * 订单分页查询
    * 查询全部都订单
    * */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 分页插件：使用PageHelper
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转换为OrderVO
        List<OrderVO> list = getOrderVOList(page);

        return new PageResult(page.getTotal(), list);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)){
            for (Orders orders : ordersList){
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList中
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }
    /*
    * 根据订单ID获取菜品信息字符串
    * @param orders
    * @return
    * */
    private String getOrderDishStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String oorderDish = x.getName() + "*" + x.getNumber() + ";";
            return oorderDish;
        }).collect(Collectors.toList());

        // 将订单对应的所有菜品，拼接在一起
        return String.join("", orderDishList);
    }



    /*
    *
    * */
    public OrderStatisticsVO statistics() {
        // 查询全部订单
        List<Orders> ordersList = orderMapper.list();
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(0);
        orderStatisticsVO.setConfirmed(0);
        orderStatisticsVO.setDeliveryInProgress(0);
        for (Orders orders : ordersList) {
            Integer status = orders.getStatus();
            switch (status) {
                case 1:
                    // 待接单
                    orderStatisticsVO.setToBeConfirmed(orderStatisticsVO.getToBeConfirmed() + 1);
                    break;
                case 2:
                    // 待派送
                    orderStatisticsVO.setConfirmed(orderStatisticsVO.getConfirmed() + 1);
                    break;
                case 3:
                    // 派送中
                    orderStatisticsVO.setDeliveryInProgress(orderStatisticsVO.getDeliveryInProgress() + 1);
                    break;
            }
        }
        return orderStatisticsVO;
    }

    /*
    * 接单
    * */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        // 数据库更新
        orderMapper.update(orders);
    }

    /*
    * 拒单
    * */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2 才可以拒单
        if (orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 支付状态
        Integer payStatus = orders.getPayStatus();
        if (payStatus.equals(Orders.PAID)){
            // 用户已经支付，需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(1),
                    new BigDecimal(1));
            log.info("申请退款：{}", refund);
        }

        // 据单需要退款，根据订单id更新订单状态
        Orders order1 = new Orders();
        order1.setId(orders.getId());
        order1.setStatus(Orders.CANCELLED);
        order1.setCancelReason(ordersRejectionDTO.getRejectionReason());
        order1.setCancelTime(LocalDateTime.now());

        orderMapper.update(order1);
    }

    /*
    * 取消订单
    * */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        // 支付状态
        Integer payStatus = orders.getPayStatus();
        if (payStatus.equals(Orders.PAID)){
            // 用户已经支付，需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(1),
                    new BigDecimal(1));
            log.info("申请退款：{}", refund);
        }

        // 管理端取消订单需要退款
        Orders order1 = new Orders();
        order1.setId(ordersCancelDTO.getId());
        order1.setStatus(Orders.CANCELLED);
        order1.setCancelReason(ordersCancelDTO.getCancelReason());
        order1.setCancelTime(LocalDateTime.now());
        orderMapper.update(order1);
    }

    /*
    * 派送订单
    *
    * */
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);

        // 校验订单是否存在
        if(orders == null || orders.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders1 = new Orders();
        orders1.setId(id);
        // 更新订单状态
        orders1.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders1);
    }

    /*
    * 完成订单
    * */
    public void complete(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 校验订单是否存在，只有派送中的订单才能完成
        if(orders == null || orders.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Orders orders1 = new Orders();
        orders1.setId(orders.getId());

        orders1.setStatus(Orders.COMPLETED);
        orders1.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders1);
    }

    public void reminder(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 校验订单是否存在
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap();
        map.put("type", 2); // 2表示的是客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());

        // 通过浏览器websocket
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
