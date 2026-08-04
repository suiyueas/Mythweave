package com.mythweave.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AvatarStorageService {

    @Value("${app.upload.avatar-path}")
    private String avatarPath;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * 保存头像
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 头像访问URL
     */
    public String saveAvatar(MultipartFile file, Long userId) {
        // 1. 验证文件
        validateFile(file);

        // 2. 生成文件名
        String extension = getFileExtension(file.getOriginalFilename());
        String filename = userId + "_" + System.currentTimeMillis() + "." + extension;

        // 3. 确保目录存在
        File dir = new File(avatarPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("创建头像目录: {}, 成功: {}", avatarPath, created);
        }

        // 4. 保存文件
        try {
            File dest = new File(dir, filename);
            file.transferTo(dest);
            log.info("头像保存成功: {}", dest.getAbsolutePath());
        } catch (IOException e) {
            log.error("头像保存失败", e);
            throw new RuntimeException("头像保存失败: " + e.getMessage());
        }

        // 5. 返回访问 URL
        return "/avatar/" + filename;
    }

    /**
     * 删除旧头像
     */
    public boolean deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/avatar/")) {
            return false;
        }

        String filename = avatarUrl.substring("/avatar/".length());
        File file = new File(avatarPath, filename);
        if (file.exists()) {
            boolean deleted = file.delete();
            log.info("删除旧头像: {}, 成功: {}", filename, deleted);
            return deleted;
        }
        return false;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 5MB");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("不支持的文件格式，请上传 JPG、PNG、WebP 或 GIF");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}