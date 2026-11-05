package com.gsoft.inventory.utils.print_batch;

import android.content.Context;
import android.graphics.Bitmap;

import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.ds_print.DsLabelRenderer;
import com.gsoft.inventory.utils.ds_print.DsPrintManager;

/**
 * 得实RFID批量打印执行器：逐张渲染位图后串行下发（渲染在调用线程，打印内部已切工作线程）
 * <p>
 * 与宝威执行器的差异：
 * 1. 打印前校验条码必须14位，不合法直接返回失败（由批量队列弹窗让用户选择跳过或终止）；
 * 2. 位图用完立即回收：单张约0.5MB，100张常驻会OOM；
 * 3. 成功后标记ISPRINTBARCODE=02（待上传）。
 *
 * @author 10904
 */
public class DsBatchPrintTarget implements BatchPrintTarget {

    /** 每条资产的打印抬头（"部门名称"/"单位+部门名称"模式下每条不同） */
    public interface TitleProvider {
        String titleFor(Assets assets);
    }

    private final Context context;
    private final String labelType;
    private final String position;
    private final TitleProvider titleProvider;

    public DsBatchPrintTarget(Context context, String labelType, String position,
                              TitleProvider titleProvider) {
        this.context = context.getApplicationContext();
        this.labelType = labelType;
        this.position = position;
        this.titleProvider = titleProvider;
    }

    @Override
    public void printOne(Assets assets, Callback callback) {
        String barCode = assets == null ? "" : assets.getBARCODEID();
        // 条码必须14位：EPC直接由条码转hex，长度不符写进去的是错误EPC，宁可拦住
        if (!DsLabelRenderer.isValidBarcode(barCode)) {
            callback.onResult(false, DsLabelRenderer.buildBarcodeError(barCode));
            return;
        }
        Bitmap bitmap;
        try {
            String title = titleProvider == null ? "" : titleProvider.titleFor(assets);
            bitmap = DsLabelRenderer.renderLabel(context, labelType, assets, title, position);
        } catch (Exception e) {
            callback.onResult(false, "标签生成异常：" + e.getMessage());
            return;
        }
        if (bitmap == null) {
            callback.onResult(false, "标签生成失败，请检查打印类型");
            return;
        }
        String epcHex = DsLabelRenderer.buildEpcHex(barCode);
        if (epcHex == null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            callback.onResult(false, "EPC生成失败，无法打印");
            return;
        }
        DsPrintManager.print(bitmap, epcHex, new DsPrintManager.PrintCallback() {
            @Override
            public void onResult(boolean success, String message) {
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                if (success) {
                    AsyncManager.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            PrintCompatActivity.markBarcodePrinted(assets);
                        }
                    });
                }
                callback.onResult(success, message);
            }
        });
    }
}