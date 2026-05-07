package com.swpu.equipment.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        sessions.put(userId, session);
        log.info("WebSocket连接建立: userId={}, sessionId={}, 当前在线用户数={}", userId, session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        sessions.remove(userId);
        log.info("WebSocket连接关闭: userId={}, sessionId={}, 当前在线用户数={}", userId, session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("收到消息: {}", message.getPayload());
    }

    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }

    public static void sendToAll(String message) {
        log.info("WebSocket广播消息: message={}, 在线session数={}", message, sessions.size());
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    log.debug("消息发送成功: sessionId={}", session.getId());
                }
            } catch (IOException e) {
                log.error("广播消息失败: error={}", e.getMessage());
            }
        }
    }

    public static int getOnlineCount() {
        return sessions.size();
    }

    public static int getSessionCount() {
        return sessions.size();
    }
}
