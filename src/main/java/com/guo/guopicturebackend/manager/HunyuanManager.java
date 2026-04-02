package com.guo.guopicturebackend.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.guo.guopicturebackend.config.HunyuanProperties;
import com.guo.guopicturebackend.model.dto.hunyuan.ChatTopicInfoCategory;
import com.guo.guopicturebackend.model.dto.hunyuan.PictureModerationResult;
import com.guo.guopicturebackend.service.PictureCategoryService;
import com.guo.guopicturebackend.service.PictureTagService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.hunyuan.v20230901.HunyuanClient;
import com.tencentcloudapi.hunyuan.v20230901.models.ChatCompletionsRequest;
import com.tencentcloudapi.hunyuan.v20230901.models.ChatCompletionsResponse;
import com.tencentcloudapi.hunyuan.v20230901.models.Choice;
import com.tencentcloudapi.hunyuan.v20230901.models.Content;
import com.tencentcloudapi.hunyuan.v20230901.models.ImageUrl;
import com.tencentcloudapi.hunyuan.v20230901.models.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 腾讯混元 ChatCompletions（含 hunyuan-vision 看图）
 */
@Component
@Slf4j
public class HunyuanManager {

    private static final String MODERATION_PROMPT = "你是图片内容安全审核员。判断该图是否适合在公开图库展示（禁止色情、暴力血腥、违禁、明显侵权标识等）。"
            + "只输出一个 JSON 对象，不要 markdown，不要其它文字。格式：{\"pass\":true或false,\"reason\":\"简短中文理由\"}。"
            + "若有任何不确定，pass 请填 true 交由人工复核。";

    @Resource
    private HunyuanProperties hunyuanProperties;

    @Resource
    private PictureTagService pictureTagService;

    @Resource
    private PictureCategoryService pictureCategoryService;

    /**
     * 调用混元理解图片，返回第一条 choice（与教程一致）
     */
    public Choice getChatTopicInfo(String imgUrl) throws TencentCloudSDKException {
        if (!canCallVision(imgUrl)) {
            return null;
        }
        return visionChat(imgUrl, buildVisionPrompt());
    }

    /**
     * 图片内容初审（JSON：pass / reason）
     */
    public PictureModerationResult moderatePictureByUrl(String imgUrl) {
        if (!canCallVision(imgUrl)) {
            return null;
        }
        try {
            Choice choice = visionChat(imgUrl, MODERATION_PROMPT);
            if (choice == null || choice.getMessage() == null) {
                return null;
            }
            String content = choice.getMessage().getContent();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            String json = stripMarkdownCodeFence(content.trim());
            return JSONUtil.toBean(json, PictureModerationResult.class);
        } catch (TencentCloudSDKException e) {
            log.error("混元 AI 审核 API 失败, imgUrl={}", imgUrl, e);
            return null;
        } catch (Exception e) {
            log.error("混元 AI 审核解析失败, imgUrl={}", imgUrl, e);
            return null;
        }
    }

    /**
     * 解析混元返回内容为结构化对象（教程：getMessage().getContent() → JSONUtil.toBean）
     */
    public ChatTopicInfoCategory getChatTopicInfoCategory(String imgUrl) {
        if (!hunyuanProperties.isEnabled()
                || StrUtil.hasBlank(hunyuanProperties.getSecretId(), hunyuanProperties.getSecretKey())) {
            return null;
        }
        try {
            Choice chatTopicInfo = getChatTopicInfo(imgUrl);
            if (chatTopicInfo == null || chatTopicInfo.getMessage() == null) {
                return null;
            }
            String content = chatTopicInfo.getMessage().getContent();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            String json = stripMarkdownCodeFence(content.trim());
            return JSONUtil.toBean(json, ChatTopicInfoCategory.class);
        } catch (TencentCloudSDKException e) {
            log.error("混元 API 调用失败, imgUrl={}", imgUrl, e);
            return null;
        } catch (Exception e) {
            log.error("混元话题信息解析失败, imgUrl={}", imgUrl, e);
            return null;
        }
    }

    private boolean canCallVision(String imgUrl) {
        return hunyuanProperties.isEnabled()
                && StrUtil.isNotBlank(hunyuanProperties.getSecretId())
                && StrUtil.isNotBlank(hunyuanProperties.getSecretKey())
                && StrUtil.isNotBlank(imgUrl);
    }

    private HunyuanClient newHunyuanClient() throws TencentCloudSDKException {
        Credential cred = new Credential(hunyuanProperties.getSecretId(), hunyuanProperties.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("hunyuan.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new HunyuanClient(cred, hunyuanProperties.getRegion(), clientProfile);
    }

    private Choice visionChat(String imgUrl, String textPrompt) throws TencentCloudSDKException {
        HunyuanClient client = newHunyuanClient();
        ChatCompletionsRequest req = new ChatCompletionsRequest();
        req.setModel(hunyuanProperties.getModel());
        req.setStream(false);

        Message userMsg = new Message();
        userMsg.setRole("user");
        Content textPart = new Content();
        textPart.setType("text");
        textPart.setText(textPrompt);
        Content imagePart = new Content();
        imagePart.setType("image_url");
        ImageUrl iu = new ImageUrl();
        iu.setUrl(imgUrl);
        imagePart.setImageUrl(iu);
        userMsg.setContents(new Content[]{textPart, imagePart});

        req.setMessages(new Message[]{userMsg});
        ChatCompletionsResponse resp = client.ChatCompletions(req);
        if (resp.getChoices() == null || resp.getChoices().length == 0) {
            return null;
        }
        return resp.getChoices()[0];
    }

    private String buildVisionPrompt() {
        List<String> tagNames = pictureTagService.listAllTagNamesForPicker();
        List<String> catNames = pictureCategoryService.listAllCategoryNamesForPicker();
        StringBuilder sb = new StringBuilder();
        sb.append("你是图库助手。请根据图片内容输出一段 JSON，且只输出 JSON 对象，不要 markdown 代码块，不要其它说明文字。\n");
        sb.append("JSON 字段：introduction（字符串，一句话简介），tags（字符串数组，从给定标签中选 0～8 个），category（字符串，从给定分类中选恰好一个或空字符串）。\n");
        if (CollUtil.isNotEmpty(tagNames)) {
            sb.append("可选标签：").append(JSONUtil.toJsonStr(tagNames)).append("\n");
        } else {
            sb.append("可选标签：[]\n");
        }
        if (CollUtil.isNotEmpty(catNames)) {
            sb.append("可选分类：").append(JSONUtil.toJsonStr(catNames)).append("\n");
        } else {
            sb.append("可选分类：[]\n");
        }
        sb.append("若无法判断分类则 category 填 \"\"；tags 只能使用上面列表中的名称，不要编造新标签。");
        return sb.toString();
    }

    private static String stripMarkdownCodeFence(String raw) {
        String t = raw;
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
            return t.trim();
        }
        return t;
    }
}
