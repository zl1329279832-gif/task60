-- ============================================================
-- 综合小区管理系统 - 数据库初始化脚本
-- ============================================================
-- 放置位置: docker/mysql-init/
-- docker-compose 首次启动 MySQL 时会自动执行此目录下所有 .sql 文件
--
-- 使用方法:
--   1. 从开发环境导出完整数据库:
--      mysqldump -u root -p zonghexiaoqiguanlixitong > 01-schema-data.sql
--   2. 将导出文件放到 docker/mysql-init/ 目录
--   3. docker-compose up -d 时自动导入
--
-- 注意: 文件名按字母顺序执行, 建议用数字前缀控制顺序:
--   01-schema.sql      (表结构)
--   02-init-data.sql   (初始数据)
-- ============================================================

-- 如果已有 dump 文件, 删除下面这行占位提示, 直接放入 dump 文件即可
SELECT '请将数据库 dump 文件放到 docker/mysql-init/ 目录下' AS notice;
