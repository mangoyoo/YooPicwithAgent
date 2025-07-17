package com.mangoyoo.yoopicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mangoyoo.yoopicbackend.dto.space.SpaceAddRequest;
import com.mangoyoo.yoopicbackend.dto.space.SpaceDeleteRequest;
import com.mangoyoo.yoopicbackend.dto.space.SpaceQueryRequest;
import com.mangoyoo.yoopicbackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mangoyoo.yoopicbackend.model.entity.User;
import com.mangoyoo.yoopicbackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 67622
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-04-14 21:12:06
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);


    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    boolean deleteSpace(SpaceDeleteRequest spaceDeleteRequest, User loginUser);

    void validDeleteSpace(Long spaceId, User loginUser);

    void updateSpace(Space space);

    void checkSpaceAuth(User loginUser, Space space);
}
