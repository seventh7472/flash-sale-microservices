-- ============================================================
-- 流量指标记录表 traffic_metrics
-- 用于存储各秒杀接口每分钟的 QPS 数据
-- 为 AI 流量预测模块提供历史数据基础
-- ============================================================
CREATE TABLE IF NOT EXISTS traffic_metrics (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  activity_id BIGINT DEFAULT NULL COMMENT '关联的秒杀活动ID，全局指标为NULL',
  interface_name VARCHAR(100) NOT NULL COMMENT '接口名称，如 /api/flash-sale/seckill/1',
  qps INT NOT NULL COMMENT '该时间窗口内的请求总数（不是每秒，是一分钟内的累计）',
  predicted_peak_qps INT DEFAULT NULL COMMENT 'AI预测的未来峰值QPS（回写）',
  collect_time DATETIME NOT NULL COMMENT '采集时间',
  PRIMARY KEY (id),
  KEY idx_activity_time (activity_id, collect_time),
  KEY idx_collect_time (collect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量指标记录表-用于AI流量预测';
