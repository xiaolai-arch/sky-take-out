package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "C端-购物车接口")
@Slf4j
public class ShoppingCatController {

    @Autowired
    private ShoppingCatService shoppingCatService;


    /**
     * 添加购物车
     * */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车:{}", shoppingCartDTO);
        shoppingCatService.add(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查询购物车数据
     * */
    @GetMapping("/list")
    @ApiOperation("查询购物车数据")
    public Result<List<ShoppingCart>> list() {
        log.info("查询购物车数据");
        List<ShoppingCart> list = shoppingCatService.showShoppingCart();
        return Result.success(list);
    }
}
