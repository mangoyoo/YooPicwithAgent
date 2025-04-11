package com.mangoyoo.yoopicbackend.tools;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mangoyoo.yoopicbackend.exception.ErrorCode;
import com.mangoyoo.yoopicbackend.exception.ThrowUtils;
import com.mangoyoo.yoopicbackend.mapper.PictureMapper;
import com.mangoyoo.yoopicbackend.model.entity.Picture;
import com.mangoyoo.yoopicbackend.model.enums.PictureReviewStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片查找服务类
 * 提供基于标签和分类的图片查找功能（仅限公共图库）
 */
@Service
@Slf4j
public class PictureFinderService {

    @Resource
    private PictureMapper pictureMapper;

    /**
     * 根据图片标签和分类查找公共图库中的图片并返回指定数量的图片URL
     * 优先返回同时满足tag和category的结果，如果不足则补充满足其中一个条件的结果
     *
     * @param tag 图片标签
     * @param category 图片分类（可为空）
     * @param count 要返回的图片数量
     * @return 格式化的图片URL字符串，多个URL用逗号分隔
     */
    public String findPictureUrlsByTagAndCategory(String tag, String category, Integer count) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(tag), ErrorCode.PARAMS_ERROR, "标签不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "查找数量必须大于0");
        ThrowUtils.throwIf(count > 100, ErrorCode.PARAMS_ERROR, "单次查找数量不能超过100");

