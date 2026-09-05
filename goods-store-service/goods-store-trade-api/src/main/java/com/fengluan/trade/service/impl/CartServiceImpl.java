package com.fengluan.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.spi.product.vo.GoodVO;
import com.fengluan.spi.trade.dto.CartAddRequest;
import com.fengluan.spi.trade.vo.CartVO;
import com.fengluan.trade.entity.CartEntity;
import com.fengluan.trade.remote.TradeProductClient;
import com.fengluan.trade.repository.CartMapper;
import com.fengluan.trade.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, CartEntity> implements CartService {

    private final CartMapper cartMapper;
    private final TradeProductClient productClient;

    @Override
    public List<CartVO> listCart(Long memberId) {
        return cartMapper.selectList(new LambdaQueryWrapper<CartEntity>()
                        .eq(CartEntity::getMemberId, memberId.intValue()))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCart(Long memberId, CartAddRequest request) {
        checkGood(request.getGoodId(), request.getQty());
        cartMapper.insertOrUpdate(memberId.intValue(), request.getGoodId().intValue(), request.getQty());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCartQty(Long memberId, Long cartId, Integer qty) {
        CartEntity cart = getOwned(memberId, cartId);
        checkGood(cart.getGoodId().longValue(), qty);
        cart.setQty(qty);
        cartMapper.updateById(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCart(Long memberId, Long cartId) {
        getOwned(memberId, cartId);
        cartMapper.deleteById(cartId);
    }

    /**
     * 校验商品存在、未下架、库存充足；返回商品信息
     */
    private GoodVO checkGood(Long goodId, int need) {
        GoodVO good = productClient.getById(goodId);
        if (good == null || Boolean.TRUE.equals(good.getIsDel())) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }
        if (good.getQty() == null || good.getQty() < need) {
            throw new BusinessException(ErrorCode.GOOD_STOCK_INSUFFICIENT);
        }
        return good;
    }

    /**
     * 校验购物车项归属当前会员且存在
     */
    private CartEntity getOwned(Long memberId, Long cartId) {
        CartEntity cart = cartMapper.selectOne(new LambdaQueryWrapper<CartEntity>()
                .eq(CartEntity::getId, cartId)
                .eq(CartEntity::getMemberId, memberId.intValue()));
        if (cart == null) {
            throw new BusinessException("购物车项不存在", ErrorCode.BAD_REQUEST.getCode());
        }
        return cart;
    }

    private CartVO toVO(CartEntity entity) {
        CartVO vo = new CartVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}