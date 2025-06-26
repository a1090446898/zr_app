package com.gsoft.inventory.data;

import com.rscja.team.qcom.deviceapi.T;

/**
 * @author 10904
 */
public class AsyncResult<T> {
    private final T data;
    private final Exception error;

    private AsyncResult(T data, Exception error) {
        this.data = data;
        this.error = error;
    }

    public static <T> AsyncResult<T> success(T data) {
        return new AsyncResult<>(data, null);
    }

    public static <T> AsyncResult<T> error(Exception error) {
        return new AsyncResult<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public T getData() {
        return data;
    }

    public Exception getError() {
        return error;
    }
}