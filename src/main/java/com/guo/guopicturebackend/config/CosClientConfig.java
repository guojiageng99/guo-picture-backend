package com.guo.guopicturebackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {  
  
    /**  
     * 域名  
     */  
    private String host;  
  
    /**  
     * secretId  
     */  
    private String secretId;  
  
    /**  
     * 密钥（注意不要泄露）  
     */  
    private String secretKey;  
  
    /**  
     * 区域  
     */  
    private String region;  
  
    /**  
     * 桶名  
     */  
    private String bucket;

    /**
     * 超过该大小使用 TransferManager 自动分片上传（字节），默认 5MB
     */
    private long multipartUploadThreshold = 5L * 1024 * 1024;

    /**
     * 分片最小大小（字节），默认 1MB
     */
    private long minimumUploadPartSize = 1L * 1024 * 1024;

    @Bean
    public COSClient cosClient() {
        // 初始化用户身份信息(secretId, secretKey)  
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224  
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 生成cos客户端  
        return new COSClient(cred, clientConfig);  
    }

    @Bean(destroyMethod = "shutdownNow")
    public TransferManager cosTransferManager(COSClient cosClient) {
        TransferManager transferManager = new TransferManager(cosClient);
        TransferManagerConfiguration cfg = new TransferManagerConfiguration();
        cfg.setMultipartUploadThreshold(multipartUploadThreshold);
        cfg.setMinimumUploadPartSize(minimumUploadPartSize);
        transferManager.setConfiguration(cfg);
        return transferManager;
    }
}
