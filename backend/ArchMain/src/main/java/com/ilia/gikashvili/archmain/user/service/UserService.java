package com.ilia.gikashvili.archmain.user.service;

import com.ilia.gikashvili.archmain.common.exception.ConflictException;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.user.dto.ChangePasswordRequest;
import com.ilia.gikashvili.archmain.user.dto.UpdateProfileRequest;
import com.ilia.gikashvili.archmain.user.entity.User;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.ilia.gikashvili.archmain.settings.service.SessionService sessions;
    private final com.ilia.gikashvili.archmain.audit.service.AuditService audit;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, com.ilia.gikashvili.archmain.settings.service.SessionService sessions, com.ilia.gikashvili.archmain.audit.service.AuditService audit) {
        this.userRepository = userRepository;this.sessions=sessions;this.audit=audit;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User with email '" + email + "' not found"));
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getById(userId);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getById(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ConflictException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        sessions.revokeAll(userId);audit.record(userId,"PASSWORD_CHANGE","Password changed");
        userRepository.save(user);
    }

    @Transactional
    public void requestDeletion(UUID userId) {
        User user = getById(userId);
        user.setDeleted(true);sessions.revokeAll(userId);
        userRepository.save(user);
        // TODO: schedule hard deletion, revoke tokens, etc.
    }
}
