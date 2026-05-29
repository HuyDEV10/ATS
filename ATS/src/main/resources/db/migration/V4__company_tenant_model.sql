CREATE TABLE IF NOT EXISTS companies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    logo_url VARCHAR(500),
    industry VARCHAR(150),
    company_size VARCHAR(100),
    website VARCHAR(255),
    address VARCHAR(500),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME,
    updated_at DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_companies_status (status),
    INDEX idx_companies_name (name)
);

ALTER TABLE users
    ADD COLUMN company_id BIGINT NULL AFTER id,
    ADD COLUMN full_name VARCHAR(255) NULL AFTER username,
    ADD COLUMN phone VARCHAR(50) NULL AFTER email,
    ADD COLUMN status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE' AFTER role;

CREATE INDEX idx_users_company_id ON users (company_id);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_status ON users (status);

ALTER TABLE jobs ADD COLUMN company_id BIGINT NULL AFTER id;
CREATE INDEX idx_jobs_company_id ON jobs (company_id);

ALTER TABLE candidates ADD COLUMN company_id BIGINT NULL AFTER id;
CREATE INDEX idx_candidates_company_id ON candidates (company_id);

ALTER TABLE resumes ADD COLUMN company_id BIGINT NULL AFTER id;
CREATE INDEX idx_resumes_company_id ON resumes (company_id);

ALTER TABLE job_applications ADD COLUMN company_id BIGINT NULL AFTER id;
CREATE INDEX idx_applications_company_id ON job_applications (company_id);

ALTER TABLE interview_records ADD COLUMN company_id BIGINT NULL AFTER id;
CREATE INDEX idx_interviews_company_id ON interview_records (company_id);

ALTER TABLE application_scores ADD COLUMN company_id BIGINT NULL AFTER id;
CREATE INDEX idx_scores_company_id ON application_scores (company_id);
