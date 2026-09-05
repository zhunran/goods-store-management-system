package com.fengluan.brand.util;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传策略接口：按配置切换本地存储 / OSS
 */
public interface FileUploadUtil {

    /**
     * 上传文件，返回可访问的相对 URL
     */
    String upload(MultipartFile file);
}