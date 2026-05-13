package com.dacn.ATS.module.audit.service;

public interface AuditLogService {
    void record(Long actorId, String action, String targetType, Long targetId, String detail);
}