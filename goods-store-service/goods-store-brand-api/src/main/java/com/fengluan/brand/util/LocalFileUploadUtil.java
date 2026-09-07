package com.fengluan.brand.util;

import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
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

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp 图片", 400);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB", 400);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException("文件上传失败", 500);
        }
        if (!matchesMagic(bytes, ext)) {
            throw new BusinessException("图片内容与扩展名不符", 400);
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(basePath, "brand");
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, bytes);
            return PREFIX + "brand/" + fileName;
        } catch (IOException e) {
            log.error("本地文件上传失败", e);
            throw new BusinessException("文件上传失败", 500);
        }
    }

    /** 校验图片文件头魔数与扩展名是否匹配，防止伪造扩展名上传脚本/HTML */
    private boolean matchesMagic(byte[] b, String ext) {
        if (b.length < 4) {
            return false;
        }
        return switch (ext) {
            case ".jpg", ".jpeg" ->
                    (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case ".png" ->
                    (b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 'P' && (b[2] & 0xFF) == 'N' && (b[3] & 0xFF) == 'G';
            case ".gif" ->
                    (b[0] & 0xFF) == 'G' && (b[1] & 0xFF) == 'I' && (b[2] & 0xFF) == 'F' && (b[3] & 0xFF) == '8';
            case ".webp" ->
                    (b[0] & 0xFF) == 'R' && (b[1] & 0xFF) == 'I' && (b[2] & 0xFF) == 'F' && (b[3] & 0xFF) == 'F'
                            && b.length >= 12 && (b[8] & 0xFF) == 'W' && (b[9] & 0xFF) == 'E'
                            && (b[10] & 0xFF) == 'B' && (b[11] & 0xFF) == 'P';
            default -> false;
        };
    }
}