package com.dacn.ATS.common.util;

import com.dacn.ATS.module.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserUtil {

    public static Long getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails == null ? null : userDetails.getId();
    }

    public static Long getCurrentCompanyId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails == null ? null : userDetails.getCompanyId();
    }

    public static String getCurrentUserRole() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails == null ? null : userDetails.getRoleName();
    }

    public static boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(getCurrentUserRole());
    }

    public static boolean isCompanyOwner() {
        return "COMPANY_OWNER".equals(getCurrentUserRole());
    }

    private static CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }

        return null;
    }
}
