package com.mangoyoo.yoopicbackend.dto.user;

import lombok.Data;

@Data
public class UserAvatarUpdateRequest {
    /**
     * id
     */
    private Long id;
    /**
     * 用户头像
     */
    private String userAvatar;
}
