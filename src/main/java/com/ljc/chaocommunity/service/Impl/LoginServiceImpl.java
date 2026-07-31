package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.dto.LoginDTO;
import com.ljc.chaocommunity.pojo.dto.RegisterDTO;
import com.ljc.chaocommunity.pojo.entity.LoginInfo;
import com.ljc.chaocommunity.pojo.entity.LoginUser;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.vo.LoginVO;
import com.ljc.chaocommunity.service.LoginService;
import com.ljc.chaocommunity.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HttpServletRequest request;

    @Override
    public LoginVO login(LoginDTO dto) {
        //进行认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        //如果认证没通过给对应的提示
        if (Objects.isNull(authenticate)){
            throw new RuntimeException("登录失败");
        }

        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        User u = loginUser.getUser();

        //先检查redis中是否已存在该用户 踢掉旧登录
        String oldToken = (String) redisTemplate.opsForValue().get("auth:user:" + u.getId() + ":token");
        if (oldToken != null){
            redisTemplate.delete("auth:token:" + oldToken);
        }

        //封装登录信息
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setId(u.getId());
        loginInfo.setUsername(u.getUsername());
        loginInfo.setNickname(u.getNickname());
        loginInfo.setAvatar(u.getAvatar());
        loginInfo.setRole(u.getRole());
        loginInfo.setStatus(u.getStatus());

        //生成新token
        String token = UUID.randomUUID().toString().replace("-", "");
        // auth:token:{token} → 存用户信息
        redisTemplate.opsForValue().set("auth:token:" + token, loginInfo, 7, TimeUnit.DAYS);
        // auth:user:{userId}:token → 存token（用于踢旧登录）
        redisTemplate.opsForValue().set("auth:user:" + u.getId() + ":token", token, 7, TimeUnit.DAYS);

        //返回给前端
        return new LoginVO(token);

    }

    @Override
    public void register(RegisterDTO dto) {
        // 1. 校验两次密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }

        // 2. 校验用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 3. 保存用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 昵称默认用用户名
        user.setNickname(dto.getUsername());
        user.setEmail(dto.getEmail());
        userMapper.insert(user);
    }


    @Override
    public void logout() {
        // 获取token
        String token = request.getHeader("token");

        if (!StringUtils.hasText(token)) {
            return;
        }

        // 查询登录信息
        LoginInfo loginInfo = (LoginInfo) redisTemplate.opsForValue()
                .get("auth:token:" + token);

        // 删除 auth:user:{userId}:token
        if (loginInfo != null) {
            redisTemplate.delete("auth:user:" + loginInfo.getId() + ":token");
        }

        // 删除 auth:token:{token}
        redisTemplate.delete("auth:token:" + token);

    }
}
