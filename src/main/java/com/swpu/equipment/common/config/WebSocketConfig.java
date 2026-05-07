package com.swpu.equipment.common.config;

import com.swpu.equipment.common.websocket.WebSocketHandler;
import com.swpu.equipment.common.websocket.WebSocketInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
// 配置WebSocket

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/ws/{userId}")
                .addInterceptors(webSocketInterceptor())
                .setAllowedOrigins("*");
        registry.addHandler(webSocketHandler(), "/api/ws/{userId}")
                .addInterceptors(webSocketInterceptor())
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler webSocketHandler() {
        return new WebSocketHandler();
    }

    @Bean
    public WebSocketInterceptor webSocketInterceptor() {
        return new WebSocketInterceptor();
    }
}
