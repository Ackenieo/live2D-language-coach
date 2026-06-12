package com.ackenieo.init_pro.application.usecase;

/**
 * 用例执行结果
 */
public class UseCaseResult<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;

    private UseCaseResult(boolean success, T data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> UseCaseResult<T> success(T data) {
        return new UseCaseResult<>(true, data, null);
    }

    public static <T> UseCaseResult<T> success() {
        return new UseCaseResult<>(true, null, null);
    }

    public static <T> UseCaseResult<T> failure(String errorMessage) {
        return new UseCaseResult<>(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
