package com.dacn.ATS.module.ai.controller;

import com.dacn.ATS.module.ai.dto.CvScoreResult;
import com.dacn.ATS.module.ai.service.AiScoringService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ai")
public class AiScreeningController {

    private final AiScoringService aiScoringService;

    public AiScreeningController(AiScoringService aiScoringService) {
        this.aiScoringService = aiScoringService;
    }

    @PostMapping("/score/{applicationId}")
    public String scoreApplication(
            @PathVariable Long applicationId,
            Model model) {
        CvScoreResult result = aiScoringService.scoreCv(applicationId);
        model.addAttribute("result", result);
        return "ai/score-result";
    }
}