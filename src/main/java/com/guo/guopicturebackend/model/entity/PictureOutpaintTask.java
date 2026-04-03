package com.guo.guopicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("picture_outpaint_task")
public class PictureOutpaintTask implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long pictureId;

    private Long spaceId;

    private String aliyunTaskId;

    /** 提交到阿里云的时间（用于超时，不受轮询刷新 updateTime 影响） */
    private Date aliSubmittedAt;

    private String status;

    private String modeCode;

    private Integer quotaCost;

    private Integer quotaRefunded;

    private String requestJson;

    private String outputImageUrl;

    private String errorCode;

    private String errorMessage;

    private String rawError;

    private Integer reconcileAttempts;

    private Date createTime;

    private Date updateTime;

    private Date finishTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
