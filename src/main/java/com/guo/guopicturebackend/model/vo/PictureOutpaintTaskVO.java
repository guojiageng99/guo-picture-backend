package com.guo.guopicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PictureOutpaintTaskVO implements Serializable {

    private Long id;

    private String status;

    private String modeCode;

    private Integer quotaCost;

    private String outputImageUrl;

    /** 用户可见中文错误说明 */
    private String errorMessage;

    private String aliyunTaskId;

    private Date createTime;

    private Date updateTime;

    private Date finishTime;

    private static final long serialVersionUID = 1L;
}
