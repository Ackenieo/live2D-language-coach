package com.ackenieo.init_pro.interfaces.dto;

import com.ackenieo.init_pro.application.usecase.UseCaseResult;

/**
 * 统一响应DTO
 */
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, null, message);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, null, message);
    }

    public static <T> ApiResponse<T> fromUseCaseResult(UseCaseResult<T> result) {
        if (result.isSuccess()) {
            return success(result.getData());
        } else {
            return failure(result.getErrorMessage());
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
