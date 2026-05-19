ALTER TABLE skill_verifications
    ADD COLUMN source_type VARCHAR(60) NULL AFTER provider,
    ADD COLUMN source_name VARCHAR(160) NULL AFTER source_type,
    ADD COLUMN source_url VARCHAR(500) NULL AFTER certificate_url,
    ADD COLUMN artifact_path VARCHAR(500) NULL AFTER source_url,
    ADD COLUMN confidence_score INT NULL AFTER status,
    ADD COLUMN extracted_skills TEXT NULL AFTER confidence_score,
    ADD COLUMN evidence_summary TEXT NULL AFTER extracted_skills,
    ADD COLUMN trust_signals TEXT NULL AFTER evidence_summary,
    ADD COLUMN risk_signals TEXT NULL AFTER trust_signals,
    ADD COLUMN last_analyzed_at DATETIME NULL AFTER risk_signals;

CREATE INDEX idx_skill_verifications_skill ON skill_verifications (skill_name);
CREATE INDEX idx_skill_verifications_confidence ON skill_verifications (confidence_score);