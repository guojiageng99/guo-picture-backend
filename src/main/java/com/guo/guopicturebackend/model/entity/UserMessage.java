package com.guo.guopicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户站内消息
 */
@TableName(value = "user_message")
@Data
public class UserMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    /** 0 未读 1 已读 */
    private Integer isRead;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
