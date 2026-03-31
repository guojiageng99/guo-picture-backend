package com.guo.guopicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户更新个人资料（不允许改角色、账号等）
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    private String userName;

    private String userAvatar;

    private String userProfile;

    private static final long serialVersionUID = 1L;
}
