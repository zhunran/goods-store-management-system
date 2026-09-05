package com.fengluan.brand.api;

import com.fengluan.brand.service.BrandService;
import com.fengluan.brand.util.FileUploadUtil;
import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.brand.BrandApi;
import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/brand/api")
public class BrandController implements BrandApi {

    private final BrandService brandService;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public PageVO<BrandVO> page(BrandQueryRequest query) {
        return brandService.page(query);
    }

    @Override
    public BrandVO getById(Long id) {
        return brandService.detail(id);
    }

    @Override
    public BrandVO create(BrandCreateRequest request) {
        return brandService.create(request);
    }

    @Override
    public BrandVO update(Long id, BrandUpdateRequest request) {
        return brandService.update(id, request);
    }

    @Override
    public Void delete(Long id) {
        brandService.delete(id);
        return null;
    }

    /**
     * Logo 上传：涉及 MultipartFile，属人机交互，不进契约
     */
    @PostMapping("/upload")
    public ApiResult<String> upload(@RequestParam("file") MultipartFile file) {
        return ApiResult.success(fileUploadUtil.upload(file));
    }
}