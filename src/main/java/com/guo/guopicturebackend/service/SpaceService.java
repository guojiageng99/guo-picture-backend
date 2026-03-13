package com.guo.guopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.model.dto.space.SpaceAddRequest;
import com.guo.guopicturebackend.model.dto.space.SpaceQueryRequest;
import com.guo.guopicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 44884
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-20 17:05:18
*/
public interface SpaceService extends IService<Space> {

    public void validSpace(Space space, boolean add);

    public void fillSpaceBySpaceLevel(Space space);

    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    public void checkSpaceAuth(User loginUser, Space space);

    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);
}
