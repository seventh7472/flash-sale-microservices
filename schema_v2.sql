 USE flash_sale_db;
 DROP TABLE IF EXISTS product, `user`, inventory, `order`, flash_sale_activity, cheating_log, traffic_prediction;
 CREATE TABLE `user` (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk',
     username VARCHAR(50) NOT NULL COMMENT 'username',
     password VARCHAR(100) NOT NULL COMMENT 'bcrypt password',
     nickname VARCHAR(50) DEFAULT NULL COMMENT 'nickname',
     phone VARCHAR(20) DEFAULT NULL COMMENT 'phone',
     role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
     risk_level TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal/1 suspicious/2 blacklist',
     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
     update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
     PRIMARY KEY(id), UNIQUE KEY uk_username(username)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE product (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk', name VARCHAR(200) NOT NULL COMMENT 'name',
     original_price DECIMAL(10,2) NOT NULL COMMENT 'price', image VARCHAR(500) DEFAULT NULL COMMENT 'img',
     description TEXT COMMENT 'desc', status TINYINT DEFAULT 1 COMMENT '1=on 0=off',
     create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY(id), KEY idx_status(status)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE seckill_activity (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk', activity_name VARCHAR(200) NOT NULL COMMENT 'name',
     product_id BIGINT NOT NULL COMMENT 'product', flash_price DECIMAL(10,2) NOT NULL COMMENT 'price',
     total_stock INT NOT NULL DEFAULT 0 COMMENT 'stock', limit_quantity INT NOT NULL DEFAULT 1 COMMENT 'limit',
     start_time DATETIME NOT NULL COMMENT 'start', end_time DATETIME NOT NULL COMMENT 'end',
     status TINYINT DEFAULT 0 COMMENT '0=wait 1=active 2=end 3=cancel',
     create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY(id), KEY idx_pid(product_id), KEY idx_st(status,start_time,end_time)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE stock (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk', product_id BIGINT NOT NULL COMMENT 'product',
     total_stock INT NOT NULL DEFAULT 0 COMMENT 'total', sold_count INT NOT NULL DEFAULT 0 COMMENT 'sold',
     version INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock',
     create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY(id), UNIQUE KEY uk_pid(product_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE seckill_order (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk', order_no VARCHAR(64) NOT NULL COMMENT 'no',
     user_id BIGINT NOT NULL COMMENT 'user', product_id BIGINT NOT NULL COMMENT 'product',
     activity_id BIGINT NOT NULL COMMENT 'activity', quantity INT NOT NULL DEFAULT 1 COMMENT 'qty',
     flash_price DECIMAL(10,2) NOT NULL COMMENT 'flash', total_price DECIMAL(10,2) NOT NULL COMMENT 'total',
     status TINYINT NOT NULL DEFAULT 0 COMMENT '0=unpaid 1=paid 2=cancel 3=refund',
     create_time DATETIME DEFAULT CURRENT_TIMESTAMP, pay_time DATETIME DEFAULT NULL,
     update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY(id), UNIQUE KEY uk_on(order_no), KEY idx_uid(user_id), KEY idx_aid(activity_id),
     KEY idx_ua(user_id,activity_id), KEY idx_sc(status,create_time)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE seckill_record (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk', user_id BIGINT NOT NULL COMMENT 'user',
     activity_id BIGINT NOT NULL COMMENT 'activity', buy_time DATETIME NOT NULL COMMENT 'time',
     status TINYINT DEFAULT 0 COMMENT '0=valid 1=cancel',
     create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY(id), UNIQUE KEY uk_ua(user_id,activity_id), KEY idx_aid(activity_id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE cheat_blacklist (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk', user_id BIGINT DEFAULT NULL COMMENT 'user',
     ip VARCHAR(64) DEFAULT NULL COMMENT 'ip', reason VARCHAR(500) DEFAULT NULL COMMENT 'reason',
     risk_score INT DEFAULT 0 COMMENT 'score', expire_time DATETIME DEFAULT NULL COMMENT 'expire',
     is_active TINYINT DEFAULT 1 COMMENT '1=active', create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY(id), KEY idx_uid(user_id), KEY idx_ip(ip)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 CREATE TABLE traffic_metrics (
     id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'pk',
     activity_id BIGINT DEFAULT NULL COMMENT '关联的秒杀活动ID，全局指标为NULL',
     interface_name VARCHAR(100) NOT NULL COMMENT '接口名称',
     qps INT NOT NULL COMMENT '该时间窗口内的请求总数（不是每秒，是一分钟内的累计）',
     predicted_peak_qps INT DEFAULT NULL COMMENT 'AI预测的未来峰值QPS（回写）',
     collect_time DATETIME NOT NULL COMMENT '采集时间',
     PRIMARY KEY(id), KEY idx_activity_time(activity_id, collect_time), KEY idx_collect_time(collect_time)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 INSERT INTO product(name,original_price) VALUES
 ('iPhone 16 Pro Max',9999),('MacBook Air M3',12999),('AirPods Pro 2',1899);
 INSERT INTO stock(product_id,total_stock) VALUES (1,100),(2,50),(3,200);
 INSERT INTO seckill_activity(activity_name,product_id,flash_price,total_stock,limit_quantity,start_time,end_time,status)
 VALUES ('iPhone Sale',1,8499,100,1,NOW()-INTERVAL 1 HOUR,NOW()+INTERVAL 2 HOUR,1),
        ('MacBook Sale',2,10999,50,1,NOW(),NOW()+INTERVAL 1 HOUR,1);
