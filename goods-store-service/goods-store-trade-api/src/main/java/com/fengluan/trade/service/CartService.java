package com.fengluan.trade.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.fengluan.spi.trade.dto.CartAddRequest;
import com.fengluan.spi.trade.vo.CartVO;
import com.fengluan.trade.entity.CartEntity;

import java.util.List;

public interface CartService extends IService<CartEntity> {

    List<CartVO> listCart(Long memberId);

    void addCart(Long memberId, CartAddRequest request);

    void updateCartQty(Long memberId, Long cartId, Integer qty);

    void removeCart(Long memberId, Long cartId);
}