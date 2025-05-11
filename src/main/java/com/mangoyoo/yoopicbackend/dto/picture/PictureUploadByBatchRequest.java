package com.mangoyoo.yoopicbackend.dto.picture;

import lombok.Data;

@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 20;
    /**
     * 名称前缀
     */
    private String namePrefix;
    private Long spaceId;
}

