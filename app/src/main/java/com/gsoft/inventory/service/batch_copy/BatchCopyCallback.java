package com.gsoft.inventory.service.batch_copy;

/**
 * 批量复制任务回调，回调在后台线程触发，调用方需自行切换到主线程
 *
 * @author 10904
 */
public interface BatchCopyCallback<T> {

    /**
     * 进度回调
     *
     * @param current 已处理数量
     * @param total   总数量
     */
    void onProgress(int current, int total);

    /**
     * 成功回调
     *
     * @param result 任务结果
     */
    void onSuccess(T result);

    /**
     * 失败回调
     *
     * @param message 失败原因
     */
    void onError(String message);
}