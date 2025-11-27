# 民国年华小程序数据库设计文档

## 概述

本数据库为民国年华晚宴预订小程序设计，采用MySQL 8.0，支持用户管理、主题展示、场次预订、支付、核销等完整业务流程。数据库设计遵循第三范式（3NF），确保数据一致性、可扩展性和性能优化。

## 数据库设计原则

1. **数据完整性**：主键、外键、唯一键约束确保数据一致性
2. **逻辑删除**：采用`is_deleted`软删除机制，保留历史数据
3. **时间戳管理**：所有表统一使用`created_at`和`updated_at`记录时间
4. **性能优化**：合理使用索引，避免JOIN操作性能瓶颈
5. **扩展性**：预留JSON字段支持灵活的业务扩展

---

## 表结构说明

### 1. users（用户表）

**描述**：存储小程序用户信息，通过微信授权登录

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键，用户唯一标识 |
| openid | VARCHAR(32) | 微信OpenID，唯一索引 |
| unionid | VARCHAR(32) | 微信UnionID，唯一索引 |
| nickname | VARCHAR(50) | 用户昵称 |
| avatar_url | VARCHAR(255) | 头像URL |
| phone | VARCHAR(11) | 手机号 |
| role | VARCHAR(20) | 角色：user-普通用户，staff-工作人员 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |
| is_deleted | TINYINT(1) | 逻辑删除标识 |

**重要特性**：
- OpenID和UnionID唯一索引，确保同一微信用户唯一
- 支持多角色：普通用户预订，工作人员核销
- 手机号可用于后续营销触达

**关联关系**：
- 一对多关联`bookings`（一个用户多个预订）
- 一对多关联`orders`（一个用户多个订单）

---

### 2. themes（主题表）

**描述**：晚宴主题信息，包含标题、描述、价格等核心信息

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| title | VARCHAR(100) | 主题名称（如"民国风华夜"） |
| subtitle | VARCHAR(200) | 副标题 |
| description | TEXT | 主题详细描述 |
| cover_image | VARCHAR(255) | 封面图片URL |
| banner_images | JSON | 轮播图片JSON数组，灵活存储多图 |
| price | BIGINT | 基础价格（分） |
| categories | JSON | 分类标签，如["民国风", "高端"] |
| sold_count | INT UNSIGNED | 已售数量，用于显示热度 |
| status | TINYINT(1) | 上下架状态 |

**重要特性**：
- `banner_images`使用JSON类型，支持灵活的轮播图管理
- `categories`标签便于前端筛选和推荐
- `sold_count`实时统计，营造抢购氛围

**关联关系**：
- 一对多关联`sessions`（一个主题多个场次）
- 一对多关联`bookings`（一个主题多个预订）
- 一对多关联`theme_images`（一个主题多张详情图）

---

### 3. sessions（场次表）

**描述**：主题的场次信息，定义具体时间和容量

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| theme_id | BIGINT UNSIGNED | 外键，关联themes表 |
| session_type | VARCHAR(50) | 场次类型（如"晚餐场"、"夜场"） |
| session_name | VARCHAR(100) | 场次名称 |
| max_capacity | INT UNSIGNED | 最大容量 |
| available_seats | INT UNSIGNED | 可用座位数 |
| price | BIGINT | 场次价格（可覆盖主题基础价） |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |

**业务规则**：
- `max_capacity`和`available_seats`实时控制可售数量
- 当`available_seats`为0时，前端显示"已售罄"
- `price`字段允许同主题不同场次价格差异化

**关联关系**：
- 多对一关联`themes`（多个场次属于一个主题）
- 一对多关联`seats`（一个场次多个座位）
- 一对多关联`bookings`（一个场次多个预订）

---

### 4. seats（座位表）

**描述**：场次座位明细，精确到每个座位的状态

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| session_id | BIGINT UNSIGNED | 外键，关联sessions表 |
| seat_id | VARCHAR(50) | 座位编号（如"A1"） |
| seat_name | VARCHAR(50) | 座位名称（如"前排1号"） |
| seat_type | VARCHAR(20) | 类型：front-前排，middle-中排，back-后排 |
| status | TINYINT(1) | 状态：0-可用，1-已锁定，2-已预订 |
| price | BIGINT | 座位价格（VIP座位可加价） |

**业务逻辑**：
1. **未选座模式**：不创建seat记录，直接使用`available_seats`计数
2. **选座模式**：
   - 用户选择座位时status=1（锁定），锁定10分钟
   - 支付完成后status=2（已预订）
   - 超时未支付则status=0（释放）

**唯一约束**：`idx_session_seat`确保场次内座位号唯一

**关联关系**：
- 多对一关联`sessions`（多个座位属于一个场次）
- 多对多关联`bookings`（通过`booking_seats`关联表）

---

### 5. bookings（预订表）

**描述**：用户预订记录，核心订单概念，创建后未支付则过期

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| user_id | BIGINT UNSIGNED | 外键，关联users表 |
| theme_id | BIGINT UNSIGNED | 外键，关联themes表 |
| session_id | BIGINT UNSIGNED | 外键，关联sessions表 |
| order_no | VARCHAR(32) | 订单号，唯一索引 |
| total_amount | BIGINT | 总金额（分） |
| seat_count | INT UNSIGNED | 预订座位数量 |
| booking_date | DATE | 预订日期 |
| status | TINYINT(1) | 0-待支付，1-已支付，2-已取消，3-已完成 |

**状态流转**：
```
待支付(0) → 已支付(1) → 已完成(3)
   ↓              ↓
已取消(2)      已退款（通过订单表）
```

**重要特性**：
- 前端"创建订单"时生成booking记录
- 30分钟内未支付自动取消（status=2）
- 支付成功后创建对应的`orders`记录

**关联关系**：
- 多对一关联`users`（多个预订属于一个用户）
- 多对一关联`themes`（多个预订属于一个主题）
- 多对一关联`sessions`（多个预订属于一个场次）
- 一对多关联`booking_seats`（一个预订多个座位）
- 一对多关联`orders`（一个预订对应多个订单，考虑分次支付场景）

---

### 6. booking_seats（预订座位关联表）

**描述**：预订与座位的多对多关联表

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| booking_id | BIGINT UNSIGNED | 外键，关联bookings表 |
| seat_id | BIGINT UNSIGNED | 外键，关联seats表 |
| seat_name | VARCHAR(50) | 冗余存储，避免seat表修改后丢失历史信息 |
| price | BIGINT | 该座位实际成交价格 |

**设计目的**：
- 解耦`bookings`和`seats`，支持灵活退改
- `seat_name`和`price`冗余，保障历史数据完整性

**关联关系**：
- 多对一关联`bookings`
- 多对一关联`seats`

---

### 7. orders（订单表）

**描述**：支付订单，与微信支付系统对接

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| user_id | BIGINT UNSIGNED | 外键，关联users表 |
| booking_id | BIGINT UNSIGNED | 外键，关联bookings表 |
| order_no | VARCHAR(32) | 订单号（与booking的order_no一致） |
| total_amount | BIGINT | 总金额 |
| pay_amount | BIGINT | 实际支付金额（优惠券场景） |
| status | TINYINT(1) | 0-待支付，1-已支付，2-支付失败，3-已退款 |
| payment_method | VARCHAR(20) | 支付方式（目前仅wechat） |
| payment_time | DATETIME | 支付时间 |
| transaction_id | VARCHAR(64) | 微信支付交易号 |

**与booking的区别**：
- `booking`是业务订单概念
- `order`是支付订单，对接微信支付系统
- 考虑组合支付、多次支付场景，一个booking可对应多个order

**关联关系**：
- 多对一关联`users`
- 多对一关联`bookings`
- 一对多关联`payments`（一个订单多次支付记录）
- 一对多关联`verification_codes`（一个订单多个核销码）

---

### 8. payments（支付记录表）

**描述**：支付流水记录，用于对账和审计

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| order_id | BIGINT UNSIGNED | 外键，关联orders表 |
| order_no | VARCHAR(32) | 订单号 |
| transaction_id | VARCHAR(64) | 微信支付交易号 |
| amount | BIGINT | 支付金额 |
| status | TINYINT(1) | 0-支付中，1-支付成功，2-支付失败 |
| payment_type | VARCHAR(20) | 支付类型 |
| pay_url | VARCHAR(500) | 支付链接（用于H5支付场景） |
| notify_result | JSON | 微信回调结果，完整存储用于对账 |

**审计价值**：
- 记录每一次支付请求和回调
- `notify_result`保存完整微信回调，便于问题排查

**关联关系**：
- 多对一关联`orders`

---

### 9. verification_codes（核销码表）

**描述**：用于线下核销的凭证，生成二维码供工作人员扫描

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| order_id | BIGINT UNSIGNED | 外键，关联orders表 |
| code | VARCHAR(20) | 核销码（6-8位数字字母组合） |
| qr_code_url | VARCHAR(500) | 二维码图片URL |
| status | TINYINT(1) | 0-未使用，1-已使用，2-已过期 |
| expiry_time | TIMESTAMP | 过期时间（订单结束后24小时） |
| verified_at | DATETIME | 核销时间 |
| admin_id | BIGINT UNSIGNED | 核销管理员ID |
| remarks | VARCHAR(500) | 核销备注 |

**生成规则**：
- 每个订单生成唯一核销码
- 格式：`NH` + 6位随机码 + 2位校验位（如：NH-3X9K7P）
- 有效期至活动结束+24小时

**使用场景**：
- 用户到场出示二维码
- 工作人员扫码核销
- 核销后status=1，不可重复使用

**关联关系**：
- 多对一关联`orders`

---

### 10. theme_images（主题图片表）

**描述**：主题详情图片，与轮播图分开管理

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| theme_id | BIGINT UNSIGNED | 外键，关联themes表 |
| image_url | VARCHAR(255) | 图片URL |
| sort_order | INT UNSIGNED | 排序顺序 |
| image_type | VARCHAR(20) | banner-轮播图，detail-详情图 |

**设计目的**：
- 主题详情页需要大量图片展示
- 与`themes.banner_images`分开，便于独立管理
- `sort_order`控制前端展示顺序

**关联关系**：
- 多对一关联`themes`

---

### 11. goods（商品表）

**描述**：附加商品，如写真、化妆、摄影服务、座位包

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| name | VARCHAR(100) | 商品名称 |
| description | TEXT | 商品描述 |
| price | BIGINT | 价格（分） |
| image_url | VARCHAR(255) | 商品图片 |
| category | VARCHAR(50) | photo-写真，makeup-化妆，photography-摄影，seat_package-座位商品包 |
| status | TINYINT(1) | 上下架状态 |
| stock | INT UNSIGNED | 库存 |

**业务场景**：
- 用户预订时可加购商品
- `seat_package`用于打包销售座位（如情侣包）
- 库存管理防止超卖

**关联关系**：
- 多对多关联`bookings`（通过`booking_goods`表）

---

### 12. booking_goods（预订商品关联表）

**描述**：预订与商品的关联关系

**字段说明**：
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT UNSIGNED | 主键 |
| booking_id | BIGINT UNSIGNED | 外键，关联bookings表 |
| goods_id | BIGINT UNSIGNED | 外键，关联goods表 |
| quantity | INT UNSIGNED | 购买数量 |
| price | BIGINT | 实际成交价格 |

**关联关系**：
- 多对一关联`bookings`
- 多对一关联`goods`

---

## 表关系总览

```
users(用户)
    ├─ bookings(预订) 一对多
    │   ├─ sessions(场次) 多对一
    │   ├─ themes(主题) 多对一
    │   ├─ booking_seats(预订座位) 一对多
    │   └─ booking_goods(预订商品) 一对多
    │
    └─ orders(订单) 一对多
        ├─ bookings(预订) 多对一
        ├─ payments(支付记录) 一对多
        └─ verification_codes(核销码) 一对多

sessions(场次)
    ├─ themes(主题) 多对一
    └─ seats(座位) 一对多
        └─ booking_seats(预订座位) 一对多

themes(主题)
    ├─ sessions(场次) 一对多
    ├─ bookings(预订) 一对多
    └─ theme_images(主题图片) 一对多

goods(商品)
    └─ booking_goods(预订商品) 一对多
```

