package com.dacn.ATS.module.portal.service;

import com.dacn.ATS.module.portal.dto.PublicApplyRequest;
import com.dacn.ATS.module.portal.dto.PublicApplyResult;

public interface PublicApplyService {

    PublicApplyResult applyToJob(Long jobId, PublicApplyRequest request);
}