-- ============================================================
-- 商品管理系统 - 表结构导出
-- 导出日期：2026-08-28
-- 数据库：shoplook2026_0324
-- 共 25 张业务表 + 5 张新权限表(auth_rbac.sql)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、业务表（14张）
-- ============================================================

-- ----------------------------
-- 1. 商品品牌表
-- ----------------------------
DROP TABLE IF EXISTS `brand`;
CREATE TABLE `brand` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '品牌名称',
  `company` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公司名称',
  `logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'logo',
  `site` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '网址',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品品牌表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2. 购物车表
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_id` int UNSIGNED NOT NULL COMMENT '会员编号',
  `good_id` int UNSIGNED NOT NULL COMMENT '商品编号。如果有sku，则关联sku表',
  `qty` int UNSIGNED NOT NULL COMMENT '数量',
  `selected` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否选中（勾选结算）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_member_good`(`member_id` ASC, `good_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '购物车表' ROW_FORMAT = DYNAMIC;

-- 已存在库增量升级：购物车勾选字段
-- ALTER TABLE `cart` ADD COLUMN `selected` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否选中（勾选结算）' AFTER `qty`;

-- ----------------------------
-- 3. 商品类别表
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` int NULL DEFAULT NULL COMMENT '父类别编号',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类别名称',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类别标题',
  `tag` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类别标签',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类别样式',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类别摘要',
  `sort` int NULL DEFAULT 10 COMMENT '排序号',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 73 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品类别表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 4. 商品信息表（SPU）
-- ----------------------------
DROP TABLE IF EXISTS `good`;
CREATE TABLE `good` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'spu编号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名称',
  `alias` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品别名',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品摘要',
  `category_id` int NULL DEFAULT NULL COMMENT '所属分类编号',
  `brand_id` int NULL DEFAULT NULL COMMENT '所属品牌编号',
  `mark_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '建议售价，标售价',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '实售价',
  `qty` int NULL DEFAULT NULL COMMENT '库存数量，如有单独的库存表，则以库存表为准',
  `pic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主图',
  `pic2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '次图',
  `detail_pics` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '详情图，多副详情图使用逗号分隔',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '详情，富文本编辑器，可覆盖spu详情',
  `is_take_down` bit(1) NULL DEFAULT b'0' COMMENT '是否下架',
  `is_hot` bit(1) NULL DEFAULT b'0' COMMENT '是否热销',
  `is_del` bit(1) NULL DEFAULT b'0' COMMENT '是否删除',
  `is_seckill` bit(1) NULL DEFAULT b'0' COMMENT '是否秒杀商品',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品信息表（SPU）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 5. 商品详情图表
-- ----------------------------
DROP TABLE IF EXISTS `good_detail_pics`;
CREATE TABLE `good_detail_pics` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '非业务主键',
  `good_id` int UNSIGNED NOT NULL COMMENT '商品编号',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '细节图地址',
  `sort` int NULL DEFAULT 10 COMMENT '显示顺序',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品详情图表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 6. 前端用户（会员）表
-- ----------------------------
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '非业务主键',
  `account` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '密码',
  `enabled` bit(1) NULL DEFAULT b'1' COMMENT '是否启用',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '姓名，全名',
  `pinyin` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '中文姓名的全拼形式',
  `pinyin_untoned` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '中文姓名的无音调拼音连写形式',
  `first_name` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '名',
  `last_name` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '姓氏',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '性别',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `height` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5, 1) UNSIGNED NULL DEFAULT NULL COMMENT '体重(kg)',
  `iq` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '智商指数',
  `qq` varchar(16) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT 'qq号',
  `wechat` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '微信号',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '电子邮箱',
  `native_place_id` int NULL DEFAULT NULL COMMENT '籍贯所在地编号',
  `card_id` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '身份证号',
  `wedlock` enum('已婚','未婚','离异') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '婚姻状况',
  `political_orientation` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '政治面貌',
  `address_id` int UNSIGNED NULL DEFAULT NULL COMMENT '家庭住址',
  `address_detail` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '地址详情',
  `race` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '汉' COMMENT '民族',
  `religion` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '宗教',
  `nationality` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '中国' COMMENT '国籍',
  `portrait` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '头像',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '备注',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后一次登录时间',
  `last_login_ip` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '最后一次登录IP',
  `extra_info` json NULL COMMENT '额外用户信息',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `version` int UNSIGNED NOT NULL DEFAULT 1 COMMENT '修改版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_account`(`account` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1001 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '前端用户（会员）表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 7. 会员收货地址表
-- ----------------------------
DROP TABLE IF EXISTS `member_address`;
CREATE TABLE `member_address` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会员账号',
  `receiver` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收货人姓名',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `addr_id` int NULL DEFAULT NULL COMMENT '街道编号',
  `addr_detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地址详情',
  `is_default` bit(1) NULL DEFAULT b'0' COMMENT '是否默认地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '会员收货地址表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 8. 订单表
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单编号（雪花算法）',
  `seckill_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '秒杀编号（雪花算法）',
  `member_account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会员账号',
  `total_pay` decimal(10, 2) NULL DEFAULT NULL COMMENT '订单总价',
  `pay_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付方式',
  `alipay_trade_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付宝交易号',
  `checkout_time` datetime NULL DEFAULT NULL COMMENT '下单时间',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `ship_time` datetime NULL DEFAULT NULL COMMENT '发货时间',
  `accept_time` datetime NULL DEFAULT NULL COMMENT '确认收货时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单状态',
  `receiver_addr_id` int NULL DEFAULT NULL COMMENT '收货人地址编号',
  `receiver_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收货人手机号',
  `receiver_addr_detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收货人完整地址',
  `order_comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单备注（客户）',
  `is_del` bit(1) NULL DEFAULT b'0' COMMENT '是否逻辑删除',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注（平台）',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_order_no`(`order_no` ASC) USING BTREE,
  UNIQUE INDEX `uq_seckill_no`(`seckill_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 9. 订单项表
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` int NULL DEFAULT NULL COMMENT '订单编号',
  `good_id` int NULL DEFAULT NULL COMMENT '商品编号',
  `deal_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '成交价格',
  `count` int NULL DEFAULT NULL COMMENT '数量',
  `good_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `good_pic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品图片',
  `good_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品摘要',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单项备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '订单项表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 10. 商品SKU表
-- ----------------------------
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sku_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'sku编号',
  `spu_id` int NULL DEFAULT NULL COMMENT 'spu编号',
  `spec_group_id` int NULL DEFAULT NULL COMMENT '规格组编号',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `alias` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品别名',
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品摘要',
  `category_id` int NULL DEFAULT NULL COMMENT '所属分类编号',
  `brand_id` int NULL DEFAULT NULL COMMENT '所属品牌编号',
  `mark_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '标售价',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '实售价',
  `qty` int NULL DEFAULT NULL COMMENT '库存数量',
  `pic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主图',
  `pic2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '次图',
  `detail_pics` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '详情图',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '详情',
  `is_take_down` bit(1) NULL DEFAULT b'0' COMMENT '是否下架',
  `is_hot` bit(1) NULL DEFAULT NULL COMMENT '是否热销',
  `is_del` bit(1) NULL DEFAULT NULL COMMENT '是否删除',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '商品SKU表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 11. 秒杀活动表
-- ----------------------------
DROP TABLE IF EXISTS `seckill`;
CREATE TABLE `seckill` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '秒杀名称',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '是否开启',
  `start_time` datetime NULL DEFAULT NULL COMMENT '秒杀开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '秒杀结束时间',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '秒杀活动表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 12. 秒杀-商品关联表
-- ----------------------------
DROP TABLE IF EXISTS `seckill_good`;
CREATE TABLE `seckill_good` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seckill_id` int NOT NULL COMMENT '秒杀活动编号',
  `good_id` int NOT NULL COMMENT '商品编号',
  `seckill_price` decimal(10, 2) NOT NULL DEFAULT 0 COMMENT '秒杀价',
  `stock_count` int NOT NULL DEFAULT 0 COMMENT '限量库存',
  `stock_sold` int NOT NULL DEFAULT 0 COMMENT '已售（DB 账本）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_seckill_good`(`seckill_id` ASC, `good_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '秒杀-商品关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 13. 规格组表
-- ----------------------------
DROP TABLE IF EXISTS `spec_group`;
CREATE TABLE `spec_group` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规格组名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '规格组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 14. 规格项表
-- ----------------------------
DROP TABLE IF EXISTS `spec_item`;
CREATE TABLE `spec_item` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spec_group_id` int NOT NULL COMMENT '规格组编号',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规格名称',
  `value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '规格值',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_group_name`(`spec_group_id` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '规格项表' ROW_FORMAT = DYNAMIC;


-- ============================================================
-- 二、系统表（11张）
-- ============================================================

-- ----------------------------
-- 15. 中国地区信息表
-- ----------------------------
DROP TABLE IF EXISTS `t_cn_region_info`;
CREATE TABLE `t_cn_region_info` (
  `CRI_ID` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `CRI_CODE` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代码',
  `CRI_NAME` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `CRI_SHORT_NAME` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '简称',
  `CRI_PARENT_ID` int UNSIGNED NULL DEFAULT NULL COMMENT '上级ID',
  `CRI_SUPERIOR_CODE` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上级代码',
  `CRI_LNG` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '经度',
  `CRI_LAT` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '纬度',
  `CRI_SORT` int NULL DEFAULT NULL COMMENT '排序',
  `CRI_GMT_CREATE` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `CRI_GMT_MODIFIED` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `CRI_MEMO` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `CRI_DATA_STATE` int NULL DEFAULT NULL COMMENT '状态',
  `CRI_TENANT_CODE` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户ID',
  `CRI_LEVEL` int NULL DEFAULT NULL COMMENT '级别',
  PRIMARY KEY (`CRI_ID`) USING BTREE,
  UNIQUE INDEX `idx_cri_code`(`CRI_CODE` ASC) USING BTREE,
  INDEX `idx_cri_parent_id`(`CRI_PARENT_ID` ASC) USING BTREE,
  INDEX `idx_cri_superior_code`(`CRI_SUPERIOR_CODE` ASC) USING BTREE,
  INDEX `idx_cri_level`(`CRI_LEVEL` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46463 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '中国地区信息' ROW_FORMAT = COMPACT;

-- ----------------------------
-- 16. 后台用户表（管理员表）
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名（账号）',
  `password` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（加密）',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像地址',
  `enabled` bit(1) NULL DEFAULT b'1' COMMENT '是否启用',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态（0正常 1异常）',
  `locked` bit(1) NULL DEFAULT b'0' COMMENT '锁定状态（0未锁定 1已锁定）',
  `user_expire_time` datetime NULL DEFAULT NULL COMMENT '账号过期时间',
  `credential_expire_time` datetime NULL DEFAULT NULL COMMENT '权限过期时间',
  `login_times` int UNSIGNED NULL DEFAULT 0 COMMENT '登录次数',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '上次登录时间',
  `last_login_ip` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上次登录IP',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  `op_mode` int NULL DEFAULT 7 COMMENT '操作权限（1删 2改 4读）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '后台用户表（管理员表）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 17. 用户组表
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_group`;
CREATE TABLE `t_rbac_group` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '是否启用',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 18. 用户组-角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_group_role`;
CREATE TABLE `t_rbac_group_role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` int NOT NULL COMMENT '组编号',
  `role_id` int NOT NULL COMMENT '角色编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_group_role`(`group_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户组-角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 19. 菜单表
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_menu`;
CREATE TABLE `t_rbac_menu` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` int NULL DEFAULT NULL COMMENT '父菜单编号',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `icon` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单链接地址',
  `sort` int NULL DEFAULT 10 COMMENT '顺序号',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 20. 权限表（旧版）
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_perm`;
CREATE TABLE `t_rbac_perm` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '是否启用',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限表（旧版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 21. 权限-资源关联表
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_perm_resource`;
CREATE TABLE `t_rbac_perm_resource` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `resource_id` int NOT NULL COMMENT '资源编号',
  `perm_id` int NOT NULL COMMENT '权限编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_perm_resource`(`resource_id` ASC, `perm_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限-资源关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 22. 资源表
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_resource`;
CREATE TABLE `t_rbac_resource` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资源名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '资源类型（路由/菜单/按钮/数据）',
  `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源值',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_name`(`name` ASC) USING BTREE,
  UNIQUE INDEX `uq_value`(`value` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '资源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 23. 角色表（旧版）
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_role`;
CREATE TABLE `t_rbac_role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '是否启用',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表（旧版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 24. 角色-权限关联表（旧版）
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_role_perm`;
CREATE TABLE `t_rbac_role_perm` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` int NOT NULL COMMENT '角色编号',
  `perm_id` int NOT NULL COMMENT '权限编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_role_perm`(`role_id` ASC, `perm_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色-权限关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 25. 用户-组关联表
-- ----------------------------
DROP TABLE IF EXISTS `t_rbac_user_group`;
CREATE TABLE `t_rbac_user_group` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int NOT NULL COMMENT '用户编号',
  `group_id` int NOT NULL COMMENT '组编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_user_group`(`user_id` ASC, `group_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户-组关联表' ROW_FORMAT = DYNAMIC;


-- ============================================================
-- 三、新权限表（5张，来自 auth_rbac.sql，计划用于新auth模块）
-- ============================================================

-- ----------------------------
-- 26. 后台管理员表（新版）
-- ----------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1启用 0禁用）',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '后台管理员表（新版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 27. 角色表（新版）
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码（ROLE_ADMIN等）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表（新版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 28. 权限/菜单表（新版）
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` int UNSIGNED NULL DEFAULT 0 COMMENT '父权限编号（0=顶级）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限编码（如goods:list）',
  `type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '类型（1目录 2菜单 3按钮）',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '前端路由路径',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int NULL DEFAULT 0 COMMENT '排序号',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1启用 0禁用）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `updated_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后修改人',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '权限表（新版）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 29. 用户-角色关联表（新版）
-- ----------------------------
DROP TABLE IF EXISTS `admin_user_role`;
CREATE TABLE `admin_user_role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` int UNSIGNED NOT NULL COMMENT '管理员编号',
  `role_id` int UNSIGNED NOT NULL COMMENT '角色编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 30. 角色-权限关联表（新版）
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` int UNSIGNED NOT NULL COMMENT '角色编号',
  `permission_id` int UNSIGNED NOT NULL COMMENT '权限编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_perm`(`role_id` ASC, `permission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色权限关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 31. 支付流水表（Stage 5 模拟支付，雪花ID）
-- ----------------------------
DROP TABLE IF EXISTS `pay_log`;
CREATE TABLE `pay_log` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `order_id` int NULL DEFAULT NULL COMMENT '订单编号（order.id）',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单编号',
  `member_account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会员账号',
  `biz_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型：PAY-支付 / REFUND-退款',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '渠道：ALIPAY/WECHAT（均模拟）',
  `trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模拟渠道流水号（雪花生成）',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额',
  `created_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_biz`(`order_id` ASC, `biz_type` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付/退款流水（模拟）' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;