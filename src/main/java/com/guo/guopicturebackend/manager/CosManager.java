package com.guo.guopicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.guo.guopicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private TransferManager transferManager;

    /**
     * 上传对象（大文件走 TransferManager 自动分片）
     */
    public void putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        if (file.length() < cosClientConfig.getMultipartUploadThreshold()) {
            cosClient.putObject(putObjectRequest);
            return;
        }
        try {
            Upload upload = transferManager.upload(putObjectRequest);
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CosClientException("上传被中断", e);
        }
    }

    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片（数据万象处理）；超过阈值时使用 TransferManager 分片上传，结果含 CI 信息。
     */
    public UploadResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = buildPicturePutRequest(key, file);
        try {
            if (file.length() < cosClientConfig.getMultipartUploadThreshold()) {
                PutObjectResult pr = cosClient.putObject(putObjectRequest);
                return toUploadResult(pr, key);
            }
            Upload upload = transferManager.upload(putObjectRequest);
            return upload.waitForUploadResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CosClientException("上传被中断", e);
        }
    }

    private PutObjectRequest buildPicturePutRequest(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));
            rules.add(thumbnailRule);
        }
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return putObjectRequest;
    }

    private static UploadResult toUploadResult(PutObjectResult pr, String key) {
        UploadResult ur = new UploadResult();
        ur.setKey(key);
        ur.setRequestId(pr.getRequestId());
        ur.setDateStr(pr.getDateStr());
        ur.setETag(pr.getETag());
        ur.setCrc64Ecma(pr.getCrc64Ecma());
        ur.setCiUploadResult(pr.getCiUploadResult());
        return ur;
    }

    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
