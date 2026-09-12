-- ============================================================
-- Stage 7：秒杀专属限量库存账本
-- 对应计划：doc/schedule/stage-7-plan/seckill-solid-plan.md 任务 4.1
-- 说明：三列均 NOT NULL DEFAULT 0，存量行为安全态（stock_count=0 不可抢）
-- ============================================================

ALTER TABLE `seckill_good`
  ADD COLUMN `seckill_price` decimal(10, 2) NOT NULL DEFAULT 0 COMMENT '秒杀价',
  ADD COLUMN `stock_count`   int NOT NULL DEFAULT 0 COMMENT '限量库存',
  ADD COLUMN `stock_sold`    int NOT NULL DEFAULT 0 COMMENT '已售（DB 账本）';

-- 可选：为存量秒杀商品补数据（按业务定价，示例）
-- UPDATE seckill_good SET seckill_price = 99.00, stock_count = 5 WHERE id = 1;
