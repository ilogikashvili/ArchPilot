package com.ilia.gikashvili.archmain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that extracts and validates the JWT from the Authorization header
 * and populates the SecurityContext.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final com.ilia.gikashvili.archmain.settings.service.SessionService sessions;
    private final com.ilia.gikashvili.archmain.user.repository.UserRepository users;
    private final com.ilia.gikashvili.archmain.agent.service.AgentTokenService agents;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, com.ilia.gikashvili.archmain.settings.service.SessionService sessions, com.ilia.gikashvili.archmain.user.repository.UserRepository users,
                                   @org.springframework.context.annotation.Lazy com.ilia.gikashvili.archmain.agent.service.AgentTokenService agents) {
        this.tokenProvider = tokenProvider; this.sessions=sessions;this.users=users;this.agents=agents;
    }

    /** Agent tokens may only touch their own project plus a few read-only catalog endpoints. */
    static boolean agentMayAccess(String path, java.util.UUID projectId) {
        if (path.equals("/api/v1/agent/me") || path.equals("/api/v1/architecture/component-types")
                || path.equals("/api/v1/architecture/edge-types")) return true;
        String base = "/api/v1/projects/" + projectId;
        if (!(path.equals(base) || path.startsWith(base + "/"))) return false;
        String rest = path.substring(base.length());
        // Agents can never manage members, agents, invitations or delete/leave the project.
        return !(rest.startsWith("/agents") || rest.startsWith("/members") || rest.startsWith("/invitations")
                || rest.equals("/leave") || rest.equals("/archive") || rest.equals("/restore"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && token.startsWith(com.ilia.gikashvili.archmain.agent.service.AgentTokenService.TOKEN_PREFIX)) {
            var agent = agents.authenticate(token);
            String path = request.getRequestURI();
            if (agent.isEmpty() || !agentMayAccess(path, agent.get().getProjectId())
                    || ("DELETE".equals(request.getMethod()) && path.equals("/api/v1/projects/" + agent.get().getProjectId()))) {
                response.setStatus(agent.isEmpty() ? 401 : 403);
                response.setContentType("application/problem+json");
                response.getWriter().write(agent.isEmpty()
                        ? "{\"status\":401,\"title\":\"Invalid or revoked agent token\"}"
                        : "{\"status\":403,\"title\":\"Agent token is not allowed to access this resource\"}");
                return;
            }
            var a = agent.get();
            var principal = new AuthenticatedUser(a.getAgentUserId(), "agent:" + a.getName());
            request.setAttribute("agentId", a.getId());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            filterChain.doFilter(request, response);
            return;
        }

        if (token != null) {
            try {
                Claims claims = tokenProvider.parseToken(token);
                if (tokenProvider.isAccessToken(claims)) {
                    var uid=tokenProvider.getUserId(claims);
                    var account=users.findById(uid).orElseThrow(()->new IllegalArgumentException("Unknown user"));
                    if(account.isDeleted())throw new IllegalArgumentException("Deleted account");
                    var sid=java.util.UUID.fromString(claims.get("sid",String.class));sessions.require(sid,uid);request.setAttribute("sid",sid);
                    AuthenticatedUser user = new AuthenticatedUser(
                            tokenProvider.getUserId(claims),
                            tokenProvider.getEmail(claims)
                    );
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException | org.springframework.security.authentication.BadCredentialsException e) {
                // Invalid token — proceed without authentication
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
