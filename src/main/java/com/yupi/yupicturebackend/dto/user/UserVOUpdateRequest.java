package com.yupi.yupicturebackend.dto.user;

import lombok.Data;

import java.util.Date;
@Data
public class UserVOUpdateRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;


    private static final long serialVersionUID = 1L;
}
