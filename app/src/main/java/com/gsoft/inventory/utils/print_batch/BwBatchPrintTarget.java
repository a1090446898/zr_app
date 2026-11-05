package com.gsoft.inventory.utils.print_batch;

import android.content.Context;
import android.graphics.Bitmap;

import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.bw_print.BwLabelRenderer;
import com.gsoft.inventory.utils.bw_print.BwPrintManager;
import com.gsoft.inventory.utils.domain.Assets;

/**
 * 宝威批量打印执行器：逐张渲染位图后串行下发（渲染在调用线程，print内部已切工作线程）
 * <p>
 * 位图用完立即回收：单张约0.45MB，100张常驻会OOM
 *
 * @author 10904
 */
public class BwBatchPrintTarget implements BatchPrintTarget {

    /** 每条资产的打印抬头（"部门名称"/"单位+部门名称"模式下每条不同） */
    public interface TitleProvider {
        String titleFor(Assets assets);
    }

    private final Context context;
    private final String labelType;
    private final String position;
    private final boolean preferWifi;
    private final TitleProvider titleProvider;

    public BwBatchPrintTarget(Context context, String labelType, String position,
                              boolean preferWifi, TitleProvider titleProvider) {
        this.context = context.getApplicationContext();
        this.labelType = labelType;
        this.position = position;
        this.preferWifi = preferWifi;
        this.titleProvider = titleProvider;
    }

    @Override
    public void printOne(Assets assets, Callback callback) {
        Bitmap bitmap;
        try {
            String title = titleProvider == null ? "" : titleProvider.titleFor(assets);
            bitmap = BwLabelRenderer.renderLabel(context, labelType, assets, title, position);
        } catch (Exception e) {
            callback.onResult(false, "标签生成异常：" + e.getMessage());
            return;
        }
        if (bitmap == null) {
            callback.onResult(false, "标签生成失败，请检查打印类型");
            return;
        }
        String epcHex = BwLabelRenderer.buildEpcHex(assets.getBARCODEID());
        BwPrintManager.print(bitmap, epcHex, preferWifi, (success, message) -> {
            // 无论成败都回收，避免逐张累积（旋转副本在BwPrintManager内回收）
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            if (success) {
                AsyncManager.getInstance().execute(() -> PrintCompatActivity.markBarcodePrinted(assets));
            }
            callback.onResult(success, message);
        });
    }
}