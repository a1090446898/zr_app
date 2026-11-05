package com.gsoft.inventory.utils.print_batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量打印结果汇总（供页面提示与返回调用页）
 *
 * @author 10904
 */
public class BatchPrintReport {

    private final int total;
    private final int successCount;
    private final int failedCount;
    private final boolean cancelled;
    private final List<Long> printedIds;

    public BatchPrintReport(int total, int successCount, int failedCount,
                            boolean cancelled, List<Long> printedIds) {
        this.total = total;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.cancelled = cancelled;
        this.printedIds = printedIds == null
                ? new ArrayList<>() : Collections.unmodifiableList(new ArrayList<>(printedIds));
    }

    public int getTotal() {
        return total;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public List<Long> getPrintedIds() {
        return printedIds;
    }

    /** 未处理条数（中途终止时提示用） */
    public int getUnfinishedCount() {
        return Math.max(0, total - successCount - failedCount);
    }

    /** 统一结果文案，避免各页面各写一套 */
    public String summary() {
        StringBuilder sb = new StringBuilder("批量打印结束：成功 ").append(successCount).append(" 张");
        if (failedCount > 0) {
            sb.append("，失败 ").append(failedCount).append(" 张");
        }
        if (cancelled && getUnfinishedCount() > 0) {
            sb.append("，未打印 ").append(getUnfinishedCount()).append(" 张");
        }
        return sb.toString();
    }
}