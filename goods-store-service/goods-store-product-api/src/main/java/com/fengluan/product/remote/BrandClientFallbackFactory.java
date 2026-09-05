package com.fengluan.product.remote;

import com.fengluan.spi.brand.BrandApi;
import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 品牌 Feign 降级：品牌服务不可用时返回空兜底，避免商品服务级联失败
 */
@Component
public class BrandClientFallbackFactory implements FallbackFactory<BrandRemoteClient> {

    @Override
    public BrandRemoteClient create(Throwable cause) {
        return new BrandRemoteClient() {
            @Override
            public PageVO<BrandVO> page(BrandQueryRequest query) {
                return PageVO.empty();
            }

            @Override
            public BrandVO getById(Long id) {
                return null;
            }

            @Override
            public BrandVO create(BrandCreateRequest request) {
                return null;
            }

            @Override
            public BrandVO update(Long id, BrandUpdateRequest request) {
                return null;
            }

            @Override
            public Void delete(Long id) {
                return null;
            }
        };
    }
}