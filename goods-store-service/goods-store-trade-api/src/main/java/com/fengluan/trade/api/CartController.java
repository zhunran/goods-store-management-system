package com.fengluan.trade.api;

import com.fengluan.spi.trade.CartApi;
import com.fengluan.spi.trade.dto.CartAddRequest;
import com.fengluan.spi.trade.vo.CartVO;
import com.fengluan.trade.service.CartService;
import com.fengluan.trade.util.CurrentUserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trade/api")
public class CartController implements CartApi {

    private final CartService cartService;

    @Override
    public List<CartVO> listCart() {
        return cartService.listCart(CurrentUserUtil.currentUserId());
    }

    @Override
    public Void addCart(@Valid @RequestBody CartAddRequest request) {
        cartService.addCart(CurrentUserUtil.currentUserId(), request);
        return null;
    }

    @Override
    public Void updateCartQty(@PathVariable Long cartId, @RequestParam Integer qty) {
        cartService.updateCartQty(CurrentUserUtil.currentUserId(), cartId, qty);
        return null;
    }

    @Override
    public Void removeCart(@PathVariable Long cartId) {
        cartService.removeCart(CurrentUserUtil.currentUserId(), cartId);
        return null;
    }
}