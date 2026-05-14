package com.sky.utils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class SftpUtil {

    private String host;
    private int port;
    private String username;
    private String password;
    private String uploadDir;
    private String accessBaseUrl;

    public String upload(byte[] bytes, String objectName) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(10000);

            ensureDir(channel, uploadDir);

            String filePath = uploadDir + "/" + objectName;
            channel.put(new ByteArrayInputStream(bytes), filePath);

            String url = accessBaseUrl + "/" + objectName;
            log.info("文件上传到: {}", url);
            return url;
        } catch (Exception e) {
            log.error("SFTP上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void ensureDir(ChannelSftp channel, String dir) throws SftpException {
        String[] folders = dir.split("/");
        String current = "";
        for (String folder : folders) {
            if (folder.isEmpty()) continue;
            current += "/" + folder;
            try {
                channel.stat(current);
            } catch (SftpException e) {
                channel.mkdir(current);
            }
        }
    }
}