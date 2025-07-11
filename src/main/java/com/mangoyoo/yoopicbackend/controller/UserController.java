package com.mangoyoo.yoopicbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mangoyoo.yoopicbackend.common.ResultUtils;
import com.mangoyoo.yoopicbackend.dto.user.*;
import com.mangoyoo.yoopicbackend.model.vo.UserVO;
import com.mangoyoo.yoopicbackend.service.UserService;
import com.mangoyoo.yoopicbackend.annotation.AuthCheck;
import com.mangoyoo.yoopicbackend.common.BaseResponse;
import com.mangoyoo.yoopicbackend.common.DeleteRequest;
import com.mangoyoo.yoopicbackend.exception.BusinessException;
import com.mangoyoo.yoopicbackend.exception.ErrorCode;
import com.mangoyoo.yoopicbackend.exception.ThrowUtils;
import com.mangoyoo.yoopicbackend.model.constant.UserConstant;
import com.mangoyoo.yoopicbackend.model.entity.User;
import com.mangoyoo.yoopicbackend.model.vo.LoginUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
    @GetMapping("/health")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 用户更新自己的信息
     */
    @PostMapping("/set/vo")
    public BaseResponse<Boolean> updateUserInfo(@RequestBody UserVOUpdateRequest userVOUpdateRequest,
                                                HttpServletRequest request) {
        // 首先获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 确保参数不为空且包含用户ID
        if (userVOUpdateRequest == null || userVOUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查请求的用户ID是否与当前登录用户ID一致
        if (!userVOUpdateRequest.getId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能更新自己的信息");
        }

        // 更新用户信息
        User user = new User();
        BeanUtils.copyProperties(userVOUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/set/avatar")
    public BaseResponse<UserVO> updateUserAvatar(
    @RequestPart("file")
    MultipartFile multipartFile,
    UserAvatarUpdateRequest userAvatarUpdateRequest,
    HttpServletRequest request) {
        // 首先获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 确保参数不为空且包含用户ID
        if (userAvatarUpdateRequest == null || userAvatarUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查请求的用户ID是否与当前登录用户ID一致
        if (!userAvatarUpdateRequest.getId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能更新自己的信息");
        }


        UserVO userVO = userService.setUserAvatar(multipartFile, userAvatarUpdateRequest, loginUser);
        ThrowUtils.throwIf(userVO == null || userVO.getId() == null, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(userVO);
    }
    /**
     * 用户修改密码
     *
     * @param userPassChangeRequest 用户修改密码请求
     * @return 是否成功
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> userChangePassword(@RequestBody UserPassChangeRequest userPassChangeRequest) {
        ThrowUtils.throwIf(userPassChangeRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userPassChangeRequest.getUserAccount();
        String userPassword = userPassChangeRequest.getUserPassword();
        String newPassword = userPassChangeRequest.getNewPassword();
        boolean result = userService.userChangePassword(userAccount, userPassword, newPassword);
        return ResultUtils.success(result);
    }

}

