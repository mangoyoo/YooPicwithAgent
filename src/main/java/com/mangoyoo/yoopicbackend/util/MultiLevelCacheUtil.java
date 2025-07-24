package com.mangoyoo.yoopicbackend.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mangoyoo.yoopicbackend.model.vo.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MultiLevelCacheUtil {

    @Resource(name = "caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Resource(name = "redisCacheManager")
    private CacheManager redisCacheManager;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PICTURE_CACHE_NAME = "pictureList";

    /**
     * 多级缓存获取
     */
    public Page<PictureVO> getFromMultiLevelCache(String cacheKey) {
        try {
            // 1. 先从Caffeine本地缓存获取
            Cache caffeineCache = caffeineCacheManager.getCache(PICTURE_CACHE_NAME);
            if (caffeineCache != null) {
                Cache.ValueWrapper wrapper = caffeineCache.get(cacheKey);
                if (wrapper != null) {
                    log.debug("从Caffeine缓存命中: {}", cacheKey);
                    return (Page<PictureVO>) wrapper.get();
                }
            }

            // 2. Caffeine未命中，从Redis获取
            Cache redisCache = redisCacheManager.getCache(PICTURE_CACHE_NAME);
            if (redisCache != null) {
                Cache.ValueWrapper wrapper = redisCache.get(cacheKey);
                if (wrapper != null) {
                    Page<PictureVO> data = (Page<PictureVO>) wrapper.get();
                    log.debug("从Redis缓存命中: {}", cacheKey);

                    // 3. 将Redis数据回写到Caffeine
                    if (caffeineCache != null) {
                        caffeineCache.put(cacheKey, data);
                        log.debug("数据回写到Caffeine缓存: {}", cacheKey);
                    }
                    return data;
                }
            }

            log.debug("缓存未命中: {}", cacheKey);
            return null;

        } catch (Exception e) {
            log.error("从多级缓存获取数据失败: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 多级缓存存储
     */
    public void putToMultiLevelCache(String cacheKey, Page<PictureVO> data) {
        try {
            // 1. 存储到Redis
            Cache redisCache = redisCacheManager.getCache(PICTURE_CACHE_NAME);
            if (redisCache != null) {
                redisCache.put(cacheKey, data);
                log.debug("数据存储到Redis缓存: {}", cacheKey);
            }

            // 2. 存储到Caffeine
            Cache caffeineCache = caffeineCacheManager.getCache(PICTURE_CACHE_NAME);
            if (caffeineCache != null) {
                caffeineCache.put(cacheKey, data);
                log.debug("数据存储到Caffeine缓存: {}", cacheKey);
            }

        } catch (Exception e) {
            log.error("存储数据到多级缓存失败: {}", cacheKey, e);
        }
    }

    /**
     * 清除多级缓存
     */
    public void evictFromMultiLevelCache(String cacheKey) {
        try {
            // 清除Caffeine缓存
            Cache caffeineCache = caffeineCacheManager.getCache(PICTURE_CACHE_NAME);
            if (caffeineCache != null) {
                caffeineCache.evict(cacheKey);
            }

            // 清除Redis缓存
            Cache redisCache = redisCacheManager.getCache(PICTURE_CACHE_NAME);
            if (redisCache != null) {
                redisCache.evict(cacheKey);
            }

            log.debug("清除多级缓存: {}", cacheKey);
        } catch (Exception e) {
            log.error("清除多级缓存失败: {}", cacheKey, e);
        }
    }

    /**
     * 根据模式清除缓存
     */
    public void evictCacheByPattern(String pattern) {
        try {
            // 清除Redis中匹配的缓存
            redisTemplate.delete(redisTemplate.keys(PICTURE_CACHE_NAME + "::" + pattern + "*"));

            // Caffeine没有模式匹配功能，需要清空整个缓存区域
            Cache caffeineCache = caffeineCacheManager.getCache(PICTURE_CACHE_NAME);
            if (caffeineCache != null) {
                caffeineCache.clear();
            }

            log.debug("根据模式清除缓存: {}", pattern);
        } catch (Exception e) {
            log.error("根据模式清除缓存失败: {}", pattern, e);
        }
    }
}
