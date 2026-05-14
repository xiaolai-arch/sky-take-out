package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.properties.SftpProperties;
import com.sky.utils.AliOssUtil;
import com.sky.utils.SftpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StorageConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("初始化阿里云OSS工具类...");
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName()
        );
    }

    @Bean
    public SftpUtil sftpUtil(SftpProperties sftpProperties) {
        log.info("初始化SFTP文件上传工具类...");
        return new SftpUtil(
                sftpProperties.getHost(),
                sftpProperties.getPort(),
                sftpProperties.getUsername(),
                sftpProperties.getPassword(),
                sftpProperties.getUploadDir(),
                sftpProperties.getAccessBaseUrl()
        );
    }
}