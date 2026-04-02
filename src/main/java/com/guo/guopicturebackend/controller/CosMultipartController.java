package com.guo.guopicturebackend.controller;

import com.guo.guopicturebackend.common.BaseResponse;
import com.guo.guopicturebackend.common.ResultUtils;
import com.guo.guopicturebackend.config.CosClientConfig;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.manager.CosMultipartService;
import com.guo.guopicturebackend.model.dto.file.CosMultipartCompleteRequest;
import com.guo.guopicturebackend.model.dto.file.CosMultipartInitResponse;
import com.guo.guopicturebackend.model.dto.file.CosMultipartListedPartVO;
import com.guo.guopicturebackend.model.dto.file.CosMultipartPartETag;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COS 分片上传与断点续传：初始化 → 多次 uploadPart →（可选 listParts）→ complete。
 * 完成后对象 URL 为 host + key。
 */
@Slf4j
@RestController
@RequestMapping("/file/cos/multipart")
public class CosMultipartController {

    @Resource
    private CosMultipartService cosMultipartService;

    @Resource
    private UserService userService;

    @Resource
    private CosClientConfig cosClientConfig;

    @PostMapping("/init")
    public BaseResponse<CosMultipartInitResponse> init(
            @RequestParam("filename") String filename,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        CosMultipartInitResponse res = cosMultipartService.initiate(loginUser.getId(), filename);
        return ResultUtils.success(res);
    }

    @PostMapping("/part")
    public BaseResponse<CosMultipartPartETag> uploadPart(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("key") String key,
            @RequestParam("partNumber") int partNumber,
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        File tmp = null;
        try {
            tmp = File.createTempFile("cos-part-", null);
            multipartFile.transferTo(tmp);
            CosMultipartPartETag tag = cosMultipartService.uploadPart(
                    loginUser.getId(), uploadId, key, partNumber, tmp);
            return ResultUtils.success(tag);
        } catch (Exception e) {
            log.error("分片上传失败 uploadId={} part={}", uploadId, partNumber, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分片上传失败");
        } finally {
            if (tmp != null && tmp.exists() && !tmp.delete()) {
                log.warn("临时分片文件删除失败 {}", tmp.getAbsolutePath());
            }
        }
    }

    @GetMapping("/parts")
    public BaseResponse<List<CosMultipartListedPartVO>> listParts(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("key") String key,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(cosMultipartService.listParts(loginUser.getId(), uploadId, key));
    }

    @PostMapping("/complete")
    public BaseResponse<Map<String, String>> complete(
            @RequestBody CosMultipartCompleteRequest body,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        cosMultipartService.complete(loginUser.getId(), body);
        Map<String, String> m = new HashMap<>(2);
        m.put("key", body.getKey());
        m.put("url", cosClientConfig.getHost() + body.getKey());
        return ResultUtils.success(m);
    }

    @PostMapping("/abort")
    public BaseResponse<Boolean> abort(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("key") String key,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        cosMultipartService.abort(loginUser.getId(), uploadId, key);
        return ResultUtils.success(true);
    }
}
