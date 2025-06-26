package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.dothantech.printer.IDzPrinter;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.Accounts;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.AssetsCategory;
import com.gsoft.inventory.utils.AssetsCategoryHelper;
import com.gsoft.inventory.utils.AssetsUtils;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DictionaryHelper;
import com.gsoft.inventory.utils.FileSearchHelper;
import com.gsoft.inventory.utils.NetworkUtils;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.gsoft.inventory.utils.SysDefine.REQESTCODE_WAITINGBLUETOOTH;

import java.util.List;

public class AssetsViewActivity extends PrintCompatActivity {

    Assets assets = null;
    QMUITopBar topBar;
    /*资产类别*/
    @BindArray(R.array.zclb)
    String[] zclbArray;
    /*取得方式*/
    @BindArray(R.array.qdfs)
    String[] qdfsArray;
    /*使用状况*/
    @BindArray(R.array.syzk)
    String[] syzkArray;
    /*文物等级*/
    @BindArray(R.array.wwdj)
    String[] wwdjArray;
    /*编制情况*/
    @BindArray(R.array.bzqk)
    String[] bzqkArray;
    /*分类用途*/
    @BindArray(R.array.flyt)
    String[] flytArray;
    /*采购组织形式*/
    @BindArray(R.array.cgzzxs)
    String[] cgzzxsArray;
    /*折旧状态*/
    @BindArray(R.array.zjzt)
    String[] zjztArray;
    /*溯源方式状态*/
    @BindArray(R.array.syfs)
    String[] syfsArray;
    @BindArray(R.array.classifications)
    String[] classifications;
    //region  控件定义
    /*条码编号*/
    @BindView(R.id.tvDOCID)
    TextView tvDOCID;

