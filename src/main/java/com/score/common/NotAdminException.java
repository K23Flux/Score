package com.score.common;

/**
 * 非管理员访问仅管理员可用的功能。
 */
public class NotAdminException extends RuntimeException {
    public NotAdminException() {
        super("仅管理员可以访问该功能");
    }

    public NotAdminException(String message) {
        super(message);
    }
}
