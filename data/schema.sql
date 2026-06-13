-- ================================================================
-- Live2D Language Coach — 数据库建表脚本
-- ================================================================

-- 建库
CREATE DATABASE IF NOT EXISTS live2d_language_coach DEFAULT CHARSET utf8mb4;
GRANT ALL ON live2d_language_coach.* TO 'init_pro'@'%';
FLUSH PRIVILEGES;

USE live2d_language_coach;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id VARCHAR(64) NOT NULL COMMENT '主键UUID',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password VARCHAR(255) DEFAULT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS t_chat_session (
    id VARCHAR(64) NOT NULL COMMENT '主键UUID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    scene VARCHAR(50) DEFAULT 'default' COMMENT '场景',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度',
    accent VARCHAR(20) DEFAULT 'us' COMMENT '口音',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    total_grade VARCHAR(5) DEFAULT NULL COMMENT '总评等级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id VARCHAR(64) NOT NULL COMMENT '主键UUID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色(user/assistant)',
    content TEXT COMMENT '消息内容',
    turn_id VARCHAR(64) DEFAULT NULL COMMENT '轮次ID',
    pron_accuracy DOUBLE DEFAULT NULL COMMENT '发音准确度',
    pron_fluency DOUBLE DEFAULT NULL COMMENT '流利度',
    pron_completion DOUBLE DEFAULT NULL COMMENT '完整度',
    accuracy_grade VARCHAR(5) DEFAULT NULL COMMENT '准确度等级',
    fluency_grade VARCHAR(5) DEFAULT NULL COMMENT '流利度等级',
    completion_grade VARCHAR(5) DEFAULT NULL COMMENT '完整度等级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';
