CREATE TABLE IF NOT EXISTS ai_decision_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    company_id BIGINT NULL,
    application_id BIGINT NOT NULL,
    application_score_id BIGINT NULL,

    ai_recommendation TEXT,
    decision VARCHAR(80) NOT NULL,
    reason TEXT,

    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NOT NULL,

    create_time DATETIME NOT NULL,
    deleted TINYINT DEFAULT 0,

    INDEX idx_ai_reviews_company_id (company_id),
    INDEX idx_ai_reviews_application_id (application_id),
    INDEX idx_ai_reviews_score_id (application_score_id),
    INDEX idx_ai_reviews_decision (decision),
    INDEX idx_ai_reviews_reviewed_by (reviewed_by)
);