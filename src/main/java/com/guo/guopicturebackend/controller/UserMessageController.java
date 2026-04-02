package com.guo.guopicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.common.BaseResponse;
import com.guo.guopicturebackend.common.ResultUtils;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.exception.ThrowUtils;
import com.guo.guopicturebackend.model.dto.message.UserMessageQueryRequest;
import com.guo.guopicturebackend.model.dto.message.UserMessageReadRequest;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.entity.UserMessage;
import com.guo.guopicturebackend.service.UserMessageService;
import com.guo.guopicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 站内消息中心（当前用户）
 */
@RestController
@RequestMapping("/user/message")
public class UserMessageController {

    @Resource
    private UserService userService;

    @Resource
    private UserMessageService userMessageService;

    @PostMapping("/list/page")
    public BaseResponse<Page<UserMessage>> listPage(@RequestBody UserMessageQueryRequest request,
                                                    HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        Page<UserMessage> page = userMessageService.pageMyMessages(loginUser.getId(), request);
        return ResultUtils.success(page);
    }

    @PostMapping("/read")
    public BaseResponse<Boolean> markRead(@RequestBody UserMessageReadRequest request,
                                            HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        boolean ok = userMessageService.markRead(loginUser.getId(), request.getId());
        return ResultUtils.success(ok);
    }

    @GetMapping("/unread/count")
    public BaseResponse<Long> unreadCount(HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(userMessageService.countUnread(loginUser.getId()));
    }
}
