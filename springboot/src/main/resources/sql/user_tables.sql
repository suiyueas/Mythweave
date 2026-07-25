-- =====================================================
-- 用户信息与设置表
-- MySQL 8.0+
-- =====================================================

USE novelcraft;

-- =====================================================
-- 1. 用户信息表
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password        VARCHAR(100)    NOT NULL COMMENT '密码（加密）',
    nickname        VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    bio             VARCHAR(500)    DEFAULT NULL COMMENT '个人简介',
    avatar          VARCHAR(500)    DEFAULT NULL COMMENT '头像URL',
    email_verified  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_user_username (username),
    INDEX idx_user_email (email),
    INDEX idx_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息';

-- =====================================================
-- 2. 用户设置表
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_user_settings (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    setting_key     VARCHAR(50)     NOT NULL COMMENT '设置键',
    setting_value   TEXT            DEFAULT NULL COMMENT '设置值',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_us_user_key (user_id, setting_key),
    INDEX idx_us_user (user_id),
    INDEX idx_us_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设置';

-- =====================================================
-- 3. 用户统计表
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_user_stats (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    total_words     INT             NOT NULL DEFAULT 0 COMMENT '总字数',
    continuous_days INT             NOT NULL DEFAULT 0 COMMENT '连续写作天数',
    works_count     INT             NOT NULL DEFAULT 0 COMMENT '作品数量',
    user_level      INT             NOT NULL DEFAULT 1 COMMENT '用户等级',
    last_write_date DATE            DEFAULT NULL COMMENT '最后写作日期',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_stats_user (user_id),
    INDEX idx_stats_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计';

-- =====================================================
-- 初始化默认用户
-- =====================================================
INSERT INTO novel_user (username, password, nickname, email, phone, bio, email_verified)
VALUES ('admin', '$2b$12$lR2oJF.cGkX/7pYK64omhuGYdmdY.1Snr/zqAbPggb3Bo.zzE5bzW', '墨染青衫', 'writer@example.com', '138****8888', '玄幻小说爱好者，笔耕不辍', 0);

INSERT INTO novel_user_stats (user_id, total_words, continuous_days, works_count, user_level)
VALUES (1, 127430, 32, 3, 8);
