package com.gsoft.inventory.service.asset_code_scan;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import com.gsoft.inventory.AssetsCodeScanActivity;
import com.gsoft.inventory.AssetsRecodeActivity;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.*;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author 10904
 */
public class ScanCodeResultHandle  extends Handler {
    private final WeakReference<Activity> mActivity;

    public ScanCodeResultHandle(Activity activity) {
        this.mActivity = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        Activity activity = mActivity.get();
        if (msg.arg1 == 1) {
            String barCode = msg.obj.toString();
            if (activity instanceof AssetsCodeScanActivity) {
                AssetsCodeScanActivity assetsActivity = (AssetsCodeScanActivity) activity;
                Accounts loginAccount = assetsActivity.loginAccount;
                if (loginAccount == null) {
                    assetsActivity.showShortText("登录失效，请退出重新登录");
                    return;
                }
                List<Assets> assetsList = Assets.find(Assets.class, "BARCODEID=? AND ACCTSUITEID=?", barCode, loginAccount.getZTBH());
                if (assetsList.size() == 1) {
                    if (assetsActivity.isSustain) {
                        Assets assets = assetsList.get(0);
                        if (!StringUtils.isNullOrEmpty(assets.getREMARKS()) && !assets.getREMARKS().contains("盘实")) {
                            assets.setREMARKS("盘实");
                        }
                        assets.setISUPLOAD(Const.NOT_UPLOAD);
                        assets.setPDZT("02");
                        assets.save();
                        AssetsUtils.uploadAssets(assets, assetsActivity);
                        assetsActivity.showShortText(barCode.concat("已盘实"));
                        assetsActivity.assetsCreating = assets;
                        assetsActivity.fillAssetsData();
                    } else {
                        assetsActivity.assetsCreating = assetsList.get(0);
                        assetsActivity.fillAssetsData();
                    }
                } else if (assetsList.size() > 1) {
                    Intent intentRecode = new Intent(assetsActivity, AssetsRecodeActivity.class);
                    intentRecode.putExtra(SysDefine.IntentExtra_SERIALCODE, barCode);
                    assetsActivity.startActivity(intentRecode);
                    assetsActivity.destroy();
                    assetsActivity.finish();
                } else {
                    assetsActivity.showShortText("未找到此编码资产！");
                }
            }
        } else {
            if (activity != null && activity instanceof com.gsoft.inventory.AssetsCodeScanActivity) {
                ((com.gsoft.inventory.AssetsCodeScanActivity) activity).showShortText("扫码失败！");
            }
        }
    }
}
