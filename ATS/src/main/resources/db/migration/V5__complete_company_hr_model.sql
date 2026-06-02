-- =========================================================
-- V5__complete_company_hr_model.sql
-- Purpose:
-- V4 đã tạo companies và thêm company_id cho users/jobs/candidates/resumes/applications/interviews/scores.
-- Vì vậy V5 chỉ bổ sung các cột còn thiếu cho application và skill verification.
-- Tuyệt đối không thêm lại users.company_id, jobs.company_id... để tránh Duplicate column.
-- =========================================================

-- 1. Bổ sung thông tin CV / verification cho job_applications
ALTER TABLE job_applications
    ADD COLUMN resume_id BIGINT NULL AFTER candidate_id,
    ADD COLUMN verification_status VARCHAR(40) NULL AFTER status,
    ADD COLUMN mismatch_score INT NULL AFTER verification_status,
    ADD COLUMN mismatch_summary TEXT NULL AFTER mismatch_score;

CREATE INDEX idx_applications_company_status
ON job_applications (company_id, status);

-- 2. Bổ sung company_id cho skill_verifications
ALTER TABLE skill_verifications
    ADD COLUMN company_id BIGINT NULL AFTER id;

CREATE INDEX idx_skill_verifications_company_id
ON skill_verifications (company_id);    