package com.guo.guopicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片标签字典
 */
@TableName(value = "picture_tag")
@Data
public class PictureTag implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tag_name")
    private String tagName;

    @TableField("usage_count")
    private Integer usageCount;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
