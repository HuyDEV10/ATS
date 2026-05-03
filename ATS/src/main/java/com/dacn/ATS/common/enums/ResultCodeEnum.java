package com.dacn.ATS.common.enums;

public enum ResultCodeEnum {
    SUCCESS(200, "Thành công"),
    BAD_REQUEST(400, "Yêu cầu không hợp lệ"),
    UNAUTHORIZED(401, "Chưa đăng nhập"),
    FORBIDDEN(403, "Không có quyền truy cập"),
    NOT_FOUND(404, "Không tìm thấy"),
    INTERNAL_ERROR(500, "Lỗi hệ thống"),
    USERNAME_EXISTS(1001, "Tên đăng nhập đã tồn tại"),
    EMAIL_EXISTS(1002, "Email đã tồn tại"),
    USER_NOT_FOUND(1003, "Người dùng không tồn tại"),
    PASSWORD_ERROR(1004, "Mật khẩu không đúng");

    private final int code;
    private final String message;

    ResultCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
