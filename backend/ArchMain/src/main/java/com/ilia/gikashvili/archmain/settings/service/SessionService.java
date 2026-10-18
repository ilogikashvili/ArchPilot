package com.ilia.gikashvili.archmain.settings.service;
import java.util.*; import java.time.Instant; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.context.request.*; import jakarta.servlet.http.HttpServletRequest; import com.ilia.gikashvili.archmain.settings.entity.UserSession; import com.ilia.gikashvili.archmain.settings.repository.UserSessionRepository; import org.springframework.security.authentication.BadCredentialsException;
@Service public class SessionService {
 private final UserSessionRepository repo; public SessionService(UserSessionRepository repo){this.repo=repo;}
 @Transactional public UserSession create(UUID uid){var s=new UserSession();s.setUserId(uid);var a=RequestContextHolder.getRequestAttributes();if(a instanceof ServletRequestAttributes r){var q=r.getRequest();String ua=q.getHeader("User-Agent");s.setUserAgent(ua==null?null:ua.substring(0,Math.min(1000,ua.length())));s.setIp(q.getRemoteAddr());}s.setLastSeenAt(Instant.now());s.setRefreshId(UUID.randomUUID().toString());return repo.save(s);}
 @Transactional public UserSession require(UUID sid,UUID uid){var s=repo.findByIdAndUserId(sid,uid).orElseThrow(()->new BadCredentialsException("Invalid session"));if(s.getRevoked())throw new BadCredentialsException("Revoked session");s.setLastSeenAt(Instant.now());return s;}
 @Transactional public void revokeAll(UUID uid){repo.findAllByUserId(uid).forEach(s->{s.setRevoked(true);repo.save(s);});}
 @Transactional public void logout(){var a=(ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();Object sid=a.getRequest().getAttribute("sid");if(sid!=null)repo.findById((UUID)sid).ifPresent(s->{s.setRevoked(true);repo.save(s);});}
}
