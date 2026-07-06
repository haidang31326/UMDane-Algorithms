CREATE TABLE user_restreaks (
    user_id BIGINT PRIMARY KEY,
    restreaks_available INT NOT NULL DEFAULT 0
);

CREATE TABLE user_restreak_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    event_date DATE NOT NULL,
    INDEX idx_user_restreak_events_user (user_id)
);
