package com.shortlink.service;

import com.shortlink.dto.request.UpdateProfileRequest;
import com.shortlink.dto.response.UserProfileResponse;

// Service interface for authenticated user profile operations.
public interface UserProfileService {
    UserProfileResponse getCurrentUserProfile(String email);
    UserProfileResponse updateProfileName(String email, UpdateProfileRequest request);
}