        try {
            List<String> allUrls = new ArrayList<>();

            // 第一步：查找同时满足tag和category的图片（如果category不为空）
            if (StrUtil.isNotBlank(category)) {
                List<String> exactMatchUrls = findPictureUrlsByExactMatch(tag, category);
                allUrls.addAll(exactMatchUrls);
                log.info("同时满足标签 [{}] 和分类 [{}] 的图片数量: {}", tag, category, exactMatchUrls.size());
            }

            // 第二步：如果结果不足，查找满足tag OR category的图片
            if (allUrls.size() < count) {
                List<String> orMatchUrls = findPictureUrlsByTagOrCategory(tag, category);
                // 去重并添加
                for (String url : orMatchUrls) {
                    if (!allUrls.contains(url)) {
                        allUrls.add(url);
                    }
                }
                log.info("满足标签 [{}] 或分类 [{}] 的图片总数量: {}", tag, category, allUrls.size());
            }

            // 如果仍然没有找到图片
            if (CollUtil.isEmpty(allUrls)) {
                log.info("根据标签 [{}] 和分类 [{}] 在公共图库中没有找到任何图片", tag, category);
                return "";
            }

            // 随机打乱列表
            Collections.shuffle(allUrls);

            // 选择指定数量的URL
            List<String> selectedUrls;
            if (allUrls.size() <= count) {
                selectedUrls = allUrls;
                log.info("找到 {} 张图片，少于请求数量 {}，返回全部", allUrls.size(), count);
            } else {
                selectedUrls = allUrls.subList(0, count);
                log.info("找到 {} 张图片，随机选择 {} 张返回", allUrls.size(), count);
            }

            // 将URL列表格式化为字符串（用逗号分隔）
            return String.join(",", selectedUrls);

        } catch (Exception e) {
            log.error("根据标签和分类在公共图库中查找图片URL时发生异常，标签: {}, 分类: {}, 数量: {}", tag, category, count, e);
            throw new RuntimeException("查找图片失败", e);
        }
    }

    /**
     * 根据多个标签和分类查找公共图库中的图片URL
     * 优先返回同时满足所有条件的结果，如果不足则补充满足部分条件的结果
     *
     * @param tags 标签列表
     * @param category 图片分类（可为空）
     * @param count 要返回的图片数量
     * @return 格式化的图片URL字符串
     */
    public String findPictureUrlsByTagsAndCategory(List<String> tags, String category, Integer count) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(tags), ErrorCode.PARAMS_ERROR, "标签列表不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "查找数量必须大于0");
        ThrowUtils.throwIf(count > 100, ErrorCode.PARAMS_ERROR, "单次查找数量不能超过100");

        try {
            List<String> allUrls = new ArrayList<>();

            // 第一步：查找同时满足所有tags和category的图片
            if (StrUtil.isNotBlank(category)) {
                List<String> exactMatchUrls = findPictureUrlsByTagsAndCategoryExactMatch(tags, category);
                allUrls.addAll(exactMatchUrls);
                log.info("同时满足标签 {} 和分类 [{}] 的图片数量: {}", tags, category, exactMatchUrls.size());
            }

            // 第二步：如果结果不足，查找满足任意tag或category的图片
            if (allUrls.size() < count) {
                List<String> partialMatchUrls = findPictureUrlsByTagsOrCategoryPartialMatch(tags, category);
                // 去重并添加
                for (String url : partialMatchUrls) {
                    if (!allUrls.contains(url)) {
                        allUrls.add(url);
                    }
                }
                log.info("满足标签 {} 或分类 [{}] 的图片总数量: {}", tags, category, allUrls.size());
            }

            // 如果仍然没有找到图片
            if (CollUtil.isEmpty(allUrls)) {
                log.info("根据标签 {} 和分类 [{}] 在公共图库中没有找到任何图片", tags, category);
                return "";
            }

            // 随机打乱列表
            Collections.shuffle(allUrls);

            // 选择指定数量的URL
            List<String> selectedUrls;
            if (allUrls.size() <= count) {
                selectedUrls = allUrls;
                log.info("找到 {} 张图片，少于请求数量 {}，返回全部", allUrls.size(), count);
            } else {
                selectedUrls = allUrls.subList(0, count);
                log.info("找到 {} 张图片，随机选择 {} 张返回", allUrls.size(), count);
            }

            // 将URL列表格式化为字符串（用逗号分隔）
            return String.join(",", selectedUrls);

        } catch (Exception e) {
            log.error("根据多个标签和分类在公共图库中查找图片URL时发生异常，标签: {}, 分类: {}, 数量: {}", tags, category, count, e);
            throw new RuntimeException("查找图片失败", e);
        }
    }

    /**
     * 查找同时满足tag和category的图片URL
     */
    private List<String> findPictureUrlsByExactMatch(String tag, String category) {
        QueryWrapper<Picture> queryWrapper = buildBaseQueryWrapper();

        // 同时满足tag和category
        queryWrapper.like("tags", "\"" + tag + "\"");
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }

        return executeQuery(queryWrapper);
    }

    /**
     * 查找满足tag OR category的图片URL
     */
    private List<String> findPictureUrlsByTagOrCategory(String tag, String category) {
        QueryWrapper<Picture> queryWrapper = buildBaseQueryWrapper();

        // tag OR category
        queryWrapper.and(qw -> {
            qw.like("tags", "\"" + tag + "\"");
            if (StrUtil.isNotBlank(category)) {
                qw.or().eq("category", category);
            }
        });

        return executeQuery(queryWrapper);
    }

    /**
     * 查找同时满足所有tags和category的图片URL
     */
    private List<String> findPictureUrlsByTagsAndCategoryExactMatch(List<String> tags, String category) {
        QueryWrapper<Picture> queryWrapper = buildBaseQueryWrapper();

        // 同时满足所有tags
        for (String tag : tags) {
            if (StrUtil.isNotBlank(tag)) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 同时满足category
        if (StrUtil.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }

        return executeQuery(queryWrapper);
    }

    /**
     * 查找满足任意tag或category的图片URL
     */
    private List<String> findPictureUrlsByTagsOrCategoryPartialMatch(List<String> tags, String category) {
        QueryWrapper<Picture> queryWrapper = buildBaseQueryWrapper();

        // 任意tag OR category
        queryWrapper.and(qw -> {
            // 添加所有tag条件（OR关系）
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);
                if (StrUtil.isNotBlank(tag)) {
                    if (i == 0) {
                        qw.like("tags", "\"" + tag + "\"");
                    } else {
                        qw.or().like("tags", "\"" + tag + "\"");
                    }
                }
            }

            // 添加category条件
            if (StrUtil.isNotBlank(category)) {
                qw.or().eq("category", category);
            }
        });

        return executeQuery(queryWrapper);
    }

    /**
     * 构建基础查询条件
     */
    private QueryWrapper<Picture> buildBaseQueryWrapper() {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        // 只查询公共图库的图片（spaceId为null）
        queryWrapper.isNull("spaceId");

        // 只查询已审核通过的图片
        queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue());

        // 只查询未删除的图片
        queryWrapper.eq("isDelete", 0);

        // 只选择需要的字段
        queryWrapper.select("url");

        return queryWrapper;
    }

    /**
     * 执行查询并返回URL列表
     */
    private List<String> executeQuery(QueryWrapper<Picture> queryWrapper) {
        List<Picture> pictureList = pictureMapper.selectList(queryWrapper);

        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }

        return pictureList.stream()
                .map(Picture::getUrl)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 向下兼容的方法：只根据标签查找（不使用分类）
     */
    public String findPictureUrlsByTag(String tag, Integer count) {
        return findPictureUrlsByTagAndCategory(tag, null, count);
    }

    /**
     * 向下兼容的方法：根据多个标签查找（AND关系，不使用分类）
     */
    public String findPictureUrlsByTags(List<String> tags, Integer count) {
        return findPictureUrlsByTagsAndCategory(tags, null, count);
    }

    /**
     * 根据分类查找图片URL
     *
     * @param category 图片分类
     * @param count 要返回的图片数量
     * @return 格式化的图片URL字符串
     */
    public String findPictureUrlsByCategory(String category, Integer count) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(category), ErrorCode.PARAMS_ERROR, "分类不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "查找数量必须大于0");
        ThrowUtils.throwIf(count > 100, ErrorCode.PARAMS_ERROR, "单次查找数量不能超过100");

        try {
            QueryWrapper<Picture> queryWrapper = buildBaseQueryWrapper();
            queryWrapper.eq("category", category);

            List<String> urlList = executeQuery(queryWrapper);

            if (CollUtil.isEmpty(urlList)) {
                log.info("根据分类 [{}] 在公共图库中没有找到任何图片", category);
                return "";
            }

            // 随机打乱列表
            Collections.shuffle(urlList);

            // 选择指定数量的URL
            List<String> selectedUrls;
            if (urlList.size() <= count) {
                selectedUrls = urlList;
                log.info("根据分类 [{}] 在公共图库中找到 {} 张图片，少于请求数量 {}，返回全部", category, urlList.size(), count);
            } else {
                selectedUrls = urlList.subList(0, count);
                log.info("根据分类 [{}] 在公共图库中找到 {} 张图片，随机选择 {} 张返回", category, urlList.size(), count);
            }

            return String.join(",", selectedUrls);

        } catch (Exception e) {
            log.error("根据分类在公共图库中查找图片URL时发生异常，分类: {}, 数量: {}", category, count, e);
            throw new RuntimeException("查找图片失败", e);
        }
    }

    /**
     * 获取公共图库中所有可用的标签
     */
    public List<String> getAllAvailableTags() {
        try {
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNull("spaceId");
            queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue());
            queryWrapper.eq("isDelete", 0);
            queryWrapper.isNotNull("tags");
            queryWrapper.ne("tags", "");
            queryWrapper.ne("tags", "[]");
            queryWrapper.select("tags");

            List<Picture> pictureList = pictureMapper.selectList(queryWrapper);

            if (CollUtil.isEmpty(pictureList)) {
                log.info("公共图库中没有找到任何带标签的图片");
                return Collections.emptyList();
            }

            List<String> allTags = pictureList.stream()
                    .map(Picture::getTags)
                    .filter(StrUtil::isNotBlank)
                    .flatMap(tagsJson -> {
                        try {
                            return cn.hutool.json.JSONUtil.parseArray(tagsJson)
                                    .stream()
                                    .map(Object::toString)
                                    .filter(StrUtil::isNotBlank);
                        } catch (Exception e) {
                            log.warn("解析标签JSON失败: {}", tagsJson, e);
                            return java.util.stream.Stream.empty();
                        }
                    })
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            log.info("公共图库中共找到 {} 个不同的标签", allTags.size());
            return allTags;

        } catch (Exception e) {
            log.error("获取公共图库可用标签时发生异常", e);
            throw new RuntimeException("获取标签失败", e);
        }
    }

    /**
     * 获取公共图库中所有可用的分类
     */
    public List<String> getAllAvailableCategories() {
        try {
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNull("spaceId");
            queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue());
            queryWrapper.eq("isDelete", 0);
            queryWrapper.isNotNull("category");
            queryWrapper.ne("category", "");
            queryWrapper.select("category");
            queryWrapper.groupBy("category");

            List<Picture> pictureList = pictureMapper.selectList(queryWrapper);

            if (CollUtil.isEmpty(pictureList)) {
                log.info("公共图库中没有找到任何带分类的图片");
                return Collections.emptyList();
            }

            List<String> categories = pictureList.stream()
                    .map(Picture::getCategory)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            log.info("公共图库中共找到 {} 个不同的分类", categories.size());
            return categories;

        } catch (Exception e) {
            log.error("获取公共图库可用分类时发生异常", e);
            throw new RuntimeException("获取分类失败", e);
        }
    }
}
