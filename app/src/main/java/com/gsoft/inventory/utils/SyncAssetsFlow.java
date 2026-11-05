package com.gsoft.inventory.utils;

import android.content.Context;
import android.os.Handler;

import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.service.main.SyncAssetsTask;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

/**
 * 资产同步（上传）公共流程：统计本地未同步数量，确认后异步上传
 * <p>
 * 流程：未同步数量为0 → 直接提示；否则确认弹窗 → 网络校验 → 进度框 + SyncAssetsTask异步同步 → 回调提示结果
 * <p>
 * 复用入口：BaseCompatActivity/BaseFragment 的 startSyncAssetsTask()，
 * 供首页"同步上传"、RFID盘点统计页"上传数据"等入口共用
 *
 * @author 10904
 */
public final class SyncAssetsFlow {

    private SyncAssetsFlow() {
    }

    /**
     * 宿主页面回调：由调用方提供短提示与进度框能力
     */
    public interface Callback {
        /**
         * 短提示
         *
         * @param textMessage 提示内容
         */
        void showShortText(CharSequence textMessage);

        /**
         * 显示进度框
         *
         * @param title   标题
         * @param message 内容
         */
        void showProgressDialog(String title, String message);

        /**
         * 隐藏进度框
         */
        void hideProgressDialog();
    }

    /**
     * 启动资产同步流程（主线程调用）
     *
     * @param context     上下文（弹窗、Toast与同步任务使用）
     * @param zcqcyCode   资产清查唯一码（InventoryApplication.getZcqcyCode()）
     * @param mainHandler 主线程Handler（异步结果切回主线程）
     * @param callback    宿主页面回调（提示与进度框）
     */
    public static void startSync(Context context, String zcqcyCode, Handler mainHandler, Callback callback) {
        long waitingSyncCount = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{Const.NOT_UPLOAD});
        if (waitingSyncCount == 0) {
            callback.showShortText("当前资产数据已经全部同步，无需同步！");
            return;
        }
        String tips = "共有：".concat(String.valueOf(waitingSyncCount)).concat("条记录未同步，是否开始同步？");

        new QMUIDialog.MessageDialogBuilder(context)
                .setTitle("系统提示")
                .setMessage(tips)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "开始同步", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    if (!NetworkUtils.checkNetConnect(context)) {
                        callback.showShortText("当前网络连接不正常，请稍后再试！");
                        return;
                    }

                    callback.showProgressDialog("", "正在同步");
                    SyncAssetsTask task = new SyncAssetsTask(context, zcqcyCode);
                    AsyncManager.getInstance().submitWithCallback(task, result -> {
                        mainHandler.post(() -> {
                            callback.hideProgressDialog();
                            callback.showShortText(result.getData());
                        });
                    });
                    dialog.dismiss();
                }).show();
    }
}