package com.guo.guopicturebackend.service;

import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.enums.PictureReviewStatusEnum;

/**
 * 图片人工审核结果：站内信 + 邮件
 */
public interface PictureReviewNotifyService {

    void notifyOwner(Picture picture, PictureReviewStatusEnum decision, String reviewMessage);
}
