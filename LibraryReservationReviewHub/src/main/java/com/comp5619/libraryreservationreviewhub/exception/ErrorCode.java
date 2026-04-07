package com.comp5619.libraryreservationreviewhub.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "Invalid request parameters"),
    NOT_LOGIN_ERROR(40100, "Not logged in"),
    NO_AUTH_ERROR(40101, "No permission"),
    NOT_FOUND_ERROR(40400, "Requested data not found"),
    BOOK_NOT_FOUND_ERROR(40401, "Book not found"),  // New entry
    FORBIDDEN_ERROR(40300, "Access forbidden"),
    USER_BANNED(40301, "User is banned"),
    SYSTEM_ERROR(50000, "Internal system error"),
    OPERATION_ERROR(50001, "Operation failed");


    /**
     * state code
     */
    private final int code;

    /**
     * message
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}

