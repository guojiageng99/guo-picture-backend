package com.guo.guopicturebackend.outpainting;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

/**
 * 阿里云 DashScope 错误码 → 用户可见中文（不落库英文原文到 errorMessage）
 */
public final class OutPaintingDashScopeErrorMapper {

    private OutPaintingDashScopeErrorMapper() {
    }

    public static String toUserMessage(String code, String rawMessage) {
        if (StrUtil.isBlank(code)) {
            return summarizeUnknown(rawMessage);
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        switch (c) {
            case "InvalidParameter":
                return "参数不合法，请检查扩图比例或图片地址";
            case "DataInspectionFailed":
            case "DataInspectionFailed.Content":
                return "内容未通过安全审核，请更换图片后重试";
            case "Throttling":
            case "Throttling.RateQuota":
            case "Throttling.AllocationQuota":
                return "当前使用人数较多，请稍后再试";
            case "AccessDenied":
            case "AccessDenied.Unpurchased":
                return "服务未开通或账号无权限，请联系管理员";
            case "Model.AccessDenied":
                return "模型访问被拒绝，请检查账号权限";
            case "InternalError":
                return "扩图服务内部错误，请稍后重试";
            case "ServiceUnavailable":
                return "扩图服务暂时不可用，请稍后重试";
            default:
                return summarizeUnknown(rawMessage);
        }
    }

    private static String summarizeUnknown(String rawMessage) {
        if (StrUtil.isNotBlank(rawMessage) && rawMessage.length() < 120) {
            // 已是中文则可直接展示
            if (rawMessage.matches(".*[\\u4e00-\\u9fa5].*")) {
                return rawMessage;
            }
        }
        return "扩图任务失败，请稍后重试或联系管理员";
    }
}
