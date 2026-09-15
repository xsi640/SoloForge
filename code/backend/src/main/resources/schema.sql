CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(32) NOT NULL,
  display_name VARCHAR(32) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  is_admin BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_app_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS board (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS board_column (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  board_id BIGINT NOT NULL,
  code VARCHAR(16) NOT NULL,
  name VARCHAR(32) NOT NULL,
  sort_order INT NOT NULL,
  CONSTRAINT uk_board_column_code UNIQUE (board_id, code),
  CONSTRAINT fk_board_column_board FOREIGN KEY (board_id) REFERENCES board (id)
);

CREATE TABLE IF NOT EXISTS task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  board_id BIGINT NOT NULL,
  column_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  assignee_id BIGINT NOT NULL,
  priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  due_date DATE,
  description VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_task_board FOREIGN KEY (board_id) REFERENCES board (id),
  CONSTRAINT fk_task_column FOREIGN KEY (column_id) REFERENCES board_column (id),
  CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_task_column ON task (column_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON task (assignee_id);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON task (due_date);

MERGE INTO board (id, name) KEY (id) VALUES (1, '团队任务看板');

MERGE INTO board_column (board_id, code, name, sort_order) KEY (board_id, code) VALUES (1, 'TODO', '待办', 1);
MERGE INTO board_column (board_id, code, name, sort_order) KEY (board_id, code) VALUES (1, 'DOING', '进行中', 2);
MERGE INTO board_column (board_id, code, name, sort_order) KEY (board_id, code) VALUES (1, 'DONE', '完成', 3);