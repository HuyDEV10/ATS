CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(30) NOT NULL,
    create_time DATETIME,
    deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    department VARCHAR(120),
    location VARCHAR(255),
    salary_range VARCHAR(120),
    status VARCHAR(40) NOT NULL,
    hr_id BIGINT,
    publish_date DATETIME,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_jobs_status (status),
    INDEX idx_jobs_hr_id (hr_id)
);

CREATE TABLE IF NOT EXISTS resumes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    uploaded_by BIGINT,
    upload_time DATETIME,
    parse_status VARCHAR(30),
    parsed_text MEDIUMTEXT,
    parse_error TEXT,
    parsed_at DATETIME,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_resumes_file_hash (file_hash)
);

CREATE TABLE IF NOT EXISTS candidates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    skills TEXT,
    experience_years INT,
    resume_id BIGINT,
    source VARCHAR(60),
    created_by BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_candidates_email (email),
    INDEX idx_candidates_resume_id (resume_id)
);

CREATE TABLE IF NOT EXISTS job_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    hr_notes TEXT,
    application_date DATETIME,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_applications_job_status (job_id, status),
    INDEX idx_applications_candidate (candidate_id)
);

CREATE TABLE IF NOT EXISTS application_scores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    resume_id BIGINT,
    overall_score INT,
    skill_score INT,
    experience_score INT,
    keyword_score INT,
    matched_skills TEXT,
    missing_skills TEXT,
    strengths TEXT,
    weaknesses TEXT,
    recommendation TEXT,
    interview_questions TEXT,
    algorithm_version VARCHAR(80),
    prompt_version VARCHAR(80),
    score_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_scores_application (application_id),
    INDEX idx_scores_job (job_id),
    INDEX idx_scores_overall (overall_score)
);

CREATE TABLE IF NOT EXISTS application_score_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_score_id BIGINT NOT NULL,
    criterion VARCHAR(100) NOT NULL,
    weight INT NOT NULL,
    score INT NOT NULL,
    evidence TEXT,
    explanation TEXT,
    create_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_score_details_score_id (application_score_id)
);

CREATE TABLE IF NOT EXISTS interview_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    interviewer_id BIGINT NOT NULL,
    interview_date DATETIME,
    round INT,
    status VARCHAR(40) NOT NULL,
    feedback TEXT,
    score INT,
    recommendation VARCHAR(80),
    meeting_link VARCHAR(500),
    location VARCHAR(255),
    notes TEXT,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_interviews_application (application_id),
    INDEX idx_interviews_interviewer (interviewer_id),
    INDEX idx_interviews_status (status)
);