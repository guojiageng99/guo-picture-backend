package com.guo.guopicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.mapper.UserMessageMapper;
import com.guo.guopicturebackend.model.dto.message.UserMessageQueryRequest;
import com.guo.guopicturebackend.model.entity.UserMessage;
import com.guo.guopicturebackend.service.UserMessageService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserMessageServiceImpl extends ServiceImpl<UserMessageMapper, UserMessage>
        implements UserMessageService {

    @Override
    public void sendToUser(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null || StrUtil.isBlank(title)) {
            return;
        }
        UserMessage msg = new UserMessage();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setBizType(bizType);
        msg.setBizId(bizId);
        msg.setIsRead(0);
        msg.setIsDelete(0);
        Date now = new Date();
        msg.setCreateTime(now);
        msg.setUpdateTime(now);
        this.save(msg);
    }

    @Override
    public Page<UserMessage> pageMyMessages(Long userId, UserMessageQueryRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = request.getCurrent();
        long pageSize = request.getPageSize();
        Page<UserMessage> page = new Page<>(current, pageSize);
        LambdaQueryWrapper<UserMessage> q = new LambdaQueryWrapper<>();
        q.eq(UserMessage::getUserId, userId);
        if (Boolean.TRUE.equals(request.getUnreadOnly())) {
            q.eq(UserMessage::getIsRead, 0);
        }
        q.orderByDesc(UserMessage::getCreateTime);
        return this.page(page, q);
    }

    @Override
    public boolean markRead(Long userId, Long messageId) {
        if (userId == null || messageId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserMessage one = this.getById(messageId);
        if (one == null || ObjUtil.notEqual(one.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "消息不存在");
        }
        return this.update(new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getId, messageId)
                .eq(UserMessage::getUserId, userId)
                .set(UserMessage::getIsRead, 1)
                .set(UserMessage::getUpdateTime, new Date()));
    }

    @Override
    public long countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        return this.lambdaQuery()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .count();
    }
}
