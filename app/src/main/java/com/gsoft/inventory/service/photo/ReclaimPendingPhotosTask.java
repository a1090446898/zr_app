package com.gsoft.inventory.service.photo;

import android.content.Context;

import com.gsoft.inventory.utils.PhotoStorageHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 待认领照片自动归位任务
 * <p>
 * 登录对应账套并同步资产后，资产表能匹配到待认领照片的 DOCID/条码时，
 * 把照片从“_待认领”目录挪入正确的账套目录并恢复为待上传状态。
 * 匹配不到（照片仍不属于当前账套）时保持待认领，等待下次登录其他账套。
 *
 * @author 10904
 */
public class ReclaimPendingPhotosTask implements Callable<Integer> {

    /** 全局运行标记：避免多个页面同时触发归位 */
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final Context context;

    public ReclaimPendingPhotosTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Integer call() {
        if (!RUNNING.compareAndSet(false, true)) {
            return 0;
        }
        try {
            return PhotoStorageHelper.reclaimPendingPhotos(context);
        } finally {
            RUNNING.set(false);
        }
    }
}