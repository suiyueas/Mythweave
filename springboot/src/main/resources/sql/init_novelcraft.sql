-- =====================================================
-- NovelCraft AI 小说创作平台 - 数据库初始化脚本
-- 所有主键均使用 BIGINT AUTO_INCREMENT（数据库自增）
-- 彻底禁用雪花算法
-- MySQL 8.0+
-- =====================================================

CREATE DATABASE IF NOT EXISTS novelcraft
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE novelcraft;

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
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_project_status (status),
    INDEX idx_project_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品项目';

-- =====================================================
-- 2. 分卷
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_volume (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    title           VARCHAR(200)    NOT NULL COMMENT '分卷标题',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '分卷描述',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_volume_project (project_id),
    INDEX idx_volume_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分卷';

-- =====================================================
-- 3. 章节
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_chapter (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    volume_id       BIGINT          DEFAULT NULL COMMENT '分卷ID',
    title           VARCHAR(200)    NOT NULL COMMENT '章节标题',
    content         LONGTEXT        DEFAULT NULL COMMENT '章节正文',
    status          VARCHAR(20)     NOT NULL DEFAULT 'draft' COMMENT '状态: draft/writing/completed',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '字数',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    version         VARCHAR(20)     DEFAULT 'v1' COMMENT '当前版本号',
    prev_version_id BIGINT          DEFAULT NULL COMMENT '上一版本ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_chapter_project (project_id),
    INDEX idx_chapter_volume (volume_id),
    INDEX idx_chapter_deleted (deleted)
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
-- 6. 人物关系
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_character_relation (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    character_id_a  BIGINT          NOT NULL COMMENT '人物A的ID',
    character_id_b  BIGINT          NOT NULL COMMENT '人物B的ID',
    relation_type   VARCHAR(50)     NOT NULL COMMENT '关系类型',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '关系描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_cr_project (project_id),
    INDEX idx_cr_chara (character_id_a),
    INDEX idx_cr_charb (character_id_b),
    INDEX idx_cr_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人物关系';

-- =====================================================
-- 7. 大纲
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_outline (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    parent_id       BIGINT          DEFAULT NULL COMMENT '父节点ID',
    title           VARCHAR(200)    NOT NULL COMMENT '标题',
    description     VARCHAR(2000)   DEFAULT NULL COMMENT '描述',
    type            VARCHAR(50)     DEFAULT NULL COMMENT '类型: volume/chapter/scene',
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
-- 8. 情节线
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_plot_thread (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    name            VARCHAR(200)    NOT NULL COMMENT '情节线名称',
    type            VARCHAR(50)     DEFAULT NULL COMMENT '类型: main/sub/hidden',
    progress        INT             NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    color           VARCHAR(20)     DEFAULT NULL COMMENT '标记颜色',
    chapters        VARCHAR(1000)   DEFAULT NULL COMMENT '涉及的章节ID列表',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_plot_project (project_id),
    INDEX idx_plot_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情节线';

-- =====================================================
-- 9. 伏笔管理
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
-- 10. 情节知识图谱
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_plot_knowledge_graph (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    node_type       VARCHAR(50)     NOT NULL COMMENT '节点类型',
    source_id       BIGINT          NOT NULL COMMENT '源节点ID',
    target_id       BIGINT          NOT NULL COMMENT '目标节点ID',
    relation_label  VARCHAR(200)    NOT NULL COMMENT '关系标签',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_pkg_project (project_id),
    INDEX idx_pkg_source (source_id),
    INDEX idx_pkg_target (target_id),
    INDEX idx_pkg_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情节知识图谱';

-- =====================================================
-- 11. 世界观设定
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_world_setting (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    name            VARCHAR(200)    NOT NULL COMMENT '设定名称',
    category        VARCHAR(50)     NOT NULL COMMENT '分类: geography/history/magic/technology/culture',
    level           INT             NOT NULL DEFAULT 1 COMMENT '层级',
    parent_id       BIGINT          DEFAULT NULL COMMENT '父节点ID',
    content         TEXT            DEFAULT NULL COMMENT '设定内容',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_ws_project (project_id),
    INDEX idx_ws_parent (parent_id),
    INDEX idx_ws_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界观设定';

-- =====================================================
-- 12. AI配置
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
-- 13. AI会话记录（合并原 novel_agent_session）
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_ai_session (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    session_id      BIGINT          DEFAULT NULL COMMENT '会话ID（同一次对话的多条消息共享同一session_id）',
    session_type    VARCHAR(20)     NOT NULL DEFAULT 'chat' COMMENT '会话类型: chat/agent',
    role            VARCHAR(20)     NOT NULL COMMENT '角色: user/assistant/system',
    agent           VARCHAR(30)     DEFAULT NULL COMMENT 'Agent类型: editor/character/style/reader',
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
-- 14. 上下文快照
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_context_snapshot (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    chapter_id      BIGINT          DEFAULT NULL COMMENT '章节ID',
    context_type    VARCHAR(50)     NOT NULL COMMENT '上下文类型',
    query_text      VARCHAR(1000)   DEFAULT NULL COMMENT '查询文本',
    assembled_prompt TEXT           DEFAULT NULL COMMENT '组装后的Prompt',
    tokens_used     INT             NOT NULL DEFAULT 0 COMMENT '消耗Token数',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_cs_project (project_id),
    INDEX idx_cs_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上下文快照';

-- =====================================================
-- 15. 灵感素材
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_inspiration (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    type            VARCHAR(50)     NOT NULL COMMENT '类型',
    content         TEXT            DEFAULT NULL COMMENT '内容',
    tags            VARCHAR(500)    DEFAULT NULL COMMENT '标签',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_ins_project (project_id),
    INDEX idx_ins_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灵感素材';

-- =====================================================
-- 16. 哨兵预警
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_sentinel_alert (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    type            VARCHAR(50)     NOT NULL COMMENT '预警类型',
    title           VARCHAR(200)    NOT NULL COMMENT '预警标题',
    description     VARCHAR(1000)   DEFAULT NULL COMMENT '预警描述',
    severity        VARCHAR(20)     NOT NULL DEFAULT 'info' COMMENT '严重程度: critical/warning/info',
    suggestion      VARCHAR(1000)   DEFAULT NULL COMMENT '修复建议',
    resolved        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已解决',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_sa_project (project_id),
    INDEX idx_sa_resolved (resolved),
    INDEX idx_sa_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='哨兵预警';

-- =====================================================
-- 17. 哨兵巡查执行日志
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_sentinel_check_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    task_id         VARCHAR(64)     NOT NULL COMMENT '巡查任务ID',
    dimension       VARCHAR(20)     NOT NULL COMMENT '维度: foreshadowing/logic/character/rhythm',
    scan_type       VARCHAR(20)     DEFAULT 'full' COMMENT '类型: full/incremental',
    total_chunks    INT             DEFAULT 0 COMMENT '总分片数',
    processed_chunks INT            DEFAULT 0 COMMENT '已处理分片数',
    alerts_found    INT             DEFAULT 0 COMMENT '发现告警数',
    status          VARCHAR(20)     DEFAULT 'running' COMMENT '状态: running/completed/failed',
    started_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at    DATETIME        DEFAULT NULL COMMENT '完成时间',
    error_message   TEXT            DEFAULT NULL COMMENT '错误信息',
    duration_ms     INT             DEFAULT 0 COMMENT '执行耗时(毫秒)',
    deleted         INT             DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_scl_project (project_id),
    INDEX idx_scl_task (task_id),
    INDEX idx_scl_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='哨兵巡查执行日志';

-- =====================================================
-- 18. 写作日志
-- =====================================================
CREATE TABLE IF NOT EXISTS novel_writing_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    project_id      BIGINT          NOT NULL COMMENT '作品ID',
    chapter_id      BIGINT          DEFAULT NULL COMMENT '章节ID',
    date            DATE            NOT NULL COMMENT '写作日期',
    word_count      INT             NOT NULL DEFAULT 0 COMMENT '当日写作字数',
    writing_duration INT            NOT NULL DEFAULT 0 COMMENT '写作时长(分钟)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         INT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-已删除',
    PRIMARY KEY (id),
    INDEX idx_wl_project (project_id),
    INDEX idx_wl_date (date),
    INDEX idx_wl_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='写作日志';

-- =====================================================
-- 初始化完成
-- =====================================================

-- 增量更新：为 novel_ai_session 添加 session_id 和 agent 字段
ALTER TABLE novel_ai_session ADD COLUMN IF NOT EXISTS session_id BIGINT DEFAULT NULL COMMENT '会话ID' AFTER project_id;
ALTER TABLE novel_ai_session ADD COLUMN IF NOT EXISTS agent VARCHAR(30) DEFAULT NULL COMMENT 'Agent类型' AFTER role;
ALTER TABLE novel_ai_session ADD INDEX IF NOT EXISTS idx_ais_session (session_id);