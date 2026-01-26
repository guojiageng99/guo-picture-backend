package com.guo.guopicturebackend.service;

import com.guo.guopicturebackend.model.dto.space.SpaceAddRequest;
import com.guo.guopicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.entity.User;

/**
* @author 44884
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-20 17:05:18
*/
public interface SpaceService extends IService<Space> {

    public void validSpace(Space space, boolean add);

    public void fillSpaceBySpaceLevel(Space space);

    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
}
