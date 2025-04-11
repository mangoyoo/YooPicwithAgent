package com.mangoyoo.yoopicbackend.dto.user;

import lombok.Data;

@Data
public class UserPassChangeRequest {

    private static final long serialVersionUID = 7171201169923493202L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
    /**
     * 新密码
     */
    private String newPassword;
}
