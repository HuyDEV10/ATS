package com.dacn.ATS.exception;

import com.dacn.ATS.common.result.Result;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý BusinessException - chuyển hướng đến trang lỗi với thông báo
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("code", e.getCode());
        mav.addObject("message", e.getMessage());
        return mav;
    }

    // Xử lý các exception khác
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("code", ResultCodeEnum.INTERNAL_ERROR.getCode());
        mav.addObject("message", ResultCodeEnum.INTERNAL_ERROR.getMessage());
        return mav;
    }
}