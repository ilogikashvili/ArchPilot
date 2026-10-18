package com.ilia.gikashvili.archmain.user.mapper;

import com.ilia.gikashvili.archmain.user.dto.UserResponse;
import com.ilia.gikashvili.archmain.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps User entity ↔ DTOs.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
