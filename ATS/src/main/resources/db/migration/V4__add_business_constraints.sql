ALTER TABLE job_applications
ADD CONSTRAINT uk_job_candidate_active UNIQUE (job_id, candidate_id, deleted);