package com.gsoft.inventory.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量复制生成结果
 *
 * @author 10904
 */
public class CopyResult {

    /** 失败原因最多保留条数，避免结果对象过大 */
    private static final int MAX_FAIL_MESSAGES = 20;

    private int successCount;

    private int failCount;

    private long costMs;

    private final List<String> failMessages = new ArrayList<>();

    public void addSuccess(int count) {
        this.successCount += count;
    }

    public void addFail(String message) {
        this.failCount++;
        if (failMessages.size() < MAX_FAIL_MESSAGES) {
            failMessages.add(message);
        }
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public long getCostMs() {
        return costMs;
    }

    public void setCostMs(long costMs) {
        this.costMs = costMs;
    }

    public List<String> getFailMessages() {
        return failMessages;
    }

    public boolean isAllSuccess() {
        return failCount == 0;
    }
}