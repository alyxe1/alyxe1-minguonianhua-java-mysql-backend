-- 民国年华小程序数据库建表脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS nianhua DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nianhua;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '微信OpenID',
    `unionid` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '微信UnionID',
    `nickname` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '用户昵称',
    `avatar_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
    `phone` VARCHAR(11) NOT NULL DEFAULT '' COMMENT '手机号',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user-普通用户，staff-工作人员',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_openid` (`openid`),
    UNIQUE KEY `idx_unionid` (`unionid`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 主题表
CREATE TABLE IF NOT EXISTS `themes` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主题ID',
    `title` VARCHAR(100) NOT NULL COMMENT '主题名称',
    `subtitle` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '主题副标题',
    `description` TEXT NOT NULL COMMENT '主题描述',
    `cover_image` VARCHAR(255) NOT NULL COMMENT '封面图片',
    `address` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '地址',
    `banner_images` JSON NOT NULL COMMENT '轮播图片JSON数组',
    `price` BIGINT NOT NULL DEFAULT '0' COMMENT '价格（分）',
    `categories` JSON NOT NULL COMMENT '分类标签JSON数组',
    `sold_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '已售数量',
    `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：1-上架，0-下架',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题表';

-- 场次表
CREATE TABLE IF NOT EXISTS `sessions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场次ID',
    `theme_id` BIGINT UNSIGNED NOT NULL COMMENT '主题ID',
    `session_type` VARCHAR(50) NOT NULL COMMENT '场次类型',
    `session_name` VARCHAR(100) NOT NULL COMMENT '场次名称',
    `max_capacity` INT UNSIGNED NOT NULL COMMENT '最大容量',
    `total_seats` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '总座位数',
    `total_makeup` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '化妆总库存',
    `total_photography` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '摄影总库存',
    `price` BIGINT NOT NULL DEFAULT '0' COMMENT '价格（分）',
    `start_time` TIME NOT NULL DEFAULT '18:00:00' COMMENT '开始时间',
    `end_time` TIME NOT NULL DEFAULT '21:00:00' COMMENT '结束时间',
    `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：1-可用，0-不可用',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_theme_id` (`theme_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次表';

-- 每日场次表
CREATE TABLE IF NOT EXISTS `daily_sessions` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '每日场次ID',
    `session_id` BIGINT UNSIGNED NOT NULL COMMENT '场次模板ID',
    `date` DATE NOT NULL COMMENT '具体日期',
    `available_seats` INT UNSIGNED NOT NULL COMMENT '可用座位数',
    `makeup_stock` INT UNSIGNED NOT NULL COMMENT '化妆库存',
    `photography_stock` INT UNSIGNED NOT NULL COMMENT '摄影库存',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_session_date` (`session_id`, `date`),
    KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日场次表';

-- 座位表
CREATE TABLE IF NOT EXISTS `seats` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '座位ID',
    `session_template_id` BIGINT UNSIGNED NOT NULL COMMENT '场次模板ID',
    `seat_id` VARCHAR(50) NOT NULL COMMENT '座位编号',
    `seat_name` VARCHAR(50) NOT NULL COMMENT '座位名称',
    `seat_type` VARCHAR(20) NOT NULL DEFAULT 'front' COMMENT '座位类型：front-前排，middle-中排，back-后排',
    `status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '状态：0-可用，1-已锁定，2-已预订',
    `price` BIGINT NOT NULL DEFAULT '0' COMMENT '价格（分）',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_session_template_seat` (`session_template_id`, `seat_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- 预订表
CREATE TABLE IF NOT EXISTS `bookings` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预订ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `theme_id` BIGINT UNSIGNED NOT NULL COMMENT '主题ID',
    `daily_session_id` BIGINT UNSIGNED NOT NULL COMMENT '每日场次ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `total_amount` BIGINT NOT NULL COMMENT '总金额（分）',
    `seat_count` INT UNSIGNED NOT NULL COMMENT '座位数量',
    `booking_date` DATE NOT NULL COMMENT '预订日期',
    `status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '状态：0-待支付，1-已支付，2-已取消，3-已完成',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_daily_session_id` (`daily_session_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预订表';

-- 预订座位关联表
CREATE TABLE IF NOT EXISTS `booking_seats` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `booking_id` BIGINT UNSIGNED NOT NULL COMMENT '预订ID',
    `seat_id` BIGINT UNSIGNED NOT NULL COMMENT '座位ID',
    `seat_name` VARCHAR(50) NOT NULL COMMENT '座位名称',
    `price` BIGINT NOT NULL DEFAULT '0' COMMENT '价格（分）',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_booking_id` (`booking_id`),
    KEY `idx_seat_id` (`seat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预订座位关联表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `booking_id` BIGINT UNSIGNED NOT NULL COMMENT '预订ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `total_amount` BIGINT NOT NULL COMMENT '总金额（分）',
    `pay_amount` BIGINT NOT NULL COMMENT '支付金额（分）',
    `status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '状态：0-待支付，1-已支付，2-支付失败，3-已退款',
    `payment_method` VARCHAR(20) NOT NULL DEFAULT '' COMMENT '支付方式：wechat-微信支付',
    `payment_time` DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '支付时间',
    `transaction_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '微信支付交易号',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_booking_id` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 支付记录表
CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `transaction_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '微信支付交易号',
    `amount` BIGINT NOT NULL COMMENT '支付金额（分）',
    `status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '状态：0-支付中，1-支付成功，2-支付失败',
    `payment_type` VARCHAR(20) NOT NULL DEFAULT 'wechat' COMMENT '支付类型：wechat-微信支付',
    `pay_url` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '支付链接',
    `notify_result` JSON NOT NULL COMMENT '支付回调结果',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- 核销码表
CREATE TABLE IF NOT EXISTS `verification_codes` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '核销码ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `code` VARCHAR(20) NOT NULL COMMENT '核销码',
    `qr_code_url` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '二维码URL',
    `status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '状态：0-未使用，1-已使用，2-已过期',
    `expiry_time` TIMESTAMP NOT NULL COMMENT '过期时间',
    `verified_at` DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '核销时间',
    `admin_id` BIGINT UNSIGNED NOT NULL DEFAULT '0' COMMENT '核销管理员ID',
    `remarks` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '核销备注',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_code` (`code`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销码表';

-- 主题图片表
CREATE TABLE IF NOT EXISTS `theme_images` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `theme_id` BIGINT UNSIGNED NOT NULL COMMENT '主题ID',
    `image_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '排序',
    `image_type` VARCHAR(20) NOT NULL DEFAULT 'banner' COMMENT '图片类型：banner-轮播图，detail-详情图',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_theme_id` (`theme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主题图片表';

-- 商品表
CREATE TABLE IF NOT EXISTS `goods` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `sub_title` VARCHAR(200) NOT NULL DEFAULT '' COMMENT '商品副标题',
    `description` TEXT NOT NULL COMMENT '商品描述',
    `price` BIGINT NOT NULL DEFAULT '0' COMMENT '价格（分）',
    `image_url` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品图片',
    `category` VARCHAR(50) NOT NULL DEFAULT 'photos' COMMENT '商品分类：photos-写真/摄影，makeup-化妆，seat_package-座位商品包，sets-套餐',
    `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：1-上架，0-下架',
    `tag` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '标签',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 预订商品关联表
CREATE TABLE IF NOT EXISTS `booking_goods` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `booking_id` BIGINT UNSIGNED NOT NULL COMMENT '预订ID',
    `goods_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `quantity` INT UNSIGNED NOT NULL DEFAULT '1' COMMENT '数量',
    `price` BIGINT NOT NULL COMMENT '价格（分）',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_booking_id` (`booking_id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预订商品关联表';

-- 优雅退出提示（5分钟后会关闭当前会话）
-- 开发员 xxx 中枢系统启动源码
