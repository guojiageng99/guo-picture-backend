package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.guo.guopicturebackend.constant.UserMessageBizType;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.enums.PictureReviewStatusEnum;
import com.guo.guopicturebackend.service.PictureReviewNotifyService;
import com.guo.guopicturebackend.service.UserMessageService;
import com.guo.guopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class PictureReviewNotifyServiceImpl implements PictureReviewNotifyService {

    @Resource
    private UserService userService;

    @Resource
    private UserMessageService userMessageService;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void notifyOwner(Picture picture, PictureReviewStatusEnum decision, String reviewMessage) {
        if (picture == null || picture.getUserId() == null || decision == null) {
            return;
        }
        User owner = userService.getById(picture.getUserId());
        if (owner == null) {
            return;
        }
        boolean pass = PictureReviewStatusEnum.PASS.equals(decision);
        String title = pass ? "图片审核已通过" : "图片审核未通过";
        String reason = StrUtil.blankToDefault(StrUtil.trim(reviewMessage),
                pass ? "您的图片已通过管理员审核。" : "您的图片未通过管理员审核。");
        StringBuilder body = new StringBuilder();
        body.append("图片名称：").append(StrUtil.blankToDefault(picture.getName(), "(无标题)")).append("\n");
        body.append("图片 ID：").append(picture.getId()).append("\n");
        body.append("审核结果：").append(pass ? "通过" : "拒绝").append("\n");
        body.append("说明：").append(reason);

        userMessageService.sendToUser(owner.getId(), title, body.toString(),
                UserMessageBizType.PICTURE_REVIEW, picture.getId());
        log.info("[审核通知] 站内信已写入 userId={} pictureId={} title={}", owner.getId(), picture.getId(), title);

        if (javaMailSender == null) {
            log.debug("[审核通知] 未发邮件：未配置 spring.mail（无 JavaMailSender）");
            return;
        }
        if (StrUtil.isBlank(owner.getUserEmail())) {
            log.debug("[审核通知] 未发邮件：用户未绑定邮箱 userId={}", owner.getId());
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            if (StrUtil.isNotBlank(mailFrom)) {
                mail.setFrom(mailFrom);
            }
            mail.setTo(owner.getUserEmail());
            mail.setSubject("[图库] " + title);
            mail.setText(body.toString());
            javaMailSender.send(mail);
            log.info("[审核通知] 邮件已发送 to={} pictureId={}", owner.getUserEmail(), picture.getId());
        } catch (Exception e) {
            log.warn("[审核通知] 邮件发送失败 userId={} email={}", owner.getId(), owner.getUserEmail(), e);
        }
    }
}
