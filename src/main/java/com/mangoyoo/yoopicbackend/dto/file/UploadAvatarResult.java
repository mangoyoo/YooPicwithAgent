package com.mangoyoo.yoopicbackend.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadAvatarResult {
    /**
     * 图片地址
     */
    private String url;

}
