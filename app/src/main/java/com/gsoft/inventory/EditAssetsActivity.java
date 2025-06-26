package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.gsoft.inventory.abs.AssetsBaseActivity;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.AssetsUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.Objects;

import static com.gsoft.inventory.utils.SysDefine.REQESTCODE_WAITINGBLUETOOTH;

/**
 * @author 10904
 */
public class EditAssetsActivity extends AssetsBaseActivity {


/*    @BindView(R.id.buttonSave)
    QMUIButton buttonSave;

    @BindView(R.id.buttonPrintLabel)
    QMUIButton buttonPrintLabel;*/


    long assetsID;

    //endregion
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_assets);
        ButterKnife.bind(this);

        initSelfData();
        initializeView();

    }

    private void initSelfData () {
        buttonPrintLabel.setOnClickListener(view -> {
            if (assetsCreating == null || assetsCreating.getId() <= 0 || TextUtils.isEmpty(assetsCreating.getBARCODEID())) {
                showShortText("资产信息错误，请先填写信息并保存！");
                return;
            }
            if (!isPrinterConnected()) {
                Intent intentBluetooth = new Intent(mContext, BluetoothDeviceActivity.class);
                startActivityForResult(intentBluetooth, REQESTCODE_WAITINGBLUETOOTH);
                return;
            }
            printAssets(editACCTSUITEID.getText().toString(), assetsCreating);
        });

        buttonSave.setOnClickListener(view -> saveData());

    }

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("资产变更");

        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightImageButton(android.R.drawable.ic_menu_camera, R.id.topbar_right_change_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (assetsCreating == null || StringUtils.isNullOrEmpty(assetsCreating.getBARCODEID())) {
                            showShortText("请先保存基本信息");
                            return;
                        }
                        Intent intentPhoto = new Intent(mContext, PhotoActivity.class);
                        intentPhoto.putExtra(SysDefine.IntentExtra_AssetsID, assetsCreating.getId());
                        startActivity(intentPhoto);
                    }
                });
        assetsID = getIntent().getLongExtra(SysDefine.IntentExtra_AssetsID, 0);
        if (assetsID == 0) {
            showShortText("资产信息错误，请退出重试！");
            return;
        }
        assetsCreating = Assets.findById(Assets.class, assetsID);
        if (assetsCreating == null) {
            showShortText("资产信息未找到，请退出重试！");
            return;
        }

        // 初始化数据
        initData();

        // 图标设置
        iconSetting();

        // 组件状态设置
        viewStateSetting();

        // 监听设置
        viewClickSetting();
        // 初始化选择器
        initCategoryPicker();


        fillAssetsData();


    }

    private void saveData(){
        if (checkRequiredFields()) {
            Assets assetsOld = Assets.findById(Assets.class, assetsID);
            if (assetsOld == null) {
                showShortText("未找到资产记录");
                return;
            }
            // 赋值到assetsCreating中
            setAssetProperties();
            // 比较那些字段有修改
            compareAndLogChanges(assetsOld, assetsCreating);
            assetsCreating.save();
            // 界面赋值
            editBARCODEID.setText(assetsCreating.getBARCODEID());
            editSERIALCODE.setText(assetsCreating.getSERIALCODE());
            showShortText("保存成功");
            buttonPrintLabel.setVisibility(View.VISIBLE);
            // 联网上传
            AssetsUtils.uploadAssets(assetsCreating, mContext);
        }
    }



    private void setAssetProperties() {
       this.setBaseAssetProperties();
        // 特殊
        assetsCreating.setZT(getZTValue());
        assetsCreating.setPDZT("02");
    }





    private String getZTValue() {
        return StringUtils.isNullOrEmpty(assetsCreating.getZT()) || "01".equals(assetsCreating.getZT()) ? "02" : assetsCreating.getZT();
    }

    private void compareAndLogChanges(Assets oldAsset, Assets newAsset) {
        StringBuilder changes = new StringBuilder("修改的字段: ");
        boolean hasChanges = false;

        if (!Objects.equals(oldAsset.getACCTSUITEID(), newAsset.getACCTSUITEID())) {
            changes.append("账套编号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSTYPE(), newAsset.getASSETSTYPE())) {
            changes.append("资产类别, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSSORTCODE(), newAsset.getASSETSSORTCODE())) {
            changes.append("资产分类, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSNAME(), newAsset.getASSETSNAME())) {
            changes.append("资产名称, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getBRAND(), newAsset.getBRAND())) {
            changes.append("品牌, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSMODEL(), newAsset.getASSETSMODEL())) {
            changes.append("规格, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSSTANDARD(), newAsset.getASSETSSTANDARD())) {
            changes.append("型号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSMEASURE(), newAsset.getASSETSMEASURE())) {
            changes.append("计量单位, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSNUM(), newAsset.getASSETSNUM())) {
            changes.append("数量, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSUSER(), newAsset.getASSETSUSER())) {
            changes.append("使用人, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getCUSTODIAN(), newAsset.getCUSTODIAN())) {
            changes.append("管理人, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getASSETSDEPT(), newAsset.getASSETSDEPT())) {
            changes.append("使用部门, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSLAYADD(), newAsset.getASSETSLAYADD())) {
            changes.append("存放地点, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getGROUNDMANAGEDEPT(), newAsset.getGROUNDMANAGEDEPT())) {
            changes.append("管理部门, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getCARDID(), newAsset.getCARDID())) {
            changes.append("卡片编号, ");
            hasChanges = true;
        }
        if(!Objects.equals(oldAsset.getBARCODEID(), newAsset.getBARCODEID())){
            changes.append("条码编号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getREMARKS(), newAsset.getREMARKS())) {
            changes.append("盘实, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSCURRPRICE(), newAsset.getASSETSCURRPRICE())) {
            changes.append("资产原值, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getDPRECIATIONTYPE(), newAsset.getDPRECIATIONTYPE())) {
            changes.append("折旧状态, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getYEARCOUNT(), newAsset.getYEARCOUNT())) {
            changes.append("预计使用月份, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getDPRECIATIONADD(), newAsset.getDPRECIATIONADD())) {
            changes.append("累计折旧, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getCURRDPRECIATIONADD(), newAsset.getCURRDPRECIATIONADD())) {
            changes.append("当前累计折旧, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getLEFTPRICE(), newAsset.getLEFTPRICE())) {
            changes.append("净值, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getASSETSUSEDATE(), newAsset.getASSETSUSEDATE())) {
            changes.append("取得日期, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getGETMODE(), newAsset.getGETMODE())) {
            changes.append("取得方式, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getUSESTATE(), newAsset.getUSESTATE())) {
            changes.append("使用状况, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSWRITERDATE(), newAsset.getASSETSWRITERDATE())) {
            changes.append("录入日期, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSWRITER(), newAsset.getASSETSWRITER())) {
            changes.append("录入人, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSENDUSEDATE(), newAsset.getASSETSENDUSEDATE())) {
            changes.append("保修截止日期, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getPURCHASINGORGANIZATION(), newAsset.getPURCHASINGORGANIZATION())) {
            changes.append("采购组织形式, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getPROPERTY(), newAsset.getPROPERTY())) {
            changes.append("产权单位, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getVEHICLENO(), newAsset.getVEHICLENO())) {
            changes.append("车牌号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSFACTORYNO(), newAsset.getASSETSFACTORYNO())) {
            changes.append("厂牌型号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getFDJH(), newAsset.getFDJH())) {
            changes.append("发动机号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getCJHM(), newAsset.getCJHM())) {
            changes.append("车架号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getVEHEXH(), newAsset.getVEHEXH())) {
            changes.append("排气量, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSPRODAREA(), newAsset.getASSETSPRODAREA())) {
            changes.append("车辆产地, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getLONGMILES(), newAsset.getLONGMILES())) {
            changes.append("行驶里程, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSMANAGER(), newAsset.getASSETSMANAGER())) {
            changes.append("权属所有人, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getASSETSDISPENSEDATE(), newAsset.getASSETSDISPENSEDATE())) {
            changes.append("发证时间, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSORGANIZATION(), newAsset.getASSETSORGANIZATION())) {
            changes.append("编制情况, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getVEHPURPOSE(), newAsset.getVEHPURPOSE())) {
            changes.append("分类用途, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSCULGRADE(), newAsset.getASSETSCULGRADE())) {
            changes.append("文物等级, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getSERIALNUM(), newAsset.getSERIALNUM())) {
            changes.append("卡片序号, ");
            hasChanges = true;
        }


        if (!Objects.equals(oldAsset.getSZLC(), newAsset.getSZLC())) {
            changes.append("所在楼层, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getCARDID(), newAsset.getCARDID())) {
            changes.append("CardID, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getKPJE(), newAsset.getKPJE())) {
            changes.append("卡片金额, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getKPJE(), newAsset.getKPJE())) {
            changes.append("卡片金额, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getISCZ(), newAsset.getISCZ())) {
            changes.append("财政资产, ");
            hasChanges = true;
        }


        if (!Objects.equals(oldAsset.getASSETSSERIALNUMBER(), newAsset.getASSETSSERIALNUMBER())) {
            changes.append("序列号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getASSETSSUPPLIER(), newAsset.getASSETSSUPPLIER())) {
            changes.append("生产厂家, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getFACTORYNO(), newAsset.getFACTORYNO())) {
            changes.append("出厂编号, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getMANAGENO(), newAsset.getMANAGENO())) {
            changes.append("管理编号, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getPARAMETERS(), newAsset.getPARAMETERS())) {
            changes.append("参数, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getTRACEABILITY(), newAsset.getTRACEABILITY())) {
            changes.append("溯源周期, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getCALIBRATIONDATE(), newAsset.getCALIBRATIONDATE())) {
            changes.append("检定校准日期, ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getVALIDDATE(), newAsset.getVALIDDATE())) {
            changes.append("有效期至, ");
            hasChanges = true;
        }

        if (!Objects.equals(oldAsset.getPROBELONG(), newAsset.getPROBELONG())) {
            changes.append("所属项目, ");
            hasChanges = true;
        }
        if(!Objects.equals(oldAsset.getCLASSIFICATION(), newAsset.getCLASSIFICATION())){
            changes.append("密级， ");
            hasChanges = true;
        }
        if (!Objects.equals(oldAsset.getISSUEDATE(), newAsset.getISSUEDATE())) {
            changes.append("配发日期, ");
            hasChanges = true;
        }

        if (hasChanges) {
            Log.d("AssetsUpdate", changes.toString());
        } else {
            Log.d("AssetsUpdate", "没有修改任何字段");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQESTCODE_WAITINGBLUETOOTH) {
                if (!isPrinterConnected()) {
                    showShortText("打印机连接失败！");
                    return;
                }
                //printAssets(editACCTSUITEID.getText().toString(), assetsCreating.getBARCODEID(), assetsCreating.getASSETSNAME(), StringUtils.isNullOrEmpty(assetsCreating.getASSETSUSER()) ? assetsCreating.getCUSTODIAN() : assetsCreating.getASSETSUSER());
                printAssets(editACCTSUITEID.getText().toString(), assetsCreating);
            }
        }
    }

}