---

## 核心业务流程

### 1. 预订流程

```sql
-- 步骤1：创建预订记录
INSERT INTO bookings (user_id, theme_id, session_id, order_no, ...)

-- 步骤2：锁定座位（选座模式）
UPDATE seats SET status = 1 WHERE id IN (...)

-- 步骤3：创建订单
INSERT INTO orders (booking_id, user_id, order_no, ...)

-- 步骤4：支付成功后
UPDATE bookings SET status = 1
UPDATE orders SET status = 1, payment_time = NOW()
UPDATE seats SET status = 2
INSERT INTO verification_codes (order_id, code, ...)
```

### 2. 核销流程

```sql
-- 工作人员扫码核销
UPDATE verification_codes
SET status = 1, verified_at = NOW(), admin_id = ?
WHERE code = ? AND status = 0

-- 查询核销状态
SELECT * FROM verification_codes WHERE code = ?
```

### 3. 取消/退款流程

```sql
-- 取消未支付订单
UPDATE bookings SET status = 2 WHERE order_no = ? AND status = 0
UPDATE orders SET status = 3 WHERE order_no = ?
UPDATE seats SET status = 0 WHERE id IN (...)
```

---

## 索引优化说明

### 高频查询场景及索引

1. **用户登录/信息查询**
   ```sql
   SELECT * FROM users WHERE openid = ?
   -- 使用 idx_openid
   ```

2. **主题列表查询**
   ```sql
   SELECT * FROM themes WHERE status = 1
   -- 使用 idx_status
   ```

3. **场次查询**
   ```sql
   SELECT * FROM sessions WHERE theme_id = ? AND status = 1
   -- 使用 idx_theme_id, idx_status
   ```

4. **我的订单列表**
   ```sql
   SELECT * FROM bookings WHERE user_id = ? ORDER BY created_at DESC
   -- 使用 idx_user_id
   ```

5. **订单详情查询**
   ```sql
   SELECT * FROM orders WHERE order_no = ?
   -- 使用 idx_order_no（唯一索引）
   ```

6. **核销码查询**
   ```sql
   SELECT * FROM verification_codes WHERE code = ?
   -- 使用 idx_code（唯一索引）
   ```

---

## 数据归档建议

1. **历史订单归档**：超过2年的已完成订单可归档到历史表
2. **核销码清理**：已核销超过1年的核销码可归档
3. **支付记录归档**：已完成的支付记录归档保留审计痕迹

---

## 扩展设计考虑

### 预留扩展字段
- `categories` (JSON)：支持动态分类
- `notify_result` (JSON)：存储完整的微信回调

### 未来可扩展表
1. **优惠券表(coupons)**：支持营销活动
2. **评价表(reviews)**：用户完成后的评价
3. **管理员表(admins)**：后台管理系统用户
4. **操作日志表(operation_logs)**：记录重要操作审计

---

## 数据库规范

### 命名规范
- 表名：小写字母，复数形式（如`users`, `bookings`）
- 字段名：小写字母，下划线分隔（如`created_at`）
- 索引名：`idx_字段名`（如`idx_user_id`）

### 数据类型规范
- 金额：BIGINT类型，单位为分，避免浮点数精度问题
- JSON数据：使用JSON类型，便于查询和索引
- 状态字段：TINYINT类型，节省存储空间
- ID字段：BIGINT UNSIGNED，支持海量数据

### 默认值规范
- 字符串：NOT NULL DEFAULT ''
- 数值：NOT NULL DEFAULT 0
- 状态字段：NOT NULL DEFAULT 0
- 时间戳：NOT NULL DEFAULT CURRENT_TIMESTAMP

---

## 版本记录

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|----------|------|
| 1.0 | 2025-11-26 | 初始版本 | Claude Code |
| 1.1 | 2025-11-27 | 添加主题图片表和商品表 | Claude Code |

---

## 联系方式

- 项目负责人：Claude Code
- 技术栈：Java 8 + Spring Boot 2.6 + MyBatis-Plus + MySQL 8.0
- 部署方式：Docker容器化部署
