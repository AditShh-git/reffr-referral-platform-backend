package com.Reffr_Backend.module.user.service;

import com.Reffr_Backend.module.user.dto.UserDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserResumeService {

    UserDto.ProfileResponse upload(MultipartFile file);

    void delete();

    String generateAccessUrl(String username);
}
