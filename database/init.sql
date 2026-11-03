-- 小店账本：数据库与专用账号初始化脚本
-- 用法（需要 MySQL 管理员权限）：
--   1. 把下面的 CHANGE_ME 换成自己的数据库密码
--   2. mysql -uroot -p < database/init.sql
--   3. 把同样的密码写入 src/main/resources/db.properties
--
-- 注意：不要把自己的真实密码提交到 GitHub。

CREATE DATABASE IF NOT EXISTS shop_ledger
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'shop_ledger_app'@'localhost'
  IDENTIFIED BY 'CHANGE_ME';
CREATE USER IF NOT EXISTS 'shop_ledger_app'@'127.0.0.1'
  IDENTIFIED BY 'CHANGE_ME';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON shop_ledger.* TO 'shop_ledger_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON shop_ledger.* TO 'shop_ledger_app'@'127.0.0.1';

FLUSH PRIVILEGES;
