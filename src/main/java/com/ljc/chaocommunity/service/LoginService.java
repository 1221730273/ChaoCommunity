package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.LoginDTO;
import com.ljc.chaocommunity.pojo.dto.RegisterDTO;
import com.ljc.chaocommunity.pojo.vo.LoginVO;
import jakarta.validation.Valid;

public interface LoginService {
    LoginVO login(@Valid LoginDTO dto);

    void register(@Valid RegisterDTO dto);
}
