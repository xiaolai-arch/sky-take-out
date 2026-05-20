package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
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
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.math.BigDecimal;
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

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


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
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail()); // 拼装完整地址

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
    public void cancel(Long id) {
        // 本质上是一个update操作
        // 需要将订单状态修改成6，取消订单
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /*
    * 再来一单
    * 就是将原来订单内到数据，全部再存放到购物车中
    * */
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

        // 获取分页结果
        Page<OrderVO> page = orderMapper.pageQuery1(ordersPageQueryDTO);
        // 再根据订单Id查询订单详情
        for (OrderVO orderVO : page) {
            Long orderId = orderVO.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
            orderVO.setOrderDetailList(orderDetailList);
        }
        return new PageResult(page.getTotal(), page.getResult());
    }
}
