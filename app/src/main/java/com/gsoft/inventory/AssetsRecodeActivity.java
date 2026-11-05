package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gsoft.inventory.adapter.AssetsRecodeListAdapter;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.domain.Accounts;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.FileSearchHelper;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AssetsRecodeActivity extends PrintCompatActivity {

    @BindView(R.id.topbar)
    QMUITopBar topBar;

    @BindView(R.id.lstRecode)
    ListView lstRecode;

    List<Assets> assetsList = null;
    AssetsRecodeListAdapter recodeListAdapter = null;
    String barCode = null;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_recode);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {

        barCode = getIntent().getStringExtra(SysDefine.IntentExtra_SERIALCODE);
        if (StringUtils.isNullOrEmpty(barCode)) {
            showShortText("无效的条形码");
            return;
        }
        topBar.setTitle("资产条码设置");
        topBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        assetsList = Assets.find(Assets.class, "BARCODEID=? AND ACCTSUITEID=?", barCode, application.getLoginAccount().getZTBH());
        recodeListAdapter = new AssetsRecodeListAdapter(assetsList, mContext);
        lstRecode.setAdapter(recodeListAdapter);
        lstRecode.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showBottomSheet(i);
            }
        });
    }

    private void showBottomSheet(final int assetsIndex) {
        final Assets assets = assetsList.get(assetsIndex);
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .addItem("升级条码")
                .addItem("编辑")
                .addItem("删除")
                .addItem("照片")
                .addItem("盘实")
                .addItem("盘盈")
                .addItem("盘亏")
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        switch (position) {
                            case 0:
                                try {
                                    Accounts accounts = AccountHelper.getACCTSUITEID(assets.getACCTSUITEID());
                                    final String accountsName = accounts == null ? "" : accounts.getZTMC();
                                    String barCode = SysConfig.getNewAssetsBarcode(application.getDeviceNo(), AccountHelper.getDepartCode(application.getLoginAccount().getZTBH(), assets.getASSETSDEPT()), assets.getASSETSTYPE());
                                    if (assets.getSERIALCODE() != null && assets.getSERIALCODE().equals(assets.getBARCODEID())) {
                                        assets.setSERIALCODE(barCode);
                                    }
                                    assets.setBARCODEID(barCode);
                                    assets.save();
                                    recodeListAdapter.notifyDataSetChanged();
                                    new QMUIDialog.MessageDialogBuilder(mContext)
                                            .setTitle("系统提示")
                                            .setMessage("是否打印新条码？")
                                            .addAction("取消", new QMUIDialogAction.ActionListener() {
                                                @Override
                                                public void onClick(QMUIDialog dialog, int index) {
                                                    dialog.dismiss();
                                                }
                                            })
                                            .addAction(0, "打印", QMUIDialogAction.ACTION_PROP_NEGATIVE, new QMUIDialogAction.ActionListener() {
                                                @Override
                                                public void onClick(QMUIDialog dialog, int index) {
                                                    // 打印统一走基类requestPrintAssets：品牌分发/连接检查/连接后续打全在基类（新增打印机无需改动本页）
                                                    requestPrintAssets(accountsName, assets, () -> markBarcodePrinted(assets));
                                                    dialog.dismiss();
                                                }
                                            })
                                            .create(mCurrentDialogStyle).show();
                                } catch (Exception ex) {
                                    showShortText("升级失败，" + ex.getMessage());
                                } catch (UnknownError unknownError) {
                                    showShortText("升级失败，" + unknownError.getMessage());
                                }
                                break;
                            case 1:
                                Intent intentEdit = new Intent(mContext, EditAssetsActivity.class);
                                intentEdit.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                                startActivity(intentEdit);
                                finish();
                                break;
                            case 2:
                                new QMUIDialog.MessageDialogBuilder(mContext)
                                        .setTitle("系统提示")
                                        .setMessage("确定要删除吗？")
                                        .addAction("取消", new QMUIDialogAction.ActionListener() {
                                            @Override
                                            public void onClick(QMUIDialog dialog, int index) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, new QMUIDialogAction.ActionListener() {
                                            @Override
                                            public void onClick(QMUIDialog dialog, int index) {
                                                boolean delAcc = assets.delete();
                                                FileSearchHelper.delFiles(SysConfig.photoDirectory, "jpg", assets.getBARCODEID(), false);
                                                showShortText("删除成功");
                                                dialog.dismiss();
                                                assetsList.remove(assets);
                                                recodeListAdapter.notifyDataSetChanged();
                                            }
                                        })
                                        .create(mCurrentDialogStyle).show();
                                break;
                            case 3:
                                Intent intentPhoto = new Intent(mContext, PhotoActivity.class);
                                intentPhoto.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                                startActivity(intentPhoto);
                                finish();
                                break;
                            case 4:
                                assets.setREMARKS("盘实");
                                assets.save();
                                showShortText("盘实成功");
                                finish();
                                break;
                            case 5:
                                assets.setREMARKS("盘盈");
                                assets.save();
                                showShortText("盘盈成功");
                                finish();
                                break;
                            case 6:
                                assets.setREMARKS("盘亏");
                                assets.save();
                                showShortText("盘亏成功");
                                finish();
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }
                })
                .build().show();
    }
}
