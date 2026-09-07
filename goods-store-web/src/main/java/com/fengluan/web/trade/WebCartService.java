package com.fengluan.web.trade;

import com.fengluan.spi.product.vo.GoodVO;
import com.fengluan.spi.trade.dto.CartAddRequest;
import com.fengluan.spi.trade.dto.CartBatchRequest;
import com.fengluan.spi.trade.dto.CartSelectedRequest;
import com.fengluan.spi.trade.vo.CartVO;
import com.fengluan.web.product.ProductFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车聚合服务（BFF）：拉取 trade 购物车基础项 + 商品图/名/价补齐
 */
@Service
@RequiredArgsConstructor
public class WebCartService {

    private final TradeCartFeignClient tradeCartFeignClient;
    private final ProductFeignClient productFeignClient;

    public List<CartItemVO> list(Long memberId) {
        return tradeCartFeignClient.listCart().stream()
                .map(this::aggregate)
                .collect(Collectors.toList());
    }

    public void add(Long memberId, CartAddRequest request) {
        tradeCartFeignClient.addCart(request);
    }

    public void updateQty(Long memberId, Long cartId, Integer qty) {
        tradeCartFeignClient.updateCartQty(cartId, qty);
    }

    public void remove(Long memberId, Long cartId) {
        tradeCartFeignClient.removeCart(cartId);
    }

    public void updateSelected(Long memberId, CartSelectedRequest request) {
        tradeCartFeignClient.updateSelected(request);
    }

    public void removeBatch(Long memberId, CartBatchRequest request) {
        tradeCartFeignClient.removeBatch(request);
    }

    /** 聚合规则 1：购物车项 + 商品主图/名称/售价 */
    private CartItemVO aggregate(CartVO cart) {
        CartItemVO vo = new CartItemVO();
        vo.setCartId(cart.getId());
        vo.setGoodId(cart.getGoodId());
        vo.setQty(cart.getQty());
        vo.setSelected(cart.getSelected());
        GoodVO good = productFeignClient.getById(cart.getGoodId());
        if (good != null) {
            vo.setGoodName(good.getName());
            vo.setGoodPic(good.getPic());
            vo.setPrice(good.getPrice());
        }
        return vo;
    }
}