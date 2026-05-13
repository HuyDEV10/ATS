CREATE TABLE IF NOT EXISTS skill_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    candidate_id BIGINT NOT NULL,
    skill_name VARCHAR(160) NOT NULL,
    provider VARCHAR(120),
    certificate_url VARCHAR(500),
    evidence_text TEXT,
    status VARCHAR(40) NOT NULL,
    verified_at DATETIME,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_skill_verifications_candidate (candidate_id),
    INDEX idx_skill_verifications_status (status)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(120),
    target_id BIGINT,
    detail TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_target (target_type, target_id)
);