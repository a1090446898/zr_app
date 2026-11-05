package com.gsoft.inventory.service.batch_copy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 批量标记删除：只改 REMARKS 与 ISUPLOAD，由同步任务提交给服务端
 *
 * @author 10904
 */
public class MarkDeleteCopyAssetsTask implements Callable<Integer> {

    private final List<Long> ids;

    public MarkDeleteCopyAssetsTask(List<Long> ids) {
        this.ids = ids == null ? new ArrayList<>() : ids;
    }

    @Override
    public Integer call() {
        return BatchCopyStore.markDelete(ids);
    }
}