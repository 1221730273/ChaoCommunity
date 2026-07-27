package com.ljc.chaocommunity.service.Impl;

import com.ljc.chaocommunity.pojo.dto.LoginDTO;
import com.ljc.chaocommunity.pojo.vo.LoginVO;
import com.ljc.chaocommunity.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginVO login(LoginDTO dto) {
        //进行认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        //如果认证没通过给对应的提示
        if (Objects.isNull(authenticate)){
            throw new RuntimeException("登录失败");
        }
        //认证通过生成一个uuid(去掉-)作为key，将用户信息存入redis
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = "login:" + token ;
        redisTemplate.opsForValue().set(key,authenticate.getPrincipal(),7, TimeUnit.DAYS);
        //返回给前端
        return new LoginVO(token);

    }
}
