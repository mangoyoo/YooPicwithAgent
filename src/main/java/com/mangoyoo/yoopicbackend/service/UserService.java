package com.mangoyoo.yoopicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mangoyoo.yoopicbackend.Manus.MyManus;
import com.mangoyoo.yoopicbackend.app.DefaultExpert;
import com.mangoyoo.yoopicbackend.dto.user.UserAvatarUpdateRequest;
import com.mangoyoo.yoopicbackend.dto.user.UserQueryRequest;
import com.mangoyoo.yoopicbackend.model.entity.User;
import com.mangoyoo.yoopicbackend.model.vo.LoginUserVO;
import com.mangoyoo.yoopicbackend.model.vo.UserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 获取加密后的密码
     *
     * @param userPassword 原始密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request httpRequest 请求方便设置 cookie
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request request
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 用户注销
     *
     * @param request request
     * @return  注销结果
     */
    boolean userLogout(HttpServletRequest request);


    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    UserVO setUserAvatar(Object inputSource, UserAvatarUpdateRequest userAvatarUpdateRequest, User loginUser);

    boolean userChangePassword(String userAccount, String userPassword, String newPassword);

    DefaultExpert getDefaultExpert(HttpServletRequest request);

    MyManus getDefaultManus(HttpServletRequest request);
}


