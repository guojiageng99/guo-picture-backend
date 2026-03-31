-- 图片标签字典（与 picture.tags JSON 中的名称对应，仅统计字典内名称）
CREATE TABLE IF NOT EXISTS picture_tag
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    tag_name     VARCHAR(64)  NOT NULL COMMENT '标签名',
    usage_count  INT          NOT NULL DEFAULT 0 COMMENT '被图片引用次数',
    sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tag_name (tag_name),
    INDEX idx_usage (usage_count)
) COMMENT '图片标签字典' COLLATE = utf8mb4_unicode_ci;

-- 图片分类字典（与 picture.category 对应）
CREATE TABLE IF NOT EXISTS picture_category
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    category_name   VARCHAR(64)  NOT NULL COMMENT '分类名',
    usage_count     INT          NOT NULL DEFAULT 0 COMMENT '被图片引用次数',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_category_name (category_name),
    INDEX idx_usage (usage_count)
) COMMENT '图片分类字典' COLLATE = utf8mb4_unicode_ci;

-- 初始数据（与原硬编码列表一致，便于平滑迁移）
INSERT IGNORE INTO picture_tag (tag_name, usage_count, sort_order) VALUES
 ('热门', 0, 0), ('搞笑', 0, 1), ('生活', 0, 2), ('高清', 0, 3), ('艺术', 0, 4), ('校园', 0, 5), ('背景', 0, 6), ('简历', 0, 7), ('创意', 0, 8);

INSERT IGNORE INTO picture_category (category_name, usage_count, sort_order) VALUES
 ('模板', 0, 0), ('电商', 0, 1), ('表情包', 0, 2), ('素材', 0, 3), ('海报', 0, 4);
