package com.ilia.gikashvili.archmain.user.controller;

import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import com.ilia.gikashvili.archmain.user.dto.ChangePasswordRequest;
import com.ilia.gikashvili.archmain.user.dto.UpdateProfileRequest;
import com.ilia.gikashvili.archmain.user.dto.UserResponse;
import com.ilia.gikashvili.archmain.user.mapper.UserMapper;
import com.ilia.gikashvili.archmain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/users/me")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * PATCH /users/me — Update the current user's profile.
     */
    @PatchMapping
    public ResponseEntity<UserResponse> updateProfile(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        var user = userService.updateProfile(currentUser.id(), request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    /**
     * PATCH /users/me/password — Change the current user's password.
     */
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /users/me — Request account deletion.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@CurrentUser AuthenticatedUser currentUser) {
        userService.requestDeletion(currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
