package com.gsoft.inventory.service.batch_copy;

import com.gsoft.inventory.data.CopyListResult;

import java.util.concurrent.Callable;

/**
 * 查询当前账套的批量复制列表
 *
 * @author 10904
 */
public class QueryCopyAssetsTask implements Callable<CopyListResult> {

    private final String ztbH;

    private final int offset;

    private final int limit;

    public QueryCopyAssetsTask(String ztbH, int offset, int limit) {
        this.ztbH = ztbH;
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public CopyListResult call() {
        CopyListResult result = new CopyListResult();
        result.setTotalCount(BatchCopyStore.countCopyAssets(ztbH));
        result.setAssets(BatchCopyStore.queryCopyAssets(ztbH, offset, limit));
        return result;
    }
}