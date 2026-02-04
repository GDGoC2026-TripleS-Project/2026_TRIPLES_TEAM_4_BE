-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_key VARCHAR(64) NOT NULL,
  type VARCHAR(30) NOT NULL,
  alarm_type VARCHAR(30) NOT NULL,
  team_id BIGINT NOT NULL,
  team_name VARCHAR(60) NOT NULL,
  team_color_hex VARCHAR(10) NOT NULL,
  message_title VARCHAR(120) NOT NULL,
  message_body VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_notifications_event_key UNIQUE (event_key),
  INDEX idx_notifications_team_created (team_id, created_at)
);

-- Notification receipts (per-user)
CREATE TABLE IF NOT EXISTS notification_receipts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  is_completed TINYINT(1) NOT NULL DEFAULT 0,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_notification_user UNIQUE (notification_id, user_id),
  INDEX idx_receipts_user_created (user_id, created_at),
  CONSTRAINT fk_receipts_notification
    FOREIGN KEY (notification_id) REFERENCES notifications(id)
      ON DELETE CASCADE
);
