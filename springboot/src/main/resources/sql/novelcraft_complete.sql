-- =====================================================
-- NovelCraft AI 小说创作平台 - 数据库完整脚本
-- MySQL 8.0+
-- 创建时间: 2026-07-27
-- =====================================================

-- =====================================================
-- 第一部分：数据库初始化
-- =====================================================
CREATE DATABASE IF NOT EXISTS novelcraft
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE novelcraft;

-- =====================================================
-- 第二部分：核心业务表（作品、章节、人物等）
-- =====================================================

-- =====================================================
-- 1. 作品项目
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_project (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    title           VARCHAR(200)    NOT NULL COMMENT '作品名称',
    cover_url       VARCHAR(500)    DEFAULT NULL COMMENT '封面URL',
    description     VARCHAR(2000)   DEFAULT NULL COMMENT '作品简介',
    genre           VARCHAR(50)     DEFAULT NULL COMMENT '作品类型',
    sub_genre       VARCHAR(50)     DEFAULT NULL COMMENT '作品子类型',
    status          VARCHAR(20)     NOT NULL DEFAULT 'draft' COMMENT '状态: draft/ongoing/completed',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '当前总字数',
    chapter_count   INT             NOT NULL DEFAULT 0 COMMENT '当前章节数',
    target_word_count INT           DEFAULT NULL COMMENT '目标字数',
    tags            VARCHAR(500)    DEFAULT NULL COMMENT '标签',
    starting_world  VARCHAR(255)    DEFAULT NULL COMMENT '起始时间',
    planned_completion_date DATE    DEFAULT NULL COMMENT '预定完本时间',
    core_setting   TEXT            DEFAULT NULL COMMENT '核心设定',
    world_settings TEXT            DEFAULT NULL COMMENT '世界观设定',
    characters     TEXT            DEFAULT NULL COMMENT '人物设定',
    characters_formatted TEXT        DEFAULT NULL COMMENT '格式化人物设定(供AI)',
    outlines       TEXT            DEFAULT NULL COMMENT '大纲结构',
    user_id         BIGINT          DEFAULT NULL COMMENT '用户ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_project_status (status),
    INDEX idx_project_deleted (deleted),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品项目';

-- =====================================================
-- 2. 章节
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_chapter (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    title           VARCHAR(200)    NOT NULL COMMENT '章节标题',
    content         LONGTEXT        DEFAULT NULL COMMENT '章节正文',
    status          VARCHAR(20)     NOT NULL DEFAULT 'draft' COMMENT '状态: draft/writing/completed',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '字数',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    version         VARCHAR(20)     DEFAULT 'v1' COMMENT '当前版本号',
    sentinel_status JSON            DEFAULT NULL COMMENT '哨兵状态',
    prev_version_id BIGINT          DEFAULT NULL COMMENT '上一版本ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_chapter_project (project_id),
    INDEX idx_chapter_deleted (deleted),
    INDEX idx_chapter_list_covering (project_id, deleted, sort_order, word_count, title, status),
    INDEX idx_chapter_update_time (project_id, deleted, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节';

-- =====================================================
-- 4. 章节历史版本
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_chapter_version (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    chapter_id      BIGINT          NOT NULL COMMENT '章节ID',
    content         LONGTEXT        DEFAULT NULL COMMENT '版本正文',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '字数',
    version         VARCHAR(20)     DEFAULT NULL COMMENT '版本号',
    change_note     VARCHAR(500)    DEFAULT NULL COMMENT '变更说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_version_chapter (chapter_id),
    INDEX idx_version_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节历史版本';

-- =====================================================
-- 5. 人物
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_character (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    name            VARCHAR(100)    NOT NULL COMMENT '人物名称',
    role            VARCHAR(50)     DEFAULT NULL COMMENT '角色定位: 主角/配角/反派等',
    type            VARCHAR(50)     DEFAULT NULL COMMENT '人物类型',
    age             INT             DEFAULT NULL COMMENT '年龄',
    avatar_color    VARCHAR(20)     DEFAULT NULL COMMENT '头像颜色',
    description     VARCHAR(2000)   DEFAULT NULL COMMENT '人物描述',
    personality     VARCHAR(1000)   DEFAULT NULL COMMENT '性格特征',
    relation        VARCHAR(500)    DEFAULT NULL COMMENT '关系描述',
    arc_start       VARCHAR(1000)   DEFAULT NULL COMMENT '弧光起点',
    arc_end         VARCHAR(1000)   DEFAULT NULL COMMENT '弧光终点',
    arc_progress    INT             DEFAULT 0 COMMENT '弧光进度 0-100',
    combat          INT             DEFAULT NULL COMMENT '战斗力',
    wisdom          INT             DEFAULT NULL COMMENT '智慧',
    emotion         INT             DEFAULT NULL COMMENT '情感',
    charm           INT             DEFAULT NULL COMMENT '魅力',
    last_seen       VARCHAR(500)    DEFAULT NULL COMMENT '最后登场',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_character_project (project_id),
    INDEX idx_character_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人物';

-- =====================================================
-- 6. 大纲
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_outline (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    parent_id       BIGINT          DEFAULT NULL COMMENT '父节点ID',
    act             VARCHAR(20)     DEFAULT 'first_act' COMMENT '所属幕: first_act/second_act/third_act',
    title           VARCHAR(200)    NOT NULL COMMENT '标题',
    description     VARCHAR(2000)   DEFAULT NULL COMMENT '描述',
    type            VARCHAR(50)     DEFAULT NULL COMMENT '类型: volume/chapter/scene',
    node_status     VARCHAR(20)     DEFAULT 'draft' COMMENT '状态: draft/pending/completed',
    chapter_id      BIGINT          DEFAULT NULL COMMENT '关联章节ID',
    is_key_event    TINYINT(1)      DEFAULT 0 COMMENT '是否为核心情节点',
    node_number     INT             DEFAULT 0 COMMENT '幕内序号',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    estimated_words INT             DEFAULT NULL COMMENT '预估字数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_outline_project (project_id),
    INDEX idx_outline_parent (parent_id),
    INDEX idx_outline_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大纲';

-- =====================================================
-- 7. 情节线
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_plot_thread (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    name            VARCHAR(200)    NOT NULL COMMENT '情节线名称',
    type            VARCHAR(50)     DEFAULT NULL COMMENT '类型: main/sub/hidden',
    progress        INT             NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    color           VARCHAR(20)     DEFAULT NULL COMMENT '标记颜色',
    chapters        VARCHAR(1000)   DEFAULT NULL COMMENT '涉及的章节ID列表',
    description     VARCHAR(500)     DEFAULT NULL COMMENT '描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_plot_project (project_id),
    INDEX idx_plot_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情节线';

-- =====================================================
-- 8. 伏笔管理
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_foreshadowing (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id        BIGINT        NOT NULL COMMENT '作品ID',
    name              VARCHAR(200)  NOT NULL COMMENT '伏笔名称',
    description       VARCHAR(1000) DEFAULT NULL COMMENT '伏笔描述',
    chapter_id        BIGINT        NOT NULL COMMENT '埋设章节ID',
    status            VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT '状态: pending/triggered/resolved',
    passed_chapters   INT           NOT NULL DEFAULT 0 COMMENT '已过去章节数',
    severity          VARCHAR(20)   DEFAULT 'normal' COMMENT '重要程度: critical/important/normal',
    resolved_chapter_id BIGINT      DEFAULT NULL COMMENT '回收章节ID',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           INT           NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_fs_project (project_id),
    INDEX idx_fs_chapter (chapter_id),
    INDEX idx_fs_status (status),
    INDEX idx_fs_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='伏笔管理';

-- =====================================================
-- 10. 世界观设定
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_world_setting (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    name            VARCHAR(200)    NOT NULL COMMENT '设定名称',
    category        VARCHAR(50)     NOT NULL COMMENT '分类: geography/history/magic/technology/culture',
    level           INT             NOT NULL DEFAULT 1 COMMENT '层级',
    parent_id       BIGINT          DEFAULT NULL COMMENT '父节点ID',
    content         TEXT            DEFAULT NULL COMMENT '设定内容',
    status          VARCHAR(32)     DEFAULT 'draft' COMMENT '状态: draft/completed/needs_work',
    related_settings JSON           DEFAULT NULL COMMENT '关联设定ID列表（JSON数组）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_ws_project (project_id),
    INDEX idx_ws_parent (parent_id),
    INDEX idx_ws_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界观设定';

-- =====================================================
-- 11. AI配置
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_ai_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    temperature     DOUBLE          DEFAULT 0.7 COMMENT '温度参数',
    top_p           DOUBLE          DEFAULT 0.9 COMMENT 'Top-P 采样',
    max_tokens      INT             DEFAULT 4096 COMMENT '最大Token数',
    style_preset    VARCHAR(50)     DEFAULT NULL COMMENT '风格预设',
    custom_prompt   TEXT            DEFAULT NULL COMMENT '自定义提示词',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_aiconfig_project (project_id),
    INDEX idx_aiconfig_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI配置';

-- =====================================================
-- 12. AI会话记录（合并原 novel_agent_session）
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_ai_session (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    session_id      BIGINT          DEFAULT NULL COMMENT '会话ID（同一次对话的多条消息共享同一session_id）',
    session_type    VARCHAR(20)     NOT NULL DEFAULT 'chat' COMMENT '会话类型: chat/agent',
    role            VARCHAR(20)    NOT NULL COMMENT '角色: user/assistant/system',
    agent           VARCHAR(30)    DEFAULT NULL COMMENT 'Agent类型: editor/character/style/reader',
    content         TEXT            DEFAULT NULL COMMENT '会话内容',
    tokens_used     INT             NOT NULL DEFAULT 0 COMMENT '消耗Token数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_ais_project (project_id),
    INDEX idx_ais_session (session_id),
    INDEX idx_ais_type (session_type),
    INDEX idx_ais_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话记录（含Chat对话与Agent任务）';

-- =====================================================
-- 14. 灵感素材
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_inspiration (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    type            VARCHAR(50)     NOT NULL COMMENT '类型',
    content         TEXT            DEFAULT NULL COMMENT '内容',
    tags            VARCHAR(500)    DEFAULT NULL COMMENT '标签',
    chapter_id      BIGINT          DEFAULT NULL COMMENT '关联章节ID',
    source          VARCHAR(20)     DEFAULT 'manual' COMMENT '来源: manual/ai',
    is_highlight    TINYINT(1)      DEFAULT 0 COMMENT '是否高亮',
    is_used         TINYINT(1)      DEFAULT 0 COMMENT '是否已使用',
    used_time       DATETIME        DEFAULT NULL COMMENT '使用时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_ins_project (project_id),
    INDEX idx_ins_type (type),
    INDEX idx_ins_chapter (chapter_id),
    INDEX idx_ins_used (is_used),
    INDEX idx_ins_highlight (is_highlight),
    INDEX idx_ins_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灵感素材';

-- =====================================================
-- 15. 写作日志
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_writing_log (
    id                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id          BIGINT      NOT NULL COMMENT '作品ID',
    chapter_id          BIGINT      DEFAULT NULL COMMENT '章节ID',
    date                DATE        NOT NULL COMMENT '写作日期',
    word_count          INT         NOT NULL DEFAULT 0 COMMENT '字数',
    writing_duration    INT         DEFAULT 0 COMMENT '写作时长（分钟）',
    create_time         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             INT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_wl_project_date (project_id, date),
    INDEX idx_wl_chapter (chapter_id),
    INDEX idx_wl_deleted (deleted),
    INDEX idx_writing_log_date_covering (project_id, date, deleted, word_count, writing_duration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='写作日志';

-- =====================================================
-- 16. 哨兵告警表
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_sentinel_alert (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    chapter_id      BIGINT          DEFAULT NULL COMMENT '关联章节ID（可为null表示全局告警）',
    type            VARCHAR(50)     NOT NULL COMMENT '告警类型',
    title           VARCHAR(200)   NOT NULL COMMENT '告警标题',
    description     TEXT            DEFAULT NULL COMMENT '告警描述',
    severity        VARCHAR(20)     DEFAULT 'info' COMMENT '严重程度: critical/warning/info',
    resolved        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已处理',
    resolved_time   DATETIME        DEFAULT NULL COMMENT '处理时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_sentinel_project (project_id),
    INDEX idx_sentinel_alert_chapter (chapter_id),
    INDEX idx_sentinel_type (type),
    INDEX idx_sentinel_resolved (resolved),
    INDEX idx_sentinel_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='哨兵告警';

-- =====================================================
-- 第三部分：用户相关表
-- =====================================================

-- =====================================================
-- 17. 用户信息表
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
    role            VARCHAR(20)     NOT NULL DEFAULT 'user' COMMENT '角色: admin-管理员 user-普通用户',
    vip_level       INT             NOT NULL DEFAULT 0 COMMENT 'VIP等级 0-普通 1-白银 2-黄金 3-钻石',
    vip_expire_at   DATETIME        DEFAULT NULL COMMENT 'VIP到期时间，NULL表示未开通',
    vip_purchased_at DATETIME       DEFAULT NULL COMMENT '最近一次VIP购买时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_user_username (username),
    INDEX idx_user_email (email),
    INDEX idx_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息';

-- 已有库升级（VIP 会员体系）：逐条执行，已存在字段会报错，属正常现象可跳过
-- ALTER TABLE novel_user ADD COLUMN vip_level INT NOT NULL DEFAULT 0 COMMENT 'VIP等级 0-普通 1-白银 2-黄金 3-钻石';
-- ALTER TABLE novel_user ADD COLUMN vip_expire_at DATETIME DEFAULT NULL COMMENT 'VIP到期时间，NULL表示未开通';
-- ALTER TABLE novel_user ADD COLUMN vip_purchased_at DATETIME DEFAULT NULL COMMENT '最近一次VIP购买时间';
-- 已有库升级（用户角色字段）：
-- ALTER TABLE novel_user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin-管理员 user-普通用户';
-- UPDATE novel_user SET role='admin' WHERE username='admin';
-- 已有库升级（管理员默认 VIP）：
-- UPDATE novel_user SET vip_level=3, vip_expire_at='2099-12-31 23:59:59', vip_purchased_at=NOW() WHERE username='admin';

-- =====================================================
-- 18. 用户设置表
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
-- 19. 用户统计表
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
-- 第四部分：初始化数据
-- =====================================================

-- =====================================================
-- 初始化默认用户
-- =====================================================
INSERT INTO novel_user (username, password, nickname, email, phone, bio, email_verified, role, vip_level, vip_expire_at, vip_purchased_at)
VALUES ('admin', '$2b$12$lR2oJF.cGkX/7pYK64omhuGYdmdY.1Snr/zqAbPggb3Bo.zzE5bzW', '墨染青衫', 'writer@example.com', '138****8888', '玄幻小说爱好者，笔耕不辍', 0, 'admin', 3, '2099-12-31 23:59:59', NOW());

INSERT INTO novel_user_stats (user_id, total_words, continuous_days, works_count, user_level)
VALUES (1, 127430, 32, 3, 8);

-- =====================================================
-- 第五部分：废弃表清理（已确认不再使用）
-- =====================================================
-- 以下表在代码中未使用，已删除定义
-- - novel_character_relation（人物关系表）
-- - novel_style_profile（风格档案表）

-- =====================================================
-- 第六部分：数据清理脚本
-- =====================================================

-- =====================================================
-- 清理旧的、重复的、已处理的占位通知（7月21日的测试数据）
-- 执行前请确保已备份数据
-- =====================================================
-- DELETE FROM novel_sentinel_alert
-- WHERE create_time LIKE '2026-07-21%'
--   AND title = '自动巡查报告提示'
--   AND resolved = 1
--   AND deleted = 0;