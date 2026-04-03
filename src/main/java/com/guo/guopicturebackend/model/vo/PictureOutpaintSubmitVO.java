package com.guo.guopicturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureOutpaintSubmitVO implements Serializable {

    /** 业务任务 id，用于轮询 */
    private Long id;

    private String status;

    /** 提交后剩余扩图次数 */
    private Integer outpaintQuotaRemaining;

    private static final long serialVersionUID = 1L;
}