    @BindView(R.id.tvBARCODEID)
    TextView tvBARCODEID;
    /*CardID*/
    @BindView(R.id.tvCARDID)
    TextView tvCARDID;
    /*取得方式*/
    @BindView(R.id.tvGETMODE)
    TextView tvGETMODE;
    /*备注*/
    @BindView(R.id.tvREMARKS)
    TextView tvREMARKS;
    /*产权单位*/
    @BindView(R.id.tvPROPERTY)
    TextView tvPROPERTY;
    /*发动机号*/
    @BindView(R.id.tvFDJH)
    TextView tvFDJH;
    /*所在楼层，空值*/
    @BindView(R.id.tvSZLC)
    TextView tvSZLC;
    /*使用(保管)人*/
    @BindView(R.id.tvCUSTODIAN)
    TextView tvCUSTODIAN;
    /*车架号*/
    @BindView(R.id.tvCJHM)
    TextView tvCJHM;
    /*账套编号*/
    @BindView(R.id.tvACCTSUITEID)
    TextView tvACCTSUITEID;
    /*卡片序号*/
    @BindView(R.id.tvSERIALNUM)
    TextView tvSERIALNUM;
    /*资产类别*/
    @BindView(R.id.tvASSETSTYPE)
    TextView tvASSETSTYPE;
    /*资产分类代码*/
    @BindView(R.id.tvASSETSSORTCODE)
    TextView tvASSETSSORTCODE;
    /*计量单位*/
    @BindView(R.id.tvASSETSMEASURE)
    TextView tvASSETSMEASURE;
    /*卡片编号*/
    @BindView(R.id.tvSERIALCODE)
    TextView tvSERIALCODE;
    /*资产名称*/
    @BindView(R.id.tvASSETSNAME)
    TextView tvASSETSNAME;
    /*规格*/
    @BindView(R.id.tvASSETSMODEL)
    TextView tvASSETSMODEL;
    /*型号*/
    @BindView(R.id.tvASSETSSTANDARD)
    TextView tvASSETSSTANDARD;
    /*取得日期*/
    @BindView(R.id.tvASSETSUSEDATE)
    TextView tvASSETSUSEDATE;
    /*保修截止日期*/
    @BindView(R.id.tvASSETSENDUSEDATE)
    TextView tvASSETSENDUSEDATE;
    /*预计使用月份*/
    @BindView(R.id.tvYEARCOUNT)
    TextView tvYEARCOUNT;
    /*使用状况*/
    @BindView(R.id.tvUSESTATE)
    TextView tvUSESTATE;
    /*折旧状态*/
    @BindView(R.id.tvDPRECIATIONTYPE)
    TextView tvDPRECIATIONTYPE;
    /*存放地点*/
    @BindView(R.id.tvASSETSLAYADD)
    TextView tvASSETSLAYADD;
    /*使用人*/
    @BindView(R.id.tvASSETSUSER)
    TextView tvASSETSUSER;
    /*使用部门*/
    @BindView(R.id.tvASSETSDEPT)
    TextView tvASSETSDEPT;
    /*卡片金额-原值，设为空值*/
    @BindView(R.id.tvKPJE)
    TextView tvKPJE;
    /*累计折旧*/
    @BindView(R.id.tvDPRECIATIONADD)
    TextView tvDPRECIATIONADD;
    /*资产原值*/
    @BindView(R.id.tvASSETSCURRPRICE)
    TextView tvASSETSCURRPRICE;
    /*当前累计折旧*/
    @BindView(R.id.tvCURRDPRECIATIONADD)
    TextView tvCURRDPRECIATIONADD;
    /*净值*/
    @BindView(R.id.tvLEFTPRICE)
    TextView tvLEFTPRICE;
    /*管理部门*/
    @BindView(R.id.tvGROUNDMANAGEDEPT)
    TextView tvGROUNDMANAGEDEPT;
    /*文物等级*/
    @BindView(R.id.tvASSETSCULGRADE)
    TextView tvASSETSCULGRADE;
    /*车辆产地*/
    @BindView(R.id.tvASSETSPRODAREA)
    TextView tvASSETSPRODAREA;
    /*车牌号*/
    @BindView(R.id.tvVEHICLENO)
    TextView tvVEHICLENO;
    /*发证时间*/
    @BindView(R.id.tvASSETSDISPENSEDATE)
    TextView tvASSETSDISPENSEDATE;
    /*权属所有人*/
    @BindView(R.id.tvASSETSMANAGER)
    TextView tvASSETSMANAGER;
    /*厂牌型号*/
    @BindView(R.id.tvASSETSFACTORYNO)
    TextView tvASSETSFACTORYNO;
    /*排气量*/
    @BindView(R.id.tvVEHEXH)
    TextView tvVEHEXH;
    /*行驶里程*/
    @BindView(R.id.tvLONGMILES)
    TextView tvLONGMILES;
    /*分类用途*/
    @BindView(R.id.tvVEHPURPOSE)
    TextView tvVEHPURPOSE;
    /*编制情况*/
    @BindView(R.id.tvASSETSORGANIZATION)
    TextView tvASSETSORGANIZATION;
    /*数量*/
    @BindView(R.id.tvASSETSNUM)
    TextView tvASSETSNUM;
    /*录入人*/
    @BindView(R.id.tvASSETSWRITER)
    TextView tvASSETSWRITER;
    /*录入日期*/
    @BindView(R.id.tvASSETSWRITERDATE)
    TextView tvASSETSWRITERDATE;
    /*采购组织形式*/
    @BindView(R.id.tvPURCHASINGORGANIZATION)
    TextView tvPURCHASINGORGANIZATION;
    /*品牌*/
    @BindView(R.id.tvBRAND)
    TextView tvBRAND;
    /*盘点表2*/
    @BindView(R.id.tvZT)
    TextView tvZT;
    /*盘点表1*/
    @BindView(R.id.tvPDZT)
    TextView tvPDZT;
    @BindView(R.id.tvISCZ)
    TextView tvISCZ;

