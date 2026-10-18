package com.ilia.gikashvili.archmain.realtime;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.agent.service.AgentTokenService;
import com.ilia.gikashvili.archmain.security.JwtTokenProvider;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Authenticates WebSocket handshakes with either a user access token or an agent token (?token=...).
 */
@Component
public class ProjectHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider jwt;
    private final ProjectAccessService access;
    private final AgentTokenService agents;
    private final UserRepository users;

    public ProjectHandshakeInterceptor(JwtTokenProvider jwt, ProjectAccessService access, AgentTokenService agents, UserRepository users) {
        this.jwt = jwt; this.access = access; this.agents = agents; this.users = users;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest r, ServerHttpResponse response, WebSocketHandler h, Map<String, Object> attrs) {
        try {
            String token = UriComponentsBuilder.fromUri(r.getURI()).build().getQueryParams().getFirst("token");
            String path = r.getURI().getPath();
            UUID pid = UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
            UUID uid;
            boolean isAgent = false;
            if (token != null && token.startsWith(AgentTokenService.TOKEN_PREFIX)) {
                var agent = agents.authenticate(token).orElseThrow();
                if (!agent.getProjectId().equals(pid)) throw new IllegalArgumentException("Wrong project");
                uid = agent.getAgentUserId();
                isAgent = true;
            } else {
                var claims = jwt.parseToken(token);
                if (!jwt.isAccessToken(claims)) throw new IllegalArgumentException();
                uid = jwt.getUserId(claims);
            }
            access.require(pid, uid, Permission.READ);
            var user = users.findById(uid).orElseThrow();
            attrs.put("projectId", pid);
            attrs.put("userId", uid);
            attrs.put("email", isAgent ? "" : user.getEmail());
            attrs.put("name", user.getDisplayName() == null ? user.getEmail() : user.getDisplayName());
            attrs.put("agent", isAgent);
            attrs.put("canEdit", access.hasPermission(pid, uid, Permission.EDIT));
            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest r, ServerHttpResponse s, WebSocketHandler h, Exception e) {}
}
