package com.fengluan.brand.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储实现：保存到 {upload.local.path}/brand/，返回相对 URL
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "upload.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileUploadUtil implements FileUploadUtil {

    @Value("${upload.local.path:./static/upload}")
    private String basePath;

    private static final String PREFIX = "/static/upload/";

    @Override
    public String upload(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(basePath, "brand");
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target.toAbsolutePath().toFile());
            return PREFIX + "brand/" + fileName;
        } catch (IOException e) {
            log.error("本地文件上传失败", e);
            throw new com.fengluan.common.exception.BusinessException("文件上传失败", 500);
        }
    }
}