package com.gsoft.inventory.utils;



import androidx.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadManager {
    private static volatile ThreadManager Instance = null;
    private ExecutorService cachedThreadPool;

    private ThreadManager() {
        if (cachedThreadPool == null) {
            /** 确保只存在一个线程池，这是一个无界线程池*/
            cachedThreadPool = Executors.newCachedThreadPool();
        }
    }

    /**
     * 获得线程管理器对象 <br>
     * 使用了double-check单例模式创建。
     *
     * @return
     */
    public static ThreadManager get() {
        if (Instance == null) {
            synchronized (ThreadManager.class) {
                if (Instance == null) Instance = new ThreadManager();
                return Instance;
            }
        }
        return Instance;
    }

    /**
     * 启动一个任务
     *
     * @param runnable
     */
    public void start(@NonNull Runnable runnable) {
        cachedThreadPool.execute(runnable);
    }

    /**
     * 使用submit（）方法启动线程
     *
     * @param task
     * @param <T>
     * @return
     */
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        return cachedThreadPool.submit(task);
    }

    /**
     * 关闭线程池
     */
    public void stop() {
        if (Instance == null) {
            return;
        }
        synchronized (this) {
            if (Instance != null) {
                cachedThreadPool.shutdownNow();
            }
            Instance = null;
        }
    }
}
