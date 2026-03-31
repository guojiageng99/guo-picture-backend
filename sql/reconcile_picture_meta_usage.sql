-- 可选：上线后若已有大量历史图片，可执行本脚本根据 picture 表回填 usage_count（在 picture_tag_category.sql 执行之后）
-- MySQL 8+，需在 yu_picture 库执行

-- 带 WHERE 仅为满足 IDE「全表 UPDATE 需有条件」的安全检查，语义仍为更新所有行
UPDATE picture_tag t
SET usage_count = (
    SELECT COUNT(*) FROM picture p
    WHERE p.isDelete = 0
      AND JSON_CONTAINS(COALESCE(p.tags, '[]'), JSON_QUOTE(t.tag_name), '$')
)
WHERE TRUE;

UPDATE picture_category c
SET usage_count = (
    SELECT COUNT(*) FROM picture p
    WHERE p.isDelete = 0 AND p.category = c.category_name
)
WHERE TRUE;
