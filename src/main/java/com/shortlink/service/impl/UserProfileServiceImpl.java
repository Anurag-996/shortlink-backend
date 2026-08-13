package com.shortlink.service.impl;

import com.shortlink.dto.request.UpdateProfileRequest;
import com.shortlink.dto.response.UserProfileResponse;
import com.shortlink.exception.BadRequestException;
import com.shortlink.service.UserProfileService;
import com.shortlink.user.User;
import com.shortlink.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation handling user profile retrieval and name updates for verified users.
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled()
        );
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfileName(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!user.isEnabled()) {
            throw new BadRequestException("User email must be verified to update profile details");
        }

        user.setName(request.name().trim());
        User updated = userRepository.save(user);

        log.info("Updated profile name for verified user email: {}", user.getEmail());

        return new UserProfileResponse(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                updated.getRole().name(),
                updated.isEnabled()
        );
    }
}
