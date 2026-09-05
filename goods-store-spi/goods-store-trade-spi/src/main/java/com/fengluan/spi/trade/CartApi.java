package com.fengluan.spi.trade;

import com.fengluan.spi.trade.dto.CartAddRequest;
import com.fengluan.spi.trade.vo.CartVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 交易服务-购物车 纯 HTTP 契约（Day 9 起规范：spi 禁 @FeignClient，controller implements、消费方 extends 复用）
 * 相对路径基于实现方 Controller 类级前缀 /trade/api（CartController），完整路径见实现类。
 * 当前会员由实现方从 X-User-Id 解析，契约不声明 header 参数。
 */
public interface CartApi {

    /** 当前会员购物车列表 */
    @GetMapping("/cart")
    List<CartVO> listCart();

    /** 添加购物车（同一商品重复添加数量累加） */
    @PostMapping("/cart")
    Void addCart(@Valid @RequestBody CartAddRequest request);

    /** 修改数量 */
    @PutMapping("/cart/{cartId}")
    Void updateCartQty(@PathVariable Long cartId, @RequestParam Integer qty);

    /** 删除购物车项 */
    @DeleteMapping("/cart/{cartId}")
    Void removeCart(@PathVariable Long cartId);
}