package org.example.travel_project1.repository;


import java.util.Optional;

import org.example.travel_project1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 根据用户名查找用户（登录时用）
    Optional<User> findByUsername(String username);

    // 判断用户名是否已存在（注册时检查）
    boolean existsByUsername(String username);
}
