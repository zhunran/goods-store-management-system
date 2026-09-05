package com.fengluan.product.remote;

import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 品牌远程装配服务：批量拉取品牌名，失败降级返回空 Map
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandRemoteService {

    private final BrandRemoteClient brandRemoteClient;

    /**
     * 拉取全量品牌，映射 brandId -> brandName（数量级小，可接受）
     */
    public Map<Integer, String> getBrandNameMap(Collection<Integer> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            BrandQueryRequest q = new BrandQueryRequest();
            q.setPageNum(1L);
            q.setPageSize(1000L);
            PageVO<BrandVO> page = brandRemoteClient.page(q);
            if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
                return Collections.emptyMap();
            }
            return page.getRecords().stream()
                    .filter(b -> b.getId() != null && brandIds.contains(b.getId().intValue()))
                    .collect(Collectors.toMap(
                            b -> b.getId().intValue(),
                            BrandVO::getName,
                            (a, b) -> a));
        } catch (Exception e) {
            log.error("查询品牌名称失败，降级返回空", e);
            return Collections.emptyMap();
        }
    }
}