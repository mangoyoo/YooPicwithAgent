package com.mangoyoo.yoopicbackend.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mangoyoo.yoopicbackend.service.PictureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PictureFinderTool {

    @Resource
    @Lazy
    private PictureService pictureService;

    @Tool(description = "当且仅当用户明确说了要用本站的图片的时候才能调用这个工具，这个工具的作用是:Find pictures by category or tags, and return URLs as a string.")
    public String findPictures(
            @ToolParam(description = "Picture category") String category,
            @ToolParam(description = "Picture tags list, comma separated") String tags,
            @ToolParam(description = "Number of pictures to find") Integer count) {

        try {
            // 1. 参数验证
            if (count == null || count <= 0) {
                return "Error: Count must be greater than 0";
            }

            if (StrUtil.isBlank(category) && StrUtil.isBlank(tags)) {
                return "Error: At least one of category or tags must be provided";
            }

            log.info("开始查找图片，分类: {}, 标签: {}, 数量: {}", category, tags, count);

            // 2. 解析标签
            List<String> tagList = parseTagsString(tags);

            // 3. 分层查找图片
            List<String> foundUrls = findPicturesWithPriority(category, tagList, count);

            if (CollUtil.isEmpty(foundUrls)) {
                return "No pictures found matching the criteria";
            }

            // 4. 将URL组合成字符串返回
            String result = String.join(",", foundUrls);
            log.info("成功找到{}张图片", foundUrls.size());

            return result;

        } catch (Exception e) {
            log.error("查找图片失败", e);
            return "Error finding pictures: " + e.getMessage();
        }
    }

    /**
     * 分层查找图片：优先返回同时满足条件的，不足时返回或的结果
     */
    private List<String> findPicturesWithPriority(String category, List<String> tagList, Integer count) {
        List<String> foundUrls = new ArrayList<>();

        try {
            // 第一层：同时满足category和tags的图片
            if (StrUtil.isNotBlank(category) && CollUtil.isNotEmpty(tagList)) {
                log.info("第一层查找：同时满足分类和标签的图片");
                List<String> bothMatchUrls = pictureService.findRandomPictureUrlsByTags(tagList, category, count);
                foundUrls.addAll(bothMatchUrls);
                log.info("第一层找到{}张图片", bothMatchUrls.size());
            }

            // 如果数量已足够，直接返回
            if (foundUrls.size() >= count) {
                return foundUrls.subList(0, count);
            }

            // 第二层：只满足category的图片
            if (StrUtil.isNotBlank(category)) {
                int remainingCount = count - foundUrls.size();
                log.info("第二层查找：只满足分类的图片，还需要{}张", remainingCount);

                // 注意：这里需要修改PictureService来允许空标签，或者创建专门的按分类查找方法
                List<String> categoryOnlyUrls = pictureService.findRandomPictureUrlsByTags(
                        Collections.emptyList(), category, remainingCount * 2); // 多查一些避免重复

                // 过滤掉已存在的URL
                List<String> newCategoryUrls = categoryOnlyUrls.stream()
                        .filter(url -> !foundUrls.contains(url))
                        .limit(remainingCount)
                        .collect(Collectors.toList());

                foundUrls.addAll(newCategoryUrls);
                log.info("第二层找到{}张新图片", newCategoryUrls.size());
            }

            // 如果数量已足够，直接返回
            if (foundUrls.size() >= count) {
                return foundUrls.subList(0, count);
            }

            // 第三层：只满足tags的图片
            if (CollUtil.isNotEmpty(tagList)) {
                int remainingCount = count - foundUrls.size();
                log.info("第三层查找：只满足标签的图片，还需要{}张", remainingCount);

                List<String> tagsOnlyUrls = pictureService.findRandomPictureUrlsByTags(
                        tagList, null, remainingCount * 2); // 多查一些避免重复

                // 过滤掉已存在的URL
                List<String> newTagsUrls = tagsOnlyUrls.stream()
                        .filter(url -> !foundUrls.contains(url))
                        .limit(remainingCount)
                        .collect(Collectors.toList());

                foundUrls.addAll(newTagsUrls);
                log.info("第三层找到{}张新图片", newTagsUrls.size());
            }

            return foundUrls;

        } catch (Exception e) {
            log.error("分层查找图片失败", e);
            return foundUrls; // 返回已找到的部分
        }
    }

    /**
     * 解析标签字符串为列表
     */
    private List<String> parseTagsString(String tags) {
        if (StrUtil.isBlank(tags)) {
            return Collections.emptyList();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 批量查找图片（返回URL列表）
     */
    public List<String> findPicturesBatch(String category, List<String> tagList, Integer count) {
        try {
            log.info("批量查找图片，分类: {}, 标签: {}, 数量: {}", category, tagList, count);
            return findPicturesWithPriority(category, tagList, count);
        } catch (Exception e) {
            log.error("批量查找图片失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 验证查找参数
     */
    public String validateSearchParams(String category, String tags, Integer count) {
        if (count == null || count <= 0) {
            return "Error: Count must be a positive number";
        }

        if (count > 50) {
            return "Warning: Large count may impact performance (max recommended: 50)";
        }

        if (StrUtil.isBlank(category) && StrUtil.isBlank(tags)) {
            return "Error: At least one search criterion (category or tags) must be provided";
        }

        if (StrUtil.isNotBlank(tags)) {
            List<String> tagList = parseTagsString(tags);
            if (tagList.size() > 10) {
                return "Warning: Too many tags may limit search results";
            }
        }

        return "Parameters validation passed";
    }

    /**
     * 按分类查找图片
     */
    public String findPicturesByCategory(String category, Integer count) {
        return findPictures(category, null, count);
    }

    /**
     * 按标签查找图片
     */
    public String findPicturesByTags(String tags, Integer count) {
        return findPictures(null, tags, count);
    }
}
