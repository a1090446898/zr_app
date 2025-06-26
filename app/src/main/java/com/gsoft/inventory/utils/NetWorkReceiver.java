package com.gsoft.inventory.utils;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.entities.SyncResult;
import org.json.JSONArray;
import org.json.JSONException;

public class NetWorkReceiver extends BroadcastReceiver {
    Context context;
    private static boolean isSyncAssets = false;
    private static final int MSG_WHART_SHOWTOAST = 21;
    private MessageToaskHandler toaskHandler;

    private static class MessageToaskHandler extends Handler {
        private final WeakReference<NetWorkReceiver> weakReference;

        public MessageToaskHandler(NetWorkReceiver receiver) {
            weakReference = new WeakReference<NetWorkReceiver>(receiver);
        }

        @Override
        public void handleMessage(Message msg) {
            NetWorkReceiver receiver = weakReference.get();
            if (receiver != null) {
                if (msg.what == MSG_WHART_SHOWTOAST && receiver.context != null) {
                    Toast.makeText(receiver.context, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public NetWorkReceiver() {
        toaskHandler = new MessageToaskHandler(NetWorkReceiver.this);
    }

    /**
     * 联网自动触发同步操作
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        // this.syncAssets();
    }

    /**
     * 自动同步资产
     */
    private void syncAssets() {
        if (NetworkUtils.checkNetConnect(context)) {
            if (System.currentTimeMillis() - lastSyncAssetsTime > 1000 * 90) {
                isSyncAssets = false;
            }
            Thread syncThread = new Thread(() -> {
                if (!isSyncAssets) {
                    isSyncAssets = true;
                    SyncAssets();
                }
            });
            syncThread.start();
        }
    }

    private Gson gson = new Gson();
    private StringCallback responseCallback = new StringCallback() {
        @Override
        public void onResponseSuccess(String result) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                List<SyncResult> results = gson.fromJson(result, new TypeToken<List<SyncResult>>() {}.getType());
                if (results == null || results.size() == 0) {
                    onResponseFailure("联网更新失败，请联系技术人员");
                } else {
                    for (SyncResult sr : results) {
                        String barcode = sr.barcodeid;
                        if (sr.result.equals("1")) {
                            List<Assets> assets = Assets.find(Assets.class, "BARCODEID=?", barcode);
                            for (Assets ast : assets) {
                                if (ast.getREMARKS().equals("待删除")) {
                                    ast.delete();
                                } else {
                                    ast.setISUPLOAD("1");
                                    ast.save();
                                }
                            }
                        } else {
                            stringBuilder.append("条码：".concat(barcode).concat("联网更新失败，"));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                onResponseFailure("联网更新失败，请联系技术人员");
            }
            if (stringBuilder.toString().length() > 2) {
                onResponseFailure(stringBuilder.toString());
            }
        }

        @Override
        public void onResponseFailure(String errMessage) {
            showToast("联网上传失败");
        }
    };

    private StringCallback disposeAddCallBack = new StringCallback() {
        @Override
        public void onResponseSuccess(String result) {

        }

        @Override
        public void onResponseFailure(String errMessage) {

        }
    };

    private void showToast(String toast) {
        Message msg = Message.obtain();
        msg.obj = toast;
        msg.what = MSG_WHART_SHOWTOAST;
        toaskHandler.sendMessage(msg);
    }

    static long lastSyncAssetsTime = 0;

    private void SyncAssets() {
        if (!NetworkUtils.checkNetConnect(context)) {
            isSyncAssets = false;
            return;
        }
        lastSyncAssetsTime = System.currentTimeMillis();
        List<Assets> assetsList = Assets.find(Assets.class, "ISUPLOAD=?", "0");
        if (!assetsList.isEmpty()) {
            ///一分半的同步时间，超时退出
            JsonArray jsonArray = new JsonArray();
            for (Assets assets : assetsList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("data", assets.toLineString());
                jsonArray.add(jsonObject);
            }
            if (jsonArray.size() > 0) {
                OkHttpUtils.getInstance().postAssetsJson(SysDefine.SERVERHOST_ASSETS(), gson.toJson(jsonArray), responseCallback);
            }
            while (isSyncAssets && (System.currentTimeMillis() - lastSyncAssetsTime) < 1000 * 90) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long waitingSyncCount = Assets.count(Assets.class, "ISUPLOAD=?", new String[]{"0"});
                if (waitingSyncCount == 0) {
                    isSyncAssets = false;
                }
            }
        }

        List<DisposeAsset> disposeAdding = DisposeAsset.find(DisposeAsset.class, "STATUS=?", "1");
        List<DisposeAsset> disposeDeleting = DisposeAsset.find(DisposeAsset.class, "STATUS=?", "2");
        for (DisposeAsset asset : disposeAdding) {
            OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), asset.getDispose(), asset.getCode()), new StringCallback() {
                @Override
                public void onResponseSuccess(String result) {
                    asset.setStatus(0);
                    asset.save();
                }

                @Override
                public void onResponseFailure(String errMessage) {

                }
            });
        }
        for (DisposeAsset asset : disposeDeleting) {
            OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), asset.getDispose(), asset.getCode()), new StringCallback() {
                @Override
                public void onResponseSuccess(String result) {
                    asset.setStatus(0);
                    asset.save();
                }

                @Override
                public void onResponseFailure(String errMessage) {

                }
            });
        }
        isSyncAssets = false;
    }
}