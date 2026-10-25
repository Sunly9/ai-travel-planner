-- ============================================================================
--  AI 旅游规划助手 —— 数据库初始化脚本
--  目标库：travel        适用：MySQL 8.0+
--  字符集：utf8mb4       存储引擎：InnoDB
--
--  导入方式（任选一种）：
--    命令行： mysql -uroot -p --default-character-set=utf8mb4 < travel_init.sql
--    Navicat：右键连接 → 运行 SQL 文件 → 选中本文件 → 开始
--    IDEA / DataGrip：打开本文件 → 选中全部 → 执行
--
--  说明：
--    1. 可重复执行。建库/建表均为 IF NOT EXISTS，不会清空或覆盖已有数据。
--    2. 表结构与后端 JPA 实体一一对应，列名遵循 Spring Boot 默认的
--       「驼峰转下划线」命名策略（createTime → create_time）。
--    3. 后端 application.yml 为 ddl-auto: update，正常情况下 Hibernate
--       启动时检查表已存在且列一致，不会再做任何结构变更。
--    4. 敏感字段 password 存 BCrypt 哈希（60 字符），不是明文。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 0. 建库（已存在则跳过）
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `travel`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `travel`;

-- ---------------------------------------------------------------------------
-- 1. 用户表  ← 实体 User
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(255) NOT NULL                COMMENT '登录名，全局唯一',
  `password`    VARCHAR(255) NOT NULL                COMMENT 'BCrypt 加密后的密码',
  `nickname`    VARCHAR(255) DEFAULT NULL            COMMENT '昵称',
  `phone`       VARCHAR(255) DEFAULT NULL            COMMENT '手机号',
  `avatar`      MEDIUMTEXT   DEFAULT NULL            COMMENT '头像 base64 data URL（前端压缩到 512px 后上传）',
  `create_time` DATETIME(6)  DEFAULT NULL            COMMENT '注册时间',
  `update_time` DATETIME(6)  DEFAULT NULL            COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------------------------------------------------------
-- 2. 行程表  ← 实体 TravelPlan
--    plan_data 存 AI 生成的整份行程 JSON（TEXT，不建独立子表）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `travel_plans` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '所属用户 id',
  `city`        VARCHAR(255) NOT NULL                COMMENT '目的地城市',
  `days`        INT          NOT NULL                COMMENT '行程天数',
  `budget`      DOUBLE       DEFAULT NULL            COMMENT '总预算（元）',
  `plan_data`   TEXT                                 COMMENT '行程 JSON 原文',
  `create_time` DATETIME(6)  DEFAULT NULL            COMMENT '创建时间',
  `update_time` DATETIME(6)  DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plan_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户保存的行程';

-- ---------------------------------------------------------------------------
-- 3. 收藏表  ← 实体 Favorite
--    同一用户对同一景点只能收藏一次（唯一约束 user_id + spot_name）
--    注意：这个唯一键名是 Hibernate 自动生成的固定哈希名，刻意照抄。
--          若换成自定义名字，ddl-auto: update 启动时会再补一个同列的唯一索引，
--          库里就会出现两个一模一样的唯一键。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `favorites` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '所属用户 id',
  `spot_name`   VARCHAR(255) NOT NULL                COMMENT '景点名称',
  `spot_data`   TEXT         DEFAULT NULL            COMMENT '景点详情 JSON（含 description，可能超过 255 字符）',
  `create_time` DATETIME(6)  DEFAULT NULL            COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9arli8yb2bu79753t6d2t48hc` (`user_id`, `spot_name`),
  KEY `idx_fav_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点收藏';

-- ---------------------------------------------------------------------------
-- 4. AI 对话表  ← 实体 ChatMessage
--    role: user=用户提问 / assistant=AI 回答
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '所属用户 id',
  `role`        VARCHAR(20)  NOT NULL                COMMENT 'user / assistant',
  `content`     TEXT                                 COMMENT '消息内容',
  `create_time` DATETIME(6)  DEFAULT NULL            COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史';

-- ---------------------------------------------------------------------------
-- 5. 地理编码缓存表  ← 实体 GeoCache
--    地名 + 城市 → 经纬度。高德按调用次数计费，重复行程不必反复解析。
--    唯一键名同样是 Hibernate 生成的固定哈希名，不要改。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `geo_cache` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `address`           VARCHAR(255) NOT NULL                COMMENT '地名（景点名/城市名）',
  `city`              VARCHAR(255) NOT NULL                COMMENT '城市限定（没传存空串）',
  `latitude`          DOUBLE       DEFAULT NULL            COMMENT '纬度 GCJ-02',
  `longitude`         DOUBLE       DEFAULT NULL            COMMENT '经度 GCJ-02',
  `formatted_address` VARCHAR(255) DEFAULT NULL            COMMENT '高德返回的标准地址',
  `province`          VARCHAR(255) DEFAULT NULL            COMMENT '省份',
  `updated_at`        DATETIME(6)  DEFAULT NULL            COMMENT '缓存时间（默认 30 天过期）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5tfk2lbd08wim6d2sq8adrkd2` (`address`, `city`),
  KEY `idx_geo_addr` (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高德地理编码缓存';

-- ---------------------------------------------------------------------------
-- 6. 导入结果自检
-- ---------------------------------------------------------------------------
SELECT table_name    AS `表名`,
       table_rows    AS `大致行数`,
       engine        AS `引擎`,
       table_collation AS `排序规则`
FROM information_schema.tables
WHERE table_schema = 'travel'
ORDER BY table_name;
