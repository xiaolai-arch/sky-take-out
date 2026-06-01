package com.sky.task;

import com.sky.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WebSocketTask {
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 通过WebSocket每隔5秒向客户端发送消息（开发调试用，上线前需关闭）
     */
    // @Scheduled(cron = "0/5 * * * * ?")
    public void sendMessageToClient() {
        // 开发调试用，暂时关闭，避免与来单提醒冲突
    }
}