package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import org.springframework.stereotype.Service;

@Service
public interface ShoppingCatService {

    /**
     * 添加购物车
     * */
    void add(ShoppingCartDTO shoppingCartDTO);
}
