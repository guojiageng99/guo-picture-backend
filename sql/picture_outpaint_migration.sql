-- AI 扩图：用户额度 + 任务表（执行前请备份）
-- 与 ShardingSphere 无关，落在默认库 yu_picture

ALTER TABLE user ADD COLUMN outpaintQuota INT NOT NULL DEFAULT 10 COMMENT 'AI扩图剩余次数';

CREATE TABLE IF NOT EXISTS picture_outpaint_task (
  id BIGINT NOT NULL COMMENT '主键',
  userId BIGINT NOT NULL,
  pictureId BIGINT NOT NULL,
  spaceId BIGINT NOT NULL DEFAULT 0,
  aliyunTaskId VARCHAR(64) DEFAULT NULL,
  aliSubmittedAt DATETIME DEFAULT NULL COMMENT '提交到阿里云的时间，用于10分钟超时判定',
  status VARCHAR(32) NOT NULL COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/RECONCILING',
  modeCode VARCHAR(32) NOT NULL DEFAULT 'standard' COMMENT 'standard|hd',
  quotaCost INT NOT NULL,
  quotaRefunded TINYINT NOT NULL DEFAULT 0,
  requestJson TEXT,
  outputImageUrl VARCHAR(1024) DEFAULT NULL,
  errorCode VARCHAR(128) DEFAULT NULL,
  errorMessage VARCHAR(512) DEFAULT NULL,
  rawError TEXT,
  reconcileAttempts INT NOT NULL DEFAULT 0,
  createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  finishTime DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_user_create (userId, createTime),
  KEY idx_status_update (status, updateTime)
) COMMENT='AI扩图异步任务';
