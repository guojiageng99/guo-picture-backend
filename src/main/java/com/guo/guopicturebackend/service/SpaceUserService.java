package com.guo.guopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guo.guopicturebackend.model.dto.spaceUser.SpaceUserAddRequest;
import com.guo.guopicturebackend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.guo.guopicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.vo.SpaceUserVO;

import java.util.List;

/**
* @author 44884
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-01-27 15:27:57
*/
public interface SpaceUserService extends IService<SpaceUser> {

    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    public void validSpaceUser(SpaceUser spaceUser, boolean add);

    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
