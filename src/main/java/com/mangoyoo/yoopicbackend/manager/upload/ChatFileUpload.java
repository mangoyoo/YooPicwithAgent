package com.mangoyoo.yoopicbackend.manager.upload;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.mangoyoo.yoopicbackend.config.CosClientConfig;
import com.mangoyoo.yoopicbackend.exception.BusinessException;
import com.mangoyoo.yoopicbackend.exception.ErrorCode;
import com.mangoyoo.yoopicbackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.Date;

@Slf4j
public abstract class ChatFileUpload {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final String uploadFile(Object inputSource, String uploadPathPrefix) {
        // 1. 校验文件
        validFile(inputSource);

        // 2. 生成文件上传路径
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);

            // 4. 处理文件来源（本地文件或URL等）
            processFile(inputSource, file);

            // 5. 上传文件到对象存储
            cosManager.putObject(uploadPath, file);

            // 6. 返回文件URL
            return cosClientConfig.getHost() + uploadPath;

        } catch (Exception e) {
            log.error("文件上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 7. 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（由子类实现具体校验逻辑）
     */
    protected abstract void validFile(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 删除临时文件
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("临时文件删除失败, filepath = {}", file.getAbsolutePath());
        }
    }
}
