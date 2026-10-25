package org.example.travel_project1.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;
    private String phone;

    // 头像以 base64 data URL 存储（前端会先压缩到 512px），varchar(255) 装不下
    @Column(columnDefinition = "MEDIUMTEXT")
    private String avatar;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
