package com.guo.guopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;

    /**
     * 空间 id，分表时必须传递。公共图库传 0
     */
    private Long spaceId;
  
    /**  
     * 状态：0-待审核, 1-通过, 2-拒绝  
     */  
    private Integer reviewStatus;  
  
    /**  
     * 审核信息  
     */  
    private String reviewMessage;  
  
  
    private static final long serialVersionUID = 1L;  
}
