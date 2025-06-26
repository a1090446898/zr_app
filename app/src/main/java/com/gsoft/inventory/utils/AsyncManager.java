package com.gsoft.inventory.utils;

import androidx.core.util.Consumer;
import com.gsoft.inventory.data.AsyncResult;

import java.util.concurrent.*;

public class AsyncManager {
    // 核心线程数
    private static final int CORE_POOL_SIZE = 5;
    // 最大线程数
    private static final int MAX_POOL_SIZE = 10;
    // 线程空闲时间
    private static final long KEEP_ALIVE_TIME = 60L;
    // 时间单位
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    // 任务队列容量
    private static final int QUEUE_CAPACITY = 100;
    // 单例实例
    private static final AsyncManager INSTANCE = new AsyncManager();
    // 线程池执行器
    private final ExecutorService executorService;

    private AsyncManager() {
        // 创建线程池
        executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TIME_UNIT,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 获取 AsyncManager 的单例实例
     * @return AsyncManager 单例实例
     */
    public static AsyncManager getInstance() {
        return INSTANCE;
    }

    /**
     * 提交一个 Runnable 任务到线程池执行
     * @param task 要执行的 Runnable 任务
     */
    public void execute(Runnable task) {
        executorService.execute(task);
    }

    /**
     * 提交一个 Callable 任务到线程池执行，并返回一个 Future 对象
     * @param task 要执行的 Callable 任务
     * @param <T> 任务返回结果的类型
     * @return 表示任务异步结果的 Future 对象
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    /**
     * 提交一个 Callable 任务到线程池执行，并通过回调处理结果
     * @param task 要执行的 Callable 任务
     * @param callback 处理任务结果的回调
     * @param <T> 任务返回结果的类型
     */
    /**
     * 提交一个 Callable 任务到线程池执行，并通过回调处理结果
     * @param task 要执行的 Callable 任务
     * @param callback 处理任务结果的回调
     * @param <T> 任务返回结果的类型
     */
    public <T> void submitWithCallback(Callable<T> task, Consumer<AsyncResult<T>> callback) {
        Future<T> future = executorService.submit(task);
        executorService.execute(() -> {
            try {
                // 因为该操作位阻塞操作，所以需要单独开一个线程来接收结果
                T result = future.get();
                callback.accept(AsyncResult.success(result));
            } catch (InterruptedException | ExecutionException e) {
                callback.accept(AsyncResult.error(e));
            }
        });
    }

    /**
     * 关闭线程池，不再接受新任务，并尝试终止正在执行的任务
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查线程池是否已经关闭
     * @return 如果线程池已经关闭返回 true，否则返回 false
     */
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    /**
     * 检查线程池是否已经终止
     * @return 如果线程池已经终止返回 true，否则返回 false
     */
    public boolean isTerminated() {
        return executorService.isTerminated();
    }
}