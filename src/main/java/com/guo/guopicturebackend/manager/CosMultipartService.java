package com.guo.guopicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.guo.guopicturebackend.config.CosClientConfig;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.exception.ThrowUtils;
import com.guo.guopicturebackend.model.dto.file.CosMultipartCompleteRequest;
import com.guo.guopicturebackend.model.dto.file.CosMultipartInitResponse;
import com.guo.guopicturebackend.model.dto.file.CosMultipartListedPartVO;
import com.guo.guopicturebackend.model.dto.file.CosMultipartPartETag;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListPartsRequest;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * COS 分片上传与断点续传（列举已传分块后仅补传缺失 part，再 complete）
 */
@Service
public class CosMultipartService {

    private static final int MAX_PART_NUMBER = 10000;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    public CosMultipartInitResponse initiate(long userId, String originalFilename) {
        ThrowUtils.throwIf(userId <= 0, ErrorCode.NO_AUTH_ERROR);
        String suffix = StrUtil.blankToDefault(FileUtil.getSuffix(originalFilename), "bin");
        String base = FileUtil.mainName(StrUtil.blankToDefault(originalFilename, "file"));
        base = base.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (base.length() > 80) {
            base = base.substring(0, 80);
        }
        String objectKey = String.format("/multipart/%s/%s_%s.%s", userId, base, IdUtil.fastSimpleUUID(), suffix);

        InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(
                cosClientConfig.getBucket(), objectKey);
        InitiateMultipartUploadResult result = cosClient.initiateMultipartUpload(req);
        return new CosMultipartInitResponse(result.getUploadId(), objectKey);
    }

    public CosMultipartPartETag uploadPart(long userId, String uploadId, String key, int partNumber, File file) {
        assertKeyOwnedByUser(userId, key);
        ThrowUtils.throwIf(StrUtil.hasBlank(uploadId, key), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(partNumber < 1 || partNumber > MAX_PART_NUMBER, ErrorCode.PARAMS_ERROR, "分块序号非法");
        ThrowUtils.throwIf(file == null || !file.isFile() || file.length() == 0, ErrorCode.PARAMS_ERROR, "分块文件无效");

        UploadPartRequest req = new UploadPartRequest();
        req.setBucketName(cosClientConfig.getBucket());
        req.setKey(key);
        req.setUploadId(uploadId);
        req.setPartNumber(partNumber);
        req.setFile(file);
        req.setPartSize(file.length());

        UploadPartResult partResult = cosClient.uploadPart(req);
        CosMultipartPartETag vo = new CosMultipartPartETag();
        vo.setPartNumber(partNumber);
        vo.setETag(partResult.getETag());
        return vo;
    }

    public List<CosMultipartListedPartVO> listParts(long userId, String uploadId, String key) {
        assertKeyOwnedByUser(userId, key);
        ThrowUtils.throwIf(StrUtil.hasBlank(uploadId, key), ErrorCode.PARAMS_ERROR);

        ListPartsRequest req = new ListPartsRequest(cosClientConfig.getBucket(), key, uploadId);
        PartListing listing = cosClient.listParts(req);
        List<CosMultipartListedPartVO> out = new ArrayList<>();
        if (listing.getParts() == null) {
            return out;
        }
        for (PartSummary p : listing.getParts()) {
            Date lm = p.getLastModified();
            out.add(new CosMultipartListedPartVO(
                    p.getPartNumber(),
                    p.getETag(),
                    p.getSize(),
                    lm != null ? lm.getTime() : 0L));
        }
        return out;
    }

    public void complete(long userId, CosMultipartCompleteRequest body) {
        ThrowUtils.throwIf(body == null, ErrorCode.PARAMS_ERROR);
        assertKeyOwnedByUser(userId, body.getKey());
        ThrowUtils.throwIf(StrUtil.hasBlank(body.getUploadId(), body.getKey()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(body.getParts() == null || body.getParts().isEmpty(), ErrorCode.PARAMS_ERROR, "分块列表不能为空");

        List<PartETag> partETags = body.getParts().stream()
                .filter(p -> p.getPartNumber() != null && StrUtil.isNotBlank(p.getETag()))
                .sorted(Comparator.comparingInt(CosMultipartPartETag::getPartNumber))
                .map(p -> new PartETag(p.getPartNumber(), p.getETag()))
                .collect(Collectors.toList());
        ThrowUtils.throwIf(partETags.isEmpty(), ErrorCode.PARAMS_ERROR, "分块列表无效");

        CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(
                cosClientConfig.getBucket(), body.getKey(), body.getUploadId(), partETags);
        cosClient.completeMultipartUpload(req);
    }

    public void abort(long userId, String uploadId, String key) {
        assertKeyOwnedByUser(userId, key);
        ThrowUtils.throwIf(StrUtil.hasBlank(uploadId, key), ErrorCode.PARAMS_ERROR);
        cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(
                cosClientConfig.getBucket(), key, uploadId));
    }

    private void assertKeyOwnedByUser(long userId, String key) {
        ThrowUtils.throwIf(StrUtil.isBlank(key), ErrorCode.PARAMS_ERROR, "key 不能为空");
        String prefix = "/multipart/" + userId + "/";
        ThrowUtils.throwIf(!key.startsWith(prefix), ErrorCode.PARAMS_ERROR, "无权操作该对象");
    }
}
