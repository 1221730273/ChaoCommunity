package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import jakarta.validation.Valid;

public interface UserService {

    void updateProfile(@Valid UserProfileDTO dto);

    void updateAvatar(@Valid CoverUpdateDTO dto);
}
