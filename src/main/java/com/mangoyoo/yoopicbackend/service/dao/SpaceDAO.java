package com.mangoyoo.yoopicbackend.service.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mangoyoo.yoopicbackend.mapper.SpaceMapper;
import com.mangoyoo.yoopicbackend.model.entity.Space;
import org.springframework.stereotype.Service;

@Service
public class SpaceDAO  extends ServiceImpl<SpaceMapper, Space> {
    public boolean updateUsage(Long spaceId, Long picSize) {
        return this.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("totalSize = totalSize + " + picSize)
                .setSql("totalCount = totalCount + 1")
                .update();
    }
    public boolean delPictureUpdateSpaceUsage(Long spaceId, Long picSize) {
        return this.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("totalSize = totalSize - " + picSize)
                .setSql("totalCount = totalCount - 1")
                .update();
    }

}

