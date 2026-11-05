package com.gsoft.inventory.service.batch_copy;

import java.util.concurrent.Callable;

/**
 * 清空当前账套的批量复制标识（不删除数据）
 *
 * @author 10904
 */
public class ClearCopyFlagTask implements Callable<Integer> {

    private final String ztbH;

    public ClearCopyFlagTask(String ztbH) {
        this.ztbH = ztbH;
    }

    @Override
    public Integer call() {
        return BatchCopyStore.clearCopyFlag(ztbH);
    }
}