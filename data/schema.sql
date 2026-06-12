CREATE TABLE IF NOT EXISTS `t_user` (
  `id` varchar(36) NOT NULL COMMENT '主键ID',
  `phone` varchar(11) NOT NULL COMMENT '手机号',
--  `password` varchar(255) DEFAULT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
