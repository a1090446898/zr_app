package com.gsoft.inventory.utils.print_batch;

import android.os.Handler;
import android.os.Looper;

import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量打印会话：与打印机品牌、与页面都无关的串行队列。
 * <p>
 * 1. 串行：上一条回调后才发下一条，并做间隔节流（打印机只认"指令下发"，并发会丢标签）；
 * 2. 失败不自动继续：进入等待决策状态，由Listener弹框询问，页面调用resume/abort；
 * 3. 终止/销毁：abort/release清理Handler，避免页面退出后继续出纸；
 * 4. 页面销毁时务必调用release()。
 *
 * @author 10904
 */
public class BatchPrintSession {

    public interface Listener {
        /** 即将打印第 current 条（finishedCount为已完成条数，用于显示进度） */
        void onProgress(int finishedCount, int total, Assets current);

        /**
         * 某条打印失败：页面必须弹框询问用户，
         * 选择"继续"调用resume()，选择"终止"调用abort()（不回调将一直暂停）
         */
        void onItemFailed(Assets assets, int position, int total, String message);

        /** 整批结束（正常完成、用户终止、无数据均会回调一次） */
        void onFinished(BatchPrintReport report);
    }

    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_WAIT_DECISION = 2;
    private static final int STATE_ABORTED = 3;
    private static final int STATE_FINISHED = 4;

    private final List<Assets> items;
    private final BatchPrintTarget target;
    private final Listener listener;
    private final long intervalMs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Long> printedIds = new ArrayList<>();

    private int state = STATE_IDLE;
    private int index = 0;
    private int successCount = 0;
    private int failedCount = 0;
    private boolean cancelled = false;

    public BatchPrintSession(List<Assets> items, BatchPrintTarget target, Listener listener) {
        this(items, target, listener, BatchPrintConfig.ITEM_INTERVAL_MS);
    }

    public BatchPrintSession(List<Assets> items, BatchPrintTarget target,
                             Listener listener, long intervalMs) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.target = target;
        this.listener = listener;
        this.intervalMs = intervalMs;
    }

    /** 开始打印（仅在未开始时有效，重复调用忽略，防重复点击） */
    public void start() {
        if (state == STATE_RUNNING || state == STATE_ABORTED || state == STATE_FINISHED) {
            return;
        }
        if (items.isEmpty()) {
            state = STATE_FINISHED;
            notifyFinished();
            return;
        }
        state = STATE_RUNNING;
        printNext(0);
    }

    /** 用户选择"继续打印"：跳过失败项，从下一条继续 */
    public void resume() {
        if (state != STATE_WAIT_DECISION) {
            return;
        }
        state = STATE_RUNNING;
        printNext(intervalMs);
    }

    /** 用户选择"立即终止"或页面退出 */
    public void abort() {
        if (state == STATE_FINISHED || state == STATE_ABORTED) {
            return;
        }
        cancelled = true;
        state = STATE_ABORTED;
        handler.removeCallbacksAndMessages(null);
        notifyFinished();
    }

    /** 页面销毁时调用：清理回调，避免内存泄漏与退出后继续打印 */
    public void release() {
        handler.removeCallbacksAndMessages(null);
        if (state != STATE_FINISHED && state != STATE_ABORTED) {
            cancelled = true;
            state = STATE_ABORTED;
        }
    }

    /** 是否仍在进行（含等待用户决策），供"取消打印"确认框判断 */
    public boolean isRunning() {
        return state == STATE_RUNNING || state == STATE_WAIT_DECISION;
    }

    private void printNext(long delay) {
        handler.postDelayed(() -> {
            if (state != STATE_RUNNING) {
                return;
            }
            if (index >= items.size()) {
                state = STATE_FINISHED;
                notifyFinished();
                return;
            }
            final Assets assets = items.get(index);
            if (listener != null) {
                listener.onProgress(index, items.size(), assets);
            }
            target.printOne(assets, (success, message) ->
                    handler.post(() -> onItemResult(assets, success, message)));
        }, delay);
    }

    private void onItemResult(Assets assets, boolean success, String message) {
        if (state != STATE_RUNNING) {
            return;
        }
        if (success) {
            successCount++;
            if (assets.getId() != null) {
                printedIds.add(assets.getId());
            }
            index++;
            printNext(intervalMs);
            return;
        }
        // 失败：先跳过该条，进入等待决策，由页面弹框后决定继续或终止
        failedCount++;
        int position = index + 1;
        index++;
        state = STATE_WAIT_DECISION;
        if (listener != null) {
            listener.onItemFailed(assets, position, items.size(), message);
        } else {
            resume();
        }
    }

    private void notifyFinished() {
        if (listener != null) {
            listener.onFinished(new BatchPrintReport(
                    items.size(), successCount, failedCount, cancelled, printedIds));
        }
    }
}