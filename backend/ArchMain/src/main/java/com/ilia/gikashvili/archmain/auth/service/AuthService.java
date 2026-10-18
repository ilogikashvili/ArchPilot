package com.ilia.gikashvili.archmain.auth.service;

import com.ilia.gikashvili.archmain.auth.dto.AuthTokenResponse;
import com.ilia.gikashvili.archmain.auth.dto.LoginRequest;
import com.ilia.gikashvili.archmain.auth.dto.RegisterRequest;
import com.ilia.gikashvili.archmain.common.exception.ConflictException;
import com.ilia.gikashvili.archmain.security.JwtTokenProvider;
import com.ilia.gikashvili.archmain.user.entity.User;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    private final com.ilia.gikashvili.archmain.settings.service.SessionService sessions;
    private final com.ilia.gikashvili.archmain.audit.service.AuditService audit;
    @Value("${archmain.admin-emails:}") private String adminEmails;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long accessTokenValidityMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, com.ilia.gikashvili.archmain.settings.service.SessionService sessions, com.ilia.gikashvili.archmain.audit.service.AuditService audit,
                       JwtTokenProvider tokenProvider,
                       @Value("${archmain.jwt.access-token-validity-ms:3600000}") long accessTokenValidityMs) {
        this.userRepository = userRepository; this.sessions=sessions;this.audit=audit;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    /**
     * Register a new user and return JWT tokens.
     */
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(norm(request.email()))) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase(java.util.Locale.ROOT));
        if(userRepository.count()==0 || java.util.Arrays.stream(adminEmails.split(",")).anyMatch(e->e.trim().equalsIgnoreCase(request.email().trim())))user.setRole("ADMIN");
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user = userRepository.save(user);

        audit.record(user.getId(), "REGISTER", "Account registered");
        events.publishEvent(new com.ilia.gikashvili.archmain.admin.service.RegistrationJobs.Registered(user.getId()));
        return generateTokens(user);
    }

    /**
     * Authenticate with email/password and return JWT tokens.
     */
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(norm(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.isDeleted()) {
            throw new BadCredentialsException("Account has been deactivated");
        }

        audit.record(user.getId(), "LOGIN", "Login successful");
        return generateTokens(user);
    }

    /**
     * Refresh an expiring access token.
     */
    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        var claims=tokenProvider.parseToken(refreshToken);
        if(!"refresh".equals(claims.get("type",String.class)))throw new BadCredentialsException("Refresh token required");
        var user=userRepository.findById(tokenProvider.getUserId(claims)).orElseThrow(()->new BadCredentialsException("Unknown account"));
        if(user.isDeleted())throw new BadCredentialsException("Deleted account");
        var session=sessions.require(java.util.UUID.fromString(claims.get("sid",String.class)),user.getId());
        if(!java.util.Objects.equals(session.getRefreshId(),claims.getId()))throw new BadCredentialsException("Refresh token already used");
        session.setRefreshId(java.util.UUID.randomUUID().toString());return tokens(user,session);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    /**
     * Request a password reset. Sends reset link/token.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(norm(email)).ifPresent(user -> {
            String resetToken = tokenProvider.generatePasswordResetToken(user.getId(), user.getEmail());
            user.setResetTokenId(tokenProvider.parseToken(resetToken).getId());userRepository.save(user);
            // Log/emit event for email sender
            log.info("Password reset requested for {}. Reset token: {}", user.getEmail(), resetToken);
        });
    }

    /**
     * Reset password using a valid recovery token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        var claims = tokenProvider.parseToken(token);
        if (!tokenProvider.isPasswordResetToken(claims)) {
            throw new com.ilia.gikashvili.archmain.common.exception.ValidationException("Invalid password reset token");
        }

        var userId = tokenProvider.getUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.ilia.gikashvili.archmain.common.exception.NotFoundException("User not found"));

        if(user.isDeleted() || !java.util.Objects.equals(user.getResetTokenId(),claims.getId()) || user.getResetTokenId()==null)throw new BadCredentialsException("Invalid reset token");
        user.setResetTokenId(null); sessions.revokeAll(user.getId()); audit.record(user.getId(),"PASSWORD_CHANGE","Password reset");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Verify user's email using a verification token.
     */
    @Transactional
    public void verifyEmail(String token) {
        var claims = tokenProvider.parseToken(token);
        if (!tokenProvider.isEmailVerificationToken(claims)) {
            throw new com.ilia.gikashvili.archmain.common.exception.ValidationException("Invalid email verification token");
        }

        var userId = tokenProvider.getUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.ilia.gikashvili.archmain.common.exception.NotFoundException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Resend an email verification token if not yet verified.
     */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(norm(email)).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String token = tokenProvider.generateEmailVerificationToken(user.getId(), user.getEmail());
                log.info("Email verification token for {}: {}", user.getEmail(), token);
            }
        });
    }

    public void logout(){sessions.logout();}
    private AuthTokenResponse generateTokens(User user) {return tokens(user,sessions.create(user.getId()));}
    private AuthTokenResponse tokens(User user,com.ilia.gikashvili.archmain.settings.entity.UserSession session){
        return new AuthTokenResponse(tokenProvider.generateAccessToken(user.getId(),user.getEmail(),session.getId()),tokenProvider.generateRefreshToken(user.getId(),session.getId(),session.getRefreshId()),accessTokenValidityMs/1000);
    }

    /** Emails are stored lower-case; normalise every lookup so "Ilia@Mail.com " and "ilia@mail.com" are the same account. */
    private static String norm(String email) { return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT); }
}
