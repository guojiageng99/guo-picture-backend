package com.guo.guopicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.dto.message.UserMessageQueryRequest;
import com.guo.guopicturebackend.model.entity.UserMessage;

public interface UserMessageService extends IService<UserMessage> {

    void sendToUser(Long userId, String title, String content, String bizType, Long bizId);

    Page<UserMessage> pageMyMessages(Long userId, UserMessageQueryRequest request);

    boolean markRead(Long userId, Long messageId);

    long countUnread(Long userId);
}
