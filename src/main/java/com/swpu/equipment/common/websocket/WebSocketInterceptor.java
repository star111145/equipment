package com.swpu.equipment.common.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Slf4j
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        URI uri = request.getURI();
        String path = uri.getPath();
        String userId = extractUserId(path);
        
        if (userId != null) {
            attributes.put("userId", userId);
            log.info("WebSocket握手: userId={}", userId);
            return true;
        }
        
        log.warn("WebSocket握手失败: 无法获取userId");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractUserId(String path) {
        String[] parts = path.split("/");
        if (parts.length > 2) {
            return parts[parts.length - 1];
        }
        return null;
    }
}
