package com.ilia.gikashvili.archmain.realtime;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class RealtimeConfig implements WebSocketConfigurer {
 private final ProjectSocket socket;private final ProjectHandshakeInterceptor interceptor;
 public RealtimeConfig(ProjectSocket s,ProjectHandshakeInterceptor i){socket=s;interceptor=i;}
 public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(socket,"/ws/projects/{projectId}").addInterceptors(interceptor).setAllowedOriginPatterns("*");}
}
