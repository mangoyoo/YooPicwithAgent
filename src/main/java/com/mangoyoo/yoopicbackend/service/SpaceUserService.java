package com.mangoyoo.yoopicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mangoyoo.yoopicbackend.dto.space.SpaceUserAddRequest;
import com.mangoyoo.yoopicbackend.dto.space.SpaceUserQueryRequest;
import com.mangoyoo.yoopicbackend.model.vo.SpaceUserVO;
import generator.domain.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 67622
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-04-18 19:54:29
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