    @BindView(R.id.editXLH)
    TextView editXLH;
    @BindView(R.id.editSCCJ)
    TextView editSCCJ;
    @BindView(R.id.editCCBH)
    TextView editCCBH;
    @BindView(R.id.editGLBH)
    TextView editGLBH;
    @BindView(R.id.editCANSHU)
    TextView editCANSHU;
    @BindView(R.id.editSYZQ)
    TextView editSYZQ;
    @BindView(R.id.editSYFS)
    TextView editSYFS;
    @BindView(R.id.editJDJZRQ)
    TextView editJDJZRQ;
    @BindView(R.id.editYXQZ)
    TextView editYXQZ;
    @BindView(R.id.editSSXM)
    TextView editSSXM;
    @BindView(R.id.editClassification)
    TextView editClassification;
    @BindView(R.id.editIsSueDate)
    TextView editIsSueDate;

    @BindView(R.id.editVENDOR)
    TextView editVENDOR;
    //endregion

    @BindView(R.id.editBOOKKEEPINGTIME)
    TextView editBOOKKEEPINGTIME;

    @BindView(R.id.editVOUCHERNO)
    TextView editVOUCHERNO;

    @BindView(R.id.editISUPLOAD)
    TextView editISUPLOAD;

    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_view);
        ButterKnife.bind(this);
        try {
            initializeView();
        } catch (Exception | UnknownError ex) {
            showShortText("解析信息失败，" + ex.getMessage());
        }
    }

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("查看资产详情");
        topBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        topBar.addRightImageButton(R.mipmap.icon_topbar_overflow, R.id.topbar_right_change_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showBottomSheet();
                    }
                });

        long assetsID = getIntent().getLongExtra(SysDefine.IntentExtra_AssetsID, 0);
        if (assetsID == 0) {
            showShortText("资产信息错误，请退出重试！");
            return;
        }
        assets = Assets.findById(Assets.class, assetsID);
        if (assets == null) {
            showShortText("资产信息未找到，请退出重试！");
            return;
        }
        this.initData();

    }


    /**
     * 初始化数据
     */
    private void initData() {
        /*docId*/
        tvDOCID.setText(assets.getDOCID() + "");
        /*条码编号*/
        tvBARCODEID.setText(assets.getBARCODEID() + "");
        /*CardID*/
        tvCARDID.setText(assets.getCARDID());
        /*取得方式*/
        tvGETMODE.setText(DictionaryHelper.getDictionaryName(assets.getGETMODE(), qdfsArray));
        /*备注*/
        tvREMARKS.setText(assets.getREMARKS() + "");
        /*产权单位*/
        tvPROPERTY.setText(assets.getPROPERTY() + "");
        /*发动机号*/
        tvFDJH.setText(assets.getFDJH() + "");
        /*所在楼层，空值*/
        tvSZLC.setText(assets.getSZLC() + "");
        /*使用(保管)人*/
        tvCUSTODIAN.setText(assets.getCUSTODIAN() + "");
        /*车架号*/
        tvCJHM.setText(assets.getCJHM() + "");
        /*账套编号*/
        Accounts accounts = AccountHelper.getACCTSUITEID(assets.getACCTSUITEID());
        tvACCTSUITEID.setText(accounts == null ? "" : accounts.getZTMC());
        /*卡片序号*/
        tvSERIALNUM.setText(assets.getSERIALNUM() + "");
        /*资产类别*/
        tvASSETSTYPE.setText(AssetsCategoryHelper.getAssetsCategoryTitle(assets.getASSETSTYPE()));
        /*资产分类代码*/
        AssetsCategory assetsCategory = AssetsCategoryHelper.getAssetsCategory(assets.getASSETSSORTCODE());
        tvASSETSSORTCODE.setText(assetsCategory == null ? "" : assetsCategory.getNAME());
        /*计量单位*/
        tvASSETSMEASURE.setText(assets.getASSETSMEASURE() + "");
        /*卡片编号*/
        tvSERIALCODE.setText(assets.getSERIALCODE() + "");
        /*资产名称*/
        tvASSETSNAME.setText(assets.getASSETSNAME() + "");
        /*规格*/
        tvASSETSMODEL.setText(assets.getASSETSMODEL() + "");
        /*型号*/
        tvASSETSSTANDARD.setText(assets.getASSETSSTANDARD() + "");
        /*取得日期*/
        tvASSETSUSEDATE.setText(assets.getASSETSUSEDATE() + "");
        /*保修截止日期*/
        tvASSETSENDUSEDATE.setText(assets.getASSETSENDUSEDATE() + "");
        /*预计使用月份*/
        tvYEARCOUNT.setText(assets.getYEARCOUNT() + "");
        /*使用状况*/
        tvUSESTATE.setText(DictionaryHelper.getDictionaryName(assets.getUSESTATE(), syzkArray));
        /*折旧状态*/
        tvDPRECIATIONTYPE.setText(DictionaryHelper.getDictionaryName(assets.getDPRECIATIONTYPE(), zjztArray));
        /*存放地点*/
        tvASSETSLAYADD.setText(assets.getASSETSLAYADD() + "");
        /*使用人*/
        tvASSETSUSER.setText(assets.getASSETSUSER() + "");
        /*使用部门*/
        tvASSETSDEPT.setText(assets.getASSETSDEPT() + "");
        /*卡片金额-原值，设为空值*/
        tvKPJE.setText(assets.getKPJE() + "");
        /*累计折旧*/
        tvDPRECIATIONADD.setText(assets.getDPRECIATIONADD() + "");
        /*资产原值*/
        tvASSETSCURRPRICE.setText(assets.getASSETSCURRPRICE() + "");
        /*当前累计折旧*/
        tvCURRDPRECIATIONADD.setText(assets.getCURRDPRECIATIONADD() + "");
        /*净值*/
        tvLEFTPRICE.setText(assets.getLEFTPRICE() + "");
        /*管理部门*/
        tvGROUNDMANAGEDEPT.setText(AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), assets.getGROUNDMANAGEDEPT()));
        /*文物等级*/
        tvASSETSCULGRADE.setText(DictionaryHelper.getDictionaryName(assets.getASSETSCULGRADE(), wwdjArray));
        /*车辆产地*/
        tvASSETSPRODAREA.setText(assets.getASSETSPRODAREA() + "");
        /*车牌号*/
        tvVEHICLENO.setText(assets.getVEHICLENO() + "");
        /*发证时间*/
        tvASSETSDISPENSEDATE.setText(assets.getASSETSDISPENSEDATE() + "");
        /*权属所有人*/
        tvASSETSMANAGER.setText(assets.getASSETSMANAGER() + "");
        /*厂牌型号*/
        tvASSETSFACTORYNO.setText(assets.getASSETSFACTORYNO() + "");
        /*排气量*/
        tvVEHEXH.setText(assets.getVEHEXH() + "");
        /*行驶里程*/
        tvLONGMILES.setText(assets.getLONGMILES() + "");
        /*分类用途*/
        tvVEHPURPOSE.setText(DictionaryHelper.getDictionaryName(assets.getVEHPURPOSE(), flytArray));//DictionaryHelper.getDictionaryName(
        /*编制情况*/
        tvASSETSORGANIZATION.setText(DictionaryHelper.getDictionaryName(assets.getASSETSORGANIZATION(), bzqkArray));
        /*数量*/
        tvASSETSNUM.setText(assets.getASSETSNUM() + "");
        /*录入人*/
        tvASSETSWRITER.setText(AccountHelper.getUserName(assets.getASSETSWRITER()));
        /*录入日期*/
        tvASSETSWRITERDATE.setText(assets.getASSETSWRITERDATE() + "");
        /*采购组织形式*/
        tvPURCHASINGORGANIZATION.setText(DictionaryHelper.getDictionaryName(assets.getPURCHASINGORGANIZATION(), cgzzxsArray));
        /*品牌*/
        tvBRAND.setText(assets.getBRAND() + "");
        /*盘点表2*/
        tvZT.setText(assets.getZTString());
        /*盘点表1*/
        tvPDZT.setText(assets.getPDZT() + "");
        /*是否财政资产*/
        tvISCZ.setText("02".equals(assets.getISCZ()) ? "是" : "否");

        /**11-25添加10个字段**/
        editXLH.setText(assets.getASSETSSERIALNUMBER() + "");
        editSCCJ.setText(assets.getASSETSSUPPLIER() + "");
        editVENDOR.setText(assets.getVENDOR() + "");
        editCCBH.setText(assets.getFACTORYNO() + "");
        editGLBH.setText(assets.getMANAGENO() + "");
        editCANSHU.setText(assets.getPARAMETERS() + "");
        editSYZQ.setText(assets.getTRACEABILITY() + "");
        editSYFS.setText(DictionaryHelper.getDictionaryName(assets.getTRACEABILITYPE(), syfsArray));
        editJDJZRQ.setText(assets.getCALIBRATIONDATE() + "");
        editYXQZ.setText(assets.getVALIDDATE() + "");
        editSSXM.setText(assets.getPROBELONG() + "");
        /**  210219添加2个字段 **/
        editIsSueDate.setText(assets.getISSUEDATE() + "");
        editClassification.setText(DictionaryHelper.getDictionaryName(assets.getCLASSIFICATION(), classifications));

        // 财务记账时间, 凭证号
        editVOUCHERNO.setText(assets.getVOUCHERNO());
        editBOOKKEEPINGTIME.setText(assets.getBOOKKEEPINGTIME());

        // 同步情况
        String isUpload = assets.getISUPLOAD();
        String isuploadStr = "已同步";
        if("0".equals(isUpload)){
            isuploadStr = "未同步";
        }
        editISUPLOAD.setText(isuploadStr);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQESTCODE_WAITINGBLUETOOTH) {
                printAssets(tvACCTSUITEID.getText().toString(), assets);
            }
        }
    }

    private void showBottomSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(mContext)
                .addItem("打印条码")
                .addItem("编辑")
                .addItem("删除")
                .addItem("照片")
                .addItem("盘实")
                .addItem("盘盈")
                .addItem("盘亏")
                .addItem("复制")
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        switch (position) {
                            case 0:
                                // 打印条码处理
                                if (!isPrinterConnected()) {
                                    Intent intentBluetooth = new Intent(mContext, BluetoothDeviceActivity.class);
                                    startActivityForResult(intentBluetooth, REQESTCODE_WAITINGBLUETOOTH);
                                    return;
                                }
                                printAssets(tvACCTSUITEID.getText().toString(), assets);
                                break;
                            case 1:
                                Intent intentEdit = new Intent(mContext, EditAssetsActivity.class);
                                intentEdit.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                                startActivity(intentEdit);
                                AssetsViewActivity.this.finish();
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
                                                /*showShortText("禁止删除");
                                                dialog.dismiss();*/
                                                assets.setREMARKS("待删除");
                                                assets.setISUPLOAD("0");
                                                assets.save();
                                                AssetsUtils.uploadAssets(assets, mContext);
                                                showShortText("删除请求已提交");
                                                dialog.dismiss();
                                                AssetsViewActivity.this.finish();
                                            }
                                        })
                                        .create(mCurrentDialogStyle).show();
                                break;
                            case 3:
                                Intent intentPhoto = new Intent(mContext, PhotoActivity.class);
                                intentPhoto.putExtra(SysDefine.IntentExtra_AssetsID, assets.getId());
                                startActivity(intentPhoto);
                                AssetsViewActivity.this.finish();
                                break;
                            case 4:
                                assets.setREMARKS("盘实");
                                assets.setISUPLOAD("0");
                                assets.setZT("02");
                                assets.save();
                                break;
                            case 5:
                                assets.setREMARKS("盘盈");
                                assets.setISUPLOAD("0");
                                assets.setZT("03");
                                assets.save();
                                break;
                            case 6:
                                assets.setREMARKS("盘亏");
                                assets.setISUPLOAD("0");
                                assets.setZT("04");
                                assets.save();
                                break;
                            case 7:
                                Intent intentCreate = new Intent(mContext, NewAssetsActivity.class);
                                intentCreate.putExtra(SysDefine.IntentExtra_AssetsView, AssetsUtils.cloneAssets(assets));
                                startActivity(intentCreate);
                                AssetsViewActivity.this.finish();
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
