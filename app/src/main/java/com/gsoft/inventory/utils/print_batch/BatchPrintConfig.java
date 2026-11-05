package com.gsoft.inventory.utils.print_batch;

/**
 * 批量打印统一配置（阈值、节流集中管理，避免散落魔法值）
 *
 * @author 10904
 */
public final class BatchPrintConfig {

    /** 单次批量打印上限（超出由调用页分批） */
    public static final int MAX_BATCH_SIZE = 100;

    /**
     * 逐条打印间隔：打印机返回成功仅代表指令已下发，连续发送会堆积队列导致丢标签
     */
    public static final long ITEM_INTERVAL_MS = 400L;

    private BatchPrintConfig() {
    }
}