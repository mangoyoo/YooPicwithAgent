package com.yupi.yupicturebackend.service.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.mapper.PictureMapper;

import com.yupi.yupicturebackend.model.entity.Picture;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PictureDAO  extends ServiceImpl<PictureMapper, Picture> {
    public List<Picture> getListPageBySpaceId(Long spaceId, Long userId) {
        return this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .eq(Picture::getUserId, userId)
                .eq(Picture::getIsDelete, 0).list();
    }
    public void deleteBatchIds(List<Long> list) {
        this.removeByIds(list);
    }

}