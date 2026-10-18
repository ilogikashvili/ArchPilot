package com.ilia.gikashvili.archmain.auth.controller;

import com.ilia.gikashvili.archmain.auth.dto.*;
import com.ilia.gikashvili.archmain.auth.service.AuthService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import com.ilia.gikashvili.archmain.user.entity.User;
import com.ilia.gikashvili.archmain.user.mapper.UserMapper;
import com.ilia.gikashvili.archmain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserService userService, UserMapper userMapper) {
        this.authService = authService;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * POST /auth/register — Create an account.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokenResponse tokens = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }

    /**
     * POST /auth/login — Authenticate and create a session.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(tokens);
    }

    /**
     * POST /auth/logout — End the current session.
     * With stateless JWT, the client simply discards the token.
     * Server-side token blacklisting can be added later.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /auth/me — Get the current user's identity.
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@CurrentUser AuthenticatedUser currentUser) {
        User user = userService.getById(currentUser.id());
        return ResponseEntity.ok(new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.getCreatedAt()
        ));
    }

    /**
     * POST /auth/refresh — Refresh an expiring access credential.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        AuthTokenResponse tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    /**
     * POST /auth/forgot-password — Request password recovery.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email != null && !email.isBlank()) {
            authService.forgotPassword(email);
        }
        return ResponseEntity.accepted().build();
    }

    /**
     * POST /auth/reset-password — Reset password using a recovery token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/verify-email — Verify an email address.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if(token==null || token.isBlank())throw new com.ilia.gikashvili.archmain.common.exception.ValidationException("Token is required");
        if (token != null && !token.isBlank()) {
            authService.verifyEmail(token);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/resend-verification — Resend the verification email.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email != null && !email.isBlank()) {
            authService.resendVerification(email);
        }
        return ResponseEntity.accepted().build();
    }
}
