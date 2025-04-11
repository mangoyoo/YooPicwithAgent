package com.mangoyoo.yoopicbackend.manager.upload;




import cn.hutool.core.io.FileUtil;
import com.mangoyoo.yoopicbackend.exception.ErrorCode;
import com.mangoyoo.yoopicbackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
@Slf4j
@Component
public class OtherFileUpload extends ChatFileUpload {

    @Override
    protected void validFile(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");

        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
//        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp", "gif");
//        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");

        // 3. 校验文件内容不为空
        ThrowUtils.throwIf(fileSize == 0, ErrorCode.PARAMS_ERROR, "文件内容不能为空");

        log.info("文件校验通过: 文件名={}, 大小={}, 类型={}",
                multipartFile.getOriginalFilename(), fileSize, fileSuffix);
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        String originalFilename = multipartFile.getOriginalFilename();
        log.info("获取原始文件名: {}", originalFilename);
        return originalFilename;
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;

        log.info("=== 开始处理文件写入 ===");
        log.info("源文件大小: {} bytes", multipartFile.getSize());
        log.info("目标文件路径: {}", file.getAbsolutePath());

        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                log.info("创建父目录: {}, 结果: {}", parentDir.getAbsolutePath(), created);
            }

            // 方法1：使用 MultipartFile.transferTo() - 推荐
            multipartFile.transferTo(file);

            // 验证文件是否写入成功
            long writtenSize = file.length();
            log.info("文件写入完成: 写入大小={} bytes, 原始大小={} bytes",
                    writtenSize, multipartFile.getSize());

            if (writtenSize == 0) {
                throw new IOException("文件写入失败：文件大小为0");
            }

            if (writtenSize != multipartFile.getSize()) {
                log.warn("警告：写入大小与原始大小不匹配！");
            }

        } catch (IOException e) {
            log.error("文件写入失败", e);
            // 清理可能创建的空文件
            if (file.exists()) {
                boolean deleted = file.delete();
                log.info("清理空文件: {}", deleted);
            }
            throw new Exception("文件写入失败: " + e.getMessage(), e);
        }

        log.info("=== 文件处理完成 ===");
    }
}
