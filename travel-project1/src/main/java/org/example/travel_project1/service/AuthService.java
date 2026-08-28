package org.example.travel_project1.service;


import lombok.RequiredArgsConstructor;
import org.example.travel_project1.common.JwtUtils;
import org.example.travel_project1.dto.LoginRequestDTO;
import org.example.travel_project1.dto.RegisterRequestDTO;
import org.example.travel_project1.entity.User;
import org.example.travel_project1.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 注册
     */
    public Map<String, Object> register(RegisterRequestDTO dto){
        // 1. 检查用户名是否已存在
        if (userRepository.existsByUsername(dto.getUsername())){
            throw new RuntimeException("用户名已被占用");
        }

        // 2. 构造用户实体，密码加密后存储
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 3. 保存到数据库
        userRepository.save(user);

        // 4. 生成 token
        String token = jwtUtils.generateToken(user.getId());

        // 5. 返回 token + 用户信息
        return buildResult(token,user);
    }

    /**
     * 登录
     */
    public Map<String, Object> login(LoginRequestDTO dto){
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 2. 校验密码（BCrypt 比对）
        if (!passwordEncoder.matches(dto.getPassword(),user.getPassword())){
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 生成 token
        String token = jwtUtils.generateToken(user.getId());

        // 4. 返回 token + 用户信息
        return buildResult(token, user);

    }

    /**
     * 根据 userId 获取用户信息（给 UserController 用）
     */
    public User getUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

    }

    /**
     * 组装返回结果：token + 用户信息（不含密码）
     */
    private Map<String,Object> buildResult(String token, User user){
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("phone", user.getPhone());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    /**
     * 更新用户信息
     */
    public void updateUser(User user){
        userRepository.save(user);
    }


}
