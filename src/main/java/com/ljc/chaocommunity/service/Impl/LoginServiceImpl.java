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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        //先检查redis中是否已存在该用户 查询id:token
        String oldToken = (String) redisTemplate.opsForValue().get("user:login:" + u.getId());
        //如果有把旧的token拿出来 删掉token:info信息
        if (oldToken != null){
            redisTemplate.delete("login:" + oldToken);
        }

        //封装登录信息
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setId(u.getId());
        loginInfo.setUsername(u.getUsername());
        loginInfo.setNickname(u.getNickname());
        loginInfo.setAvatar(u.getAvatar());
        loginInfo.setRole(u.getRole());
        loginInfo.setStatus(u.getStatus());

        //生成一个uuid(去掉-)作为新key(token)
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = "login:" + token ;

        //创建token:info
        redisTemplate.opsForValue().set(key, loginInfo,7, TimeUnit.DAYS);
        //创建id:token(覆盖掉旧的id:token)
        redisTemplate.opsForValue().set("user:login:" + u.getId(), token,7, TimeUnit.DAYS);

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
        // 昵称不传则默认用用户名
        user.setNickname(dto.getNickname() != null && !dto.getNickname().isBlank()
                ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        userMapper.insert(user);
    }
}
