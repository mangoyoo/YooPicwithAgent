package com.mangoyoo.yoopicbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mangoyoo.yoopicbackend.dto.file.UploadAvatarResult;
import com.mangoyoo.yoopicbackend.dto.user.UserAvatarUpdateRequest;
import com.mangoyoo.yoopicbackend.dto.user.UserQueryRequest;
import com.mangoyoo.yoopicbackend.manager.upload.PictureUploadTemplate;
import com.mangoyoo.yoopicbackend.model.vo.UserVO;
import com.mangoyoo.yoopicbackend.service.UserService;
import com.mangoyoo.yoopicbackend.enums.UserRoleEnum;
import com.mangoyoo.yoopicbackend.exception.BusinessException;
import com.mangoyoo.yoopicbackend.exception.ErrorCode;
import com.mangoyoo.yoopicbackend.exception.ThrowUtils;
import com.mangoyoo.yoopicbackend.manager.upload.FilePictureUpload;
import com.mangoyoo.yoopicbackend.model.auth.StpKit;
import com.mangoyoo.yoopicbackend.model.entity.User;
import com.mangoyoo.yoopicbackend.model.vo.LoginUserVO;
import com.mangoyoo.yoopicbackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import com.mangoyoo.yoopicbackend.config.CosClientConfig;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.mangoyoo.yoopicbackend.model.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 67622
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-03 17:50:33
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private CosClientConfig cosClientConfig;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());
        int randomNum = new Random().nextInt(17) + 1; // 生成 1~17 的随机数
        user.setUserAvatar(String.format(cosClientConfig.getHost() + "/UserAvatar/default/default%d", randomNum));
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "mangoyoo";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAllBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);

    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
    @Override
    public UserVO setUserAvatar(Object inputSource, UserAvatarUpdateRequest userAvatarUpdateRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        String uploadPathPrefix;
        uploadPathPrefix=String.format("UserAvatar/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        UploadAvatarResult uploadAvatarResult = pictureUploadTemplate.uploadUserAvatar(inputSource, uploadPathPrefix);
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserAvatar(uploadAvatarResult.getUrl());
        boolean result = this.updateById(user);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
    /**
     * 修改用户密码
     * @param userAccount 用户账号
     * @param userPassword 原密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    @Override
    public boolean userChangePassword(String userAccount, String userPassword, String newPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码格式错误");
        }
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码过短");
        }

        // 检查新密码不能与原密码相同
        if (userPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与原密码相同");
        }

        // 2. 验证原密码是否正确
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在或密码错误
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或原密码错误");
        }

        // 3. 加密新密码并更新
        String encryptNewPassword = getEncryptPassword(newPassword);
        user.setUserPassword(encryptNewPassword);
        boolean updateResult = this.updateById(user);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码修改失败，数据库错误");
        }

        return true;
    }


}





