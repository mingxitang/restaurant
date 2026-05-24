CREATE TABLE IF NOT EXISTS waiter_call (
    call_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id INT NOT NULL,
    user_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/HANDLED',
    remark VARCHAR(255),
    call_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_time DATETIME NULL,
    handled_by BIGINT NULL,
    CONSTRAINT fk_waiter_call_table FOREIGN KEY (table_id) REFERENCES table_info(table_id) ON DELETE RESTRICT,
    CONSTRAINT fk_waiter_call_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_waiter_call_handler FOREIGN KEY (handled_by) REFERENCES `user`(user_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='顾客呼叫服务员记录表';

CREATE INDEX idx_waiter_call_status ON waiter_call(status);
CREATE INDEX idx_waiter_call_time ON waiter_call(call_time);
