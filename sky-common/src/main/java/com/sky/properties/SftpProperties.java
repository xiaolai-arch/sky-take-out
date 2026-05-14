package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.sftp")
@Data
public class SftpProperties {

    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String uploadDir;
    private String accessBaseUrl;

}