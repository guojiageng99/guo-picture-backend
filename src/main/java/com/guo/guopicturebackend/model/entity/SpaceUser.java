package com.guo.guopicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

/**
 * 空间用户关联
 * @TableName space_user
 */
@TableName(value ="space_user")
public class SpaceUser implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    public Long getId() {
        return id;
    }

    /**
     * id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 空间 id
     */
    public Long getSpaceId() {
        return spaceId;
    }

    /**
     * 空间 id
     */
    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    /**
     * 用户 id
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 用户 id
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 空间角色：viewer/editor/admin
     */
    public String getSpaceRole() {
        return spaceRole;
    }

    /**
     * 空间角色：viewer/editor/admin
     */
    public void setSpaceRole(String spaceRole) {
        this.spaceRole = spaceRole;
    }

    /**
     * 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}