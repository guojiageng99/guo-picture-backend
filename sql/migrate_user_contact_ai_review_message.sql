-- 用户联系方式（注册必填；历史数据可为 NULL）
ALTER TABLE user
    ADD COLUMN userPhone VARCHAR(32) NULL COMMENT '手机号' AFTER userProfile,
    ADD COLUMN userEmail VARCHAR(128) NULL COMMENT '邮箱' AFTER userPhone;

CREATE UNIQUE INDEX uk_user_userPhone ON user (userPhone);
CREATE UNIQUE INDEX uk_user_userEmail ON user (userEmail);

-- 图片 AI 初审（0 未出结果 1 通过 2 不通过；人工审核仍用 reviewStatus）
ALTER TABLE picture
    ADD COLUMN aiReviewStatus INT NOT NULL DEFAULT 0 COMMENT 'AI审核:0待/1通过/2拒绝' AFTER reviewTime,
    ADD COLUMN aiReviewMessage VARCHAR(512) NULL COMMENT 'AI审核说明' AFTER aiReviewStatus;

-- 若存在分表 picture_{spaceId}，请在各分表上执行与 picture 相同的 ADD COLUMN（结构需一致）

-- 站内消息中心
CREATE TABLE IF NOT EXISTS user_message (
    id           BIGINT       NOT NULL PRIMARY KEY COMMENT 'id',
    userId       BIGINT       NOT NULL COMMENT '接收用户id',
    title        VARCHAR(256) NOT NULL COMMENT '标题',
    content      TEXT         NULL COMMENT '正文',
    bizType      VARCHAR(64)  NULL COMMENT '业务类型',
    bizId        BIGINT       NULL COMMENT '业务主键',
    isRead       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 0否1是',
    createTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_message_user (userId, isRead, createTime)
) COMMENT ='用户站内消息';
