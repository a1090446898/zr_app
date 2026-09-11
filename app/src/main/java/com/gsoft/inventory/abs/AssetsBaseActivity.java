package com.gsoft.inventory.abs;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import butterknife.BindArray;
import butterknife.BindDrawable;
import butterknife.BindView;
import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.view.OptionsPickerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.data.CategoryData;
import com.gsoft.inventory.service.new_assets.LoadCategoryTask;
import com.gsoft.inventory.utils.*;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author 10904
 */
public abstract class AssetsBaseActivity extends PrintCompatActivity {
    public QMUITopBar topBar;

    public int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    public Assets assetsCreating = null;
    public String[] ztArray = null;
    public String[] dwArray = null;
    public String[] bmArray = null;

   public Accounts loginAccount = null;

   @BindView(R.id.buttonAIPhoto)
   public Button buttonAIPhoto;

    /*资产类别*/
    @BindArray(R.array.zclb)
    public String[] zclbArray;

    /*资产类别新*/
    @BindArray(R.array.zclbNew)
    public String[] zclbNewArray;
    /*折旧状态*/
    @BindArray(R.array.zjzt)
    public String[] zjztArray;
    /*取得方式*/
    @BindArray(R.array.qdfs)
    public String[] qdfsArray;
    /*使用状况*/
    @BindArray(R.array.syzk)
    public String[] syzkArray;
    /*文物等级*/
    @BindArray(R.array.wwdj)
    public String[] wwdjArray;
    /*编制情况*/
    @BindArray(R.array.bzqk)
    public String[] bzqkArray;
    /*分类用途*/
    @BindArray(R.array.flyt)
    public String[] flytArray;
    /*采购组织形式*/
    @BindArray(R.array.cgzzxs)
    public String[] cgzzxsArray;
    /**
     * 溯源方式
     */
    @BindArray(R.array.syfs)
    public String[] syfsArray;

    @BindArray(R.array.classifications)
    public String[] classifications;

    @BindArray(R.array.osArray)
    public String[] osArray;

    @BindArray(R.array.sampleStateFields)
    public String[] sampleStateFields;

    /*设备用途*/
    @BindArray(R.array.devicePurposeFields)
    public String[] devicePurposeFields;

    /*网络类型*/
    @BindArray(R.array.netTypeFields)
    public String[] netTypeFields;

    /*是否公物仓*/
    @BindArray(R.array.gwcFields)
    public String[] gwcFields;

    /*特种设备*/
    @BindArray(R.array.specialDeviceFields)
    public String[] specialDeviceFields;

    /*会计账簿*/
    @BindArray(R.array.accountBookFields)
    public String[] accountBookFields;

    /*下发情况*/
    @BindArray(R.array.downInfoFields)
    public String[] downInfoFields;

    /*设备分类*/
    @BindArray(R.array.deviceTypeFields)
    public String[] deviceTypeFields;

    /*条码编号*/
    @BindView(R.id.editBARCODEID)
    public EditText editBARCODEID;
    /*CardID*/
    @BindView(R.id.editCARDID)
    public EditText editCARDID;
    /*取得方式*/
    @BindView(R.id.editGETMODE)
    public EditText editGETMODE;
    /*备注*/
    @BindView(R.id.editREMARKS)
    public EditText editREMARKS;
    /*产权单位*/
    @BindView(R.id.editPROPERTY)
    public EditText editPROPERTY;
    /*发动机号*/
    @BindView(R.id.editFDJH)
    public EditText editFDJH;
    /*所在楼层，空值*/
    @BindView(R.id.editSZLC)
    public EditText editSZLC;
    /*使用(保管)人*/
    @BindView(R.id.editCUSTODIAN)
    public AutoCompleteTextView editCUSTODIAN;
    /*车架号*/
    @BindView(R.id.editCJHM)
    public EditText editCJHM;
    /*账套编号*/
    @BindView(R.id.editACCTSUITEID)
    public EditText editACCTSUITEID;
    /*卡片序号*/
    @BindView(R.id.editSERIALNUM)
    public EditText editSERIALNUM;
    /*资产类别*/
    @BindView(R.id.editASSETSTYPE)
    public EditText editASSETSTYPE;
    /*资产分类代码*/
    @BindView(R.id.editASSETSSORTCODE)
    public EditText editASSETSSORTCODE;
    /*计量单位*/
    @BindView(R.id.editASSETSMEASURE)
    public EditText editASSETSMEASURE;
    /*资产编号*/
    @BindView(R.id.editSERIALCODE)
    public EditText editSERIALCODE;
    /*资产名称*/
    @BindView(R.id.editASSETSNAME)
    public EditText editASSETSNAME;
    /*规格*/
    @BindView(R.id.editASSETSMODEL)
    public EditText editASSETSMODEL;
    /*型号*/
    @BindView(R.id.editASSETSSTANDARD)
    public EditText editASSETSSTANDARD;
    /*取得日期*/
    @BindView(R.id.editASSETSUSEDATE)
    public EditText editASSETSUSEDATE;
    /*保修截止日期*/
    @BindView(R.id.editASSETSENDUSEDATE)
    public EditText editASSETSENDUSEDATE;
    /*预计使用月份*/
    @BindView(R.id.editYEARCOUNT)
    public EditText editYEARCOUNT;
    /*使用状况*/
    @BindView(R.id.editUSESTATE)
    public EditText editUSESTATE;
    /*折旧状态*/
    @BindView(R.id.editDPRECIATIONTYPE)
    public EditText editDPRECIATIONTYPE;
    /*存放地点*/
    @BindView(R.id.editASSETSLAYADD)
    public EditText editASSETSLAYADD;
    /*资产位置*/
    @BindView(R.id.editGROUNDSETPOS)
    public EditText editGROUNDSETPOS;
    /*使用人*/
    @BindView(R.id.editASSETSUSER)
    public AutoCompleteTextView editASSETSUSER;
    /*使用部门,名称*/
    @BindView(R.id.editASSETSDEPT)
    public AutoCompleteTextView editASSETSDEPT;

    /*所属处室*/
    @BindView(R.id.editDEPARTMENT)
    public AutoCompleteTextView editDEPARTMENT;
    /*是否公物仓*/
    @BindView(R.id.editISGWC)
    public EditText editISGWC;
    /*卡片金额-原值，设为空值*/
    @BindView(R.id.editKPJE)
    public TextView editKPJE;
    /*累计折旧*/
    @BindView(R.id.editDPRECIATIONADD)
    public EditText editDPRECIATIONADD;
    /*资产原值*/
    @BindView(R.id.editASSETSCURRPRICE)
    public EditText editASSETSCURRPRICE;
    /*当前累计折旧*/
    @BindView(R.id.editCURRDPRECIATIONADD)
    public EditText editCURRDPRECIATIONADD;
    /*净值*/
    @BindView(R.id.editLEFTPRICE)
    public TextView editLEFTPRICE;
    /*管理部门*/
    @BindView(R.id.editGROUNDMANAGEDEPT)
    public EditText editGROUNDMANAGEDEPT;
    /*文物等级*/
    @BindView(R.id.editASSETSCULGRADE)
    public EditText editASSETSCULGRADE;
    /*车辆产地*/
    @BindView(R.id.editASSETSPRODAREA)
    public EditText editASSETSPRODAREA;
    /*车牌号*/
    @BindView(R.id.editVEHICLENO)
    public EditText editVEHICLENO;
    /*发证时间*/
    @BindView(R.id.editASSETSDISPENSEDATE)
    public EditText editASSETSDISPENSEDATE;
    /*权属所有人*/
    @BindView(R.id.editASSETSMANAGER)
    public EditText editASSETSMANAGER;
    /*厂牌型号*/
    @BindView(R.id.editASSETSFACTORYNO)
    public EditText editASSETSFACTORYNO;
    /*排气量*/
    @BindView(R.id.editVEHEXH)
    public EditText editVEHEXH;
    /*行驶里程*/
    @BindView(R.id.editLONGMILES)
    public EditText editLONGMILES;
    /*分类用途*/
    @BindView(R.id.editVEHPURPOSE)
    public EditText editVEHPURPOSE;
    /*编制情况*/
    @BindView(R.id.editASSETSORGANIZATION)
    public EditText editASSETSORGANIZATION;
    /*数量*/
    @BindView(R.id.editASSETSNUM)
    public EditText editASSETSNUM;
    /*录入人*/
    @BindView(R.id.editASSETSWRITER)
    public EditText editASSETSWRITER;
    /*录入日期*/
    @BindView(R.id.editASSETSWRITERDATE)
    public EditText editASSETSWRITERDATE;
    /*采购组织形式*/
    @BindView(R.id.editPURCHASINGORGANIZATION)
    public EditText editPURCHASINGORGANIZATION;
    /*品牌*/
    @BindView(R.id.editBRAND)
    public EditText editBRAND;

    @BindView(R.id.editPDZT)
    public EditText editPDZT;
    @BindView(R.id.toggleISCZ)
    public Switch toggleISCZ;

    @BindView(R.id.editXLH)
    public EditText editXLH;
    @BindView(R.id.editSCCJ)
    public EditText editSCCJ;
    @BindView(R.id.editCCBH)
    public EditText editCCBH;

    /*出厂日期*/
    @BindView(R.id.editFACTORYDATE)
    public EditText editFACTORYDATE;

    @BindView(R.id.editGLBH)
    public EditText editGLBH;

    /*特种设备*/
    @BindView(R.id.editSPECIALDEVICE)
    public EditText editSPECIALDEVICE;
    /*设备注册代码*/
    @BindView(R.id.editDEVICECODE)
    public EditText editDEVICECODE;
    /*特种设备出厂编号*/
    @BindView(R.id.editSPECIALFACTORYNO)
    public EditText editSPECIALFACTORYNO;
    /*使用登记证编号*/
    @BindView(R.id.editCERTIFICATENUMBER)
    public EditText editCERTIFICATENUMBER;

    @BindView(R.id.editCANSHU)
    public EditText editCANSHU;
    @BindView(R.id.editSYZQ)
    public EditText editSYZQ;
    @BindView(R.id.editSYFS)
    public EditText editSYFS;
    @BindView(R.id.editJDJZRQ)
    public EditText editJDJZRQ;
    @BindView(R.id.editYXQZ)
    public EditText editYXQZ;
    @BindView(R.id.editSSXM)
    public EditText editSSXM;

    @BindView(R.id.editPRINTNO)
    public EditText editPRINTNO;
    @BindView(R.id.editPRO)
    public EditText editPRO;
    @BindView(R.id.editACCOUNTBOOK)
    public EditText editACCOUNTBOOK;
    @BindView(R.id.editDOWNINFO)
    public EditText editDOWNINFO;
    @BindView(R.id.editSUMCOMPLETEMONTH)
    public EditText editSUMCOMPLETEMONTH;
    @BindView(R.id.editCOMPLETEMONTH)
    public EditText editCOMPLETEMONTH;
    @BindView(R.id.editMONDEPREVALUE)
    public EditText editMONDEPREVALUE;
    @BindView(R.id.editDEVICETYPE)
    public EditText editDEVICETYPE;

    @BindView(R.id.editVENDOR)
    public EditText editVENDOR;

    @BindView(R.id.editClassification)
    public TextView editClassification;

    @BindView(R.id.editIsSueDate)
    public TextView editIsSueDate;


    @BindDrawable(R.drawable.icon_right)
    public Drawable icon_right;

    @BindView(R.id.editBOOKKEEPINGTIME)
    public EditText editBOOKKEEPINGTIME;

    @BindView(R.id.editDWCODE)
    public EditText editDWCODE;


    @BindView(R.id.editVOUCHERNO)
    public EditText editVOUCHERNO;

    @BindView(R.id.editSAMPLDATE)
    public EditText editSAMPLDATE;

    @BindView(R.id.editSAMPLCODE)
    public EditText editSAMPLCODE;

    @BindView(R.id.editMONITORPRO)
    public EditText editMONITORPRO;

    @BindView(R.id.editSAVEMETHOD)
    public EditText editSAVEMETHOD;

    @BindView(R.id.editSAMPLESTATE)
    public EditText editSAMPLESTATE;

    @BindView(R.id.editCONCENTRATION)
    public EditText editCONCENTRATION;

    @BindView(R.id.editPREPARER)
    public EditText editPREPARER;

    @BindView(R.id.editMEDIA)
    public EditText editMEDIA;

    @BindView(R.id.editSTORAGENEED)
    public EditText editSTORAGENEED;


    @BindView(R.id.editSAMPLEUSER)
    public EditText editSAMPLEUSER;


    @BindView(R.id.editHARDID)
    public EditText editHARDID;

    @BindView(R.id.editIP)
    public EditText editIP;

    @BindView(R.id.editMAC)
    public EditText editMAC;

    @BindView(R.id.editINTERNETID)
    public EditText editINTERNETID;


    @BindView(R.id.editPORT)
    public EditText editPORT;

    @BindView(R.id.editPURPOSE)
    public EditText editPURPOSE;

    @BindView(R.id.editNETTYPE)
    public EditText editNETTYPE;


    @BindView(R.id.editHARDSN)
    public EditText editHARDSN;

    @BindView(R.id.editOS)
    public EditText editOS;

    @BindView(R.id.editINSTALLDATE)
    public EditText editINSTALLDATE;


    @BindView(R.id.buttonSave)
    @Nullable
    public QMUIButton buttonSave;

    @BindView(R.id.buttonPrintLabel)
    @Nullable
    public QMUIButton buttonPrintLabel;

    @BindView(R.id.buttonCopy)
    @Nullable
    public QMUIButton buttonCopy;

    public List<AssetsCategory> rootCategoryArray = null;
    public List<List<AssetsCategory>> secondCategoryArray = null;
    public List<List<List<AssetsCategory>>> thirdCategoryArray = null;

    public OptionsPickerView pvOptions;


    public void initData(){

        loginAccount = application.getLoginAccount();
        if (loginAccount == null) {
            showShortText("登录超时，请重新登录！");
            return;
        }
        ztArray = new String[SysConfig.getZTList().size()];
        SysConfig.getZTList().toArray(ztArray);
        // 获取部门名称
        List<String> danweiList = AccountHelper.getDanweiList(loginAccount.getZTBH());
        dwArray = new String[danweiList.size()];
        danweiList.toArray(dwArray);
        bmArray = new String[AccountHelper.getDepartList(loginAccount.getZTBH()).size()];
        AccountHelper.getDepartList(loginAccount.getZTBH()).toArray(bmArray);

    }

    public void iconSetting(){
        //必须设置图片的大小否则没有作用
        icon_right.setBounds(0, 0, 60, 60);
        editACCTSUITEID.setCompoundDrawables(null, null, icon_right, null);
        editASSETSTYPE.setCompoundDrawables(null, null, icon_right, null);
        editDPRECIATIONTYPE.setCompoundDrawables(null, null, icon_right, null);
        editASSETSSORTCODE.setCompoundDrawables(null, null, icon_right, null);
        editVEHPURPOSE.setCompoundDrawables(null, null, icon_right, null);
        editUSESTATE.setCompoundDrawables(null, null, icon_right, null);
        editASSETSORGANIZATION.setCompoundDrawables(null, null, icon_right, null);
        editPURCHASINGORGANIZATION.setCompoundDrawables(null, null, icon_right, null);
        editASSETSCULGRADE.setCompoundDrawables(null, null, icon_right, null);
        editGETMODE.setCompoundDrawables(null, null, icon_right, null);
        editPROPERTY.setCompoundDrawables(null, null, icon_right, null);
        editGROUNDMANAGEDEPT.setCompoundDrawables(null, null, icon_right, null);
        editASSETSUSEDATE.setCompoundDrawables(null, null, icon_right, null);
        editASSETSENDUSEDATE.setCompoundDrawables(null, null, icon_right, null);
        editASSETSDISPENSEDATE.setCompoundDrawables(null, null, icon_right, null);
        editSYFS.setCompoundDrawables(null, null, icon_right, null);
        editYXQZ.setCompoundDrawables(null, null, icon_right, null);
        editJDJZRQ.setCompoundDrawables(null, null, icon_right, null);
        editIsSueDate.setCompoundDrawables(null, null, icon_right, null);
        editClassification.setCompoundDrawables(null, null, icon_right, null);
        editACCOUNTBOOK.setCompoundDrawables(null, null, icon_right, null);
        editDOWNINFO.setCompoundDrawables(null, null, icon_right, null);
        editDEVICETYPE.setCompoundDrawables(null, null, icon_right, null);
        editBOOKKEEPINGTIME.setCompoundDrawables(null, null, icon_right, null);
        editSAMPLDATE.setCompoundDrawables(null, null, icon_right, null);
        editSAMPLESTATE.setCompoundDrawables(null, null, icon_right, null);
        editPURPOSE.setCompoundDrawables(null, null, icon_right, null);
        editNETTYPE.setCompoundDrawables(null, null, icon_right, null);

        editISGWC.setCompoundDrawables(null, null, icon_right, null);
        editSPECIALDEVICE.setCompoundDrawables(null, null, icon_right, null);
//        editOS.setCompoundDrawables(null, null, icon_right, null);
        editINSTALLDATE.setCompoundDrawables(null, null, icon_right, null);
    }

    public void viewStateSetting(){
        editDPRECIATIONTYPE.setFocusable(false);
        editDPRECIATIONTYPE.setFocusableInTouchMode(false);
        editGROUNDMANAGEDEPT.setFocusable(false);
        editGROUNDMANAGEDEPT.setFocusableInTouchMode(false);
        editACCTSUITEID.setFocusable(false);
        editACCTSUITEID.setFocusableInTouchMode(false);
        editASSETSTYPE.setFocusable(false);
        editASSETSTYPE.setFocusableInTouchMode(false);
        editASSETSSORTCODE.setFocusable(false);
        editASSETSSORTCODE.setFocusableInTouchMode(false);
        editVEHPURPOSE.setFocusable(false);
        editVEHPURPOSE.setFocusableInTouchMode(false);
        editUSESTATE.setFocusable(false);
        editUSESTATE.setFocusableInTouchMode(false);
        editASSETSORGANIZATION.setFocusable(false);
        editASSETSORGANIZATION.setFocusableInTouchMode(false);
        editPURCHASINGORGANIZATION.setFocusable(false);
        editPURCHASINGORGANIZATION.setFocusableInTouchMode(false);
        editASSETSCULGRADE.setFocusable(false);
        editASSETSCULGRADE.setFocusableInTouchMode(false);
        editGETMODE.setFocusable(false);
        editGETMODE.setFocusableInTouchMode(false);
        editPROPERTY.setFocusable(false);
        editPROPERTY.setFocusableInTouchMode(false);
        editASSETSUSEDATE.setFocusable(false);
        editASSETSUSEDATE.setFocusableInTouchMode(false);
        editASSETSENDUSEDATE.setFocusable(false);
        editASSETSENDUSEDATE.setFocusableInTouchMode(false);
        editASSETSDISPENSEDATE.setFocusable(false);
        editASSETSDISPENSEDATE.setFocusableInTouchMode(false);
        editSYFS.setFocusableInTouchMode(false);
        editSYFS.setFocusable(false);
        editYXQZ.setFocusableInTouchMode(false);
        editYXQZ.setFocusable(false);
        editJDJZRQ.setFocusableInTouchMode(false);
        editJDJZRQ.setFocusable(false);

        editFACTORYDATE.setFocusableInTouchMode(false);
        editFACTORYDATE.setFocusable(false);

        editSAMPLDATE.setFocusable(false);
        editSAMPLESTATE.setFocusable(false);
        editPURPOSE.setFocusable(false);
        editPURPOSE.setFocusableInTouchMode(false);
        editNETTYPE.setFocusable(false);
        editNETTYPE.setFocusableInTouchMode(false);
        editACCOUNTBOOK.setFocusable(false);
        editACCOUNTBOOK.setFocusableInTouchMode(false);
        editDOWNINFO.setFocusable(false);
        editDOWNINFO.setFocusableInTouchMode(false);
        editDEVICETYPE.setFocusable(false);
        editDEVICETYPE.setFocusableInTouchMode(false);
        editBOOKKEEPINGTIME.setFocusable(false);
        editBOOKKEEPINGTIME.setFocusableInTouchMode(false);
        editISGWC.setFocusable(false);
        editISGWC.setFocusableInTouchMode(false);
        editSPECIALDEVICE.setFocusable(false);
        editSPECIALDEVICE.setFocusableInTouchMode(false);
        editINSTALLDATE.setFocusable(false);
    }


    /**
     * 公共方法：异步加载分类数据
     */
    public void asyncCategory(String assetsType) {
        LoadCategoryTask task = new LoadCategoryTask(mContext, assetsType);
        AsyncManager.getInstance().submitWithCallback(task, result -> {
            mainHandler.post(() -> {
                if (result.isSuccess()) {
                    CategoryData data = result.getData();
                    if (data == null) return;
                    rootCategoryArray = data.rootCategories;
                    secondCategoryArray = data.secondCategories;
                    thirdCategoryArray = data.thirdCategories;
                    // 调用公共初始化方法
                    initCategoryPicker();
                } else {
                    showShortText("后台加载分类数据失败……");
                }
            });
        });
    }

    /**
     * 公共方法：初始化分类选择器
     */
    public void initCategoryPicker() {
        pvOptions = new OptionsPickerBuilder(this, (options1, options2, options3, v) -> {
            AssetsCategory selectedCategory = thirdCategoryArray.get(options1).get(options2).get(options3);
            assetsCreating.setASSETSSORTCODE(selectedCategory.getCODE());
            editASSETSSORTCODE.setText(selectedCategory.getNAME()); // 假设子类包含此控件（需保证控件ID一致）
            editASSETSNAME.setText(selectedCategory.getNAME());
            editASSETSMEASURE.setText(selectedCategory.getDEFUNIT() + "");
        })
                .setTitleText("分类选择")
                .setContentTextSize(20)
                .setDividerColor(Color.LTGRAY)
                .setSelectOptions(0, 1)
                .isRestoreItem(true)
                .isCenterLabel(false)
                .setOptionsSelectChangeListener((options1, options2, options3) -> {})
                .build();
    }

    public String getZichanStr() {
        return StringUtils.isNullOrEmpty(editASSETSCURRPRICE.getText().toString())
                ? (StringUtils.isNullOrEmpty(editLEFTPRICE.getText().toString())
                ? editKPJE.getText().toString()
                : editLEFTPRICE.getText().toString())
                : editASSETSCURRPRICE.getText().toString();
    }


    /**
     * 条码编号为空时候自动生成
     * 资产编号为空的时候赋值条码编号
     * @param barCode
     */
    public void setBarCode(String barCode) {
        // 条码编号
        String barCodeInputStr = editBARCODEID.getText().toString();
        // 资产编号
        String serialInputStr = editSERIALCODE.getText().toString();

        // 对条码编号的判断
        if(StringUtils.isNullOrEmpty(barCodeInputStr)){
            assetsCreating.setBARCODEID(barCode);
            editBARCODEID.setText(barCode);
            barCodeInputStr = barCode;
        }
        else{
            assetsCreating.setBARCODEID(barCodeInputStr);
        }

        // 对卡片比编号的判断
        if(StringUtils.isNullOrEmpty(serialInputStr)){
            assetsCreating.setSERIALCODE(barCodeInputStr);
            editSERIALCODE.setText(barCodeInputStr);
        }
        else{
            assetsCreating.setSERIALCODE(serialInputStr);
        }

    }

    public void setBaseAssetProperties(){
        String barCode = SysConfig.getNewAssetsBarcodeByDevice(application.getDeviceNo());
        setBarCode(barCode);
        assetsCreating.setCARDID(editCARDID.getText().toString());
        assetsCreating.setPROPERTY(editPROPERTY.getText().toString());
        assetsCreating.setFDJH(editFDJH.getText().toString());
        assetsCreating.setSZLC(editSZLC.getText().toString());
        assetsCreating.setCUSTODIAN(editCUSTODIAN.getText().toString());
        assetsCreating.setASSETSUSER(editASSETSUSER.getText().toString());
        assetsCreating.setASSETSDEPT(editASSETSDEPT.getText().toString());
        assetsCreating.setCJHM(editCJHM.getText().toString());
        assetsCreating.setSERIALNUM(editSERIALNUM.getText().toString());
        assetsCreating.setASSETSMEASURE(editASSETSMEASURE.getText().toString());
        assetsCreating.setASSETSNAME(editASSETSNAME.getText().toString());
        assetsCreating.setASSETSMODEL(editASSETSMODEL.getText().toString());
        assetsCreating.setASSETSSTANDARD(editASSETSSTANDARD.getText().toString());
        assetsCreating.setASSETSUSEDATE(editASSETSUSEDATE.getText().toString());
        assetsCreating.setASSETSENDUSEDATE(editASSETSENDUSEDATE.getText().toString());
        assetsCreating.setYEARCOUNT(editYEARCOUNT.getText().toString());
        assetsCreating.setASSETSLAYADD(editASSETSLAYADD.getText().toString());
        assetsCreating.setGROUNDSETPOS(editGROUNDSETPOS.getText().toString());
        assetsCreating.setASSETSDEPT(editASSETSDEPT.getText().toString());
        String zichanStr = getZichanStr();
        assetsCreating.setASSETSCURRPRICE(zichanStr);
        assetsCreating.setLEFTPRICE(zichanStr);
        assetsCreating.setKPJE(zichanStr);
        editASSETSCURRPRICE.setText(zichanStr);
        editLEFTPRICE.setText(zichanStr);
        editASSETSCURRPRICE.setText(zichanStr);
        assetsCreating.setDPRECIATIONADD(editDPRECIATIONADD.getText().toString());
        assetsCreating.setCURRDPRECIATIONADD(editCURRDPRECIATIONADD.getText().toString());
        assetsCreating.setASSETSPRODAREA(editASSETSPRODAREA.getText().toString());
        assetsCreating.setVEHICLENO(editVEHICLENO.getText().toString());
        assetsCreating.setASSETSDISPENSEDATE(editASSETSDISPENSEDATE.getText().toString());
        assetsCreating.setASSETSMANAGER(editASSETSMANAGER.getText().toString());
        assetsCreating.setASSETSFACTORYNO(editASSETSFACTORYNO.getText().toString());
        assetsCreating.setVEHEXH(editVEHEXH.getText().toString());
        assetsCreating.setLONGMILES(editLONGMILES.getText().toString());
        assetsCreating.setASSETSNUM(Integer.parseInt(editASSETSNUM.getText().toString()));
        assetsCreating.setASSETSWRITER(application.getZcqcyCode());
        String assetsWriterDate = editASSETSWRITERDATE.getText().toString();
        if(StringUtils.isNullOrEmpty(assetsWriterDate)){
            assetsWriterDate = DataConvert.FormatDate2Day();
        }
        assetsCreating.setASSETSWRITERDATE(assetsWriterDate);
        assetsCreating.setBRAND(editBRAND.getText().toString());
        assetsCreating.setISCZ(toggleISCZ.isChecked() ? "02" : "01");
        assetsCreating.setASSETSSERIALNUMBER(editXLH.getText().toString());
        assetsCreating.setASSETSSUPPLIER(editSCCJ.getText().toString());
        assetsCreating.setVENDOR(editVENDOR.getText().toString());
        assetsCreating.setFACTORYNO(editCCBH.getText().toString());
        assetsCreating.setFACTORYDATE(editFACTORYDATE.getText().toString());
        assetsCreating.setMANAGENO(editGLBH.getText().toString());
        /*特种设备：01是 02否，默认空*/
        String specialDeviceStr = editSPECIALDEVICE.getText().toString();
        assetsCreating.setSPECIALDEVICE("");
        for (int i = 0; i < specialDeviceFields.length; i++) {
            if (specialDeviceFields[i].equals(specialDeviceStr)) {
                assetsCreating.setSPECIALDEVICE(String.format("%02d", i + 1));
                break;
            }
        }
        assetsCreating.setDEVICECODE(editDEVICECODE.getText().toString());
        assetsCreating.setSPECIALFACTORYNO(editSPECIALFACTORYNO.getText().toString());
        assetsCreating.setCERTIFICATENUMBER(editCERTIFICATENUMBER.getText().toString());
        assetsCreating.setPARAMETERS(editCANSHU.getText().toString());
        assetsCreating.setTRACEABILITY(editSYZQ.getText().toString());
        assetsCreating.setCALIBRATIONDATE(editJDJZRQ.getText().toString());
        assetsCreating.setVALIDDATE(editYXQZ.getText().toString());
        assetsCreating.setPROBELONG(editSSXM.getText().toString());
        assetsCreating.setPRINTNO(editPRINTNO.getText().toString());
        assetsCreating.setPRO(editPRO.getText().toString());
        String accountBookStr = editACCOUNTBOOK.getText().toString();
        for (int i = 0; i < accountBookFields.length; i++) {
            if (accountBookFields[i].equals(accountBookStr)) {
                assetsCreating.setACCOUNTBOOK(String.format("%02d", i + 1));
                break;
            }
        }
        String downInfoStr = editDOWNINFO.getText().toString();
        for (int i = 0; i < downInfoFields.length; i++) {
            if (downInfoFields[i].equals(downInfoStr)) {
                assetsCreating.setDOWNINFO(String.format("%02d", i + 1));
                break;
            }
        }
        String deviceTypeStr = editDEVICETYPE.getText().toString();
        for (int i = 0; i < deviceTypeFields.length; i++) {
            if (deviceTypeFields[i].equals(deviceTypeStr)) {
                assetsCreating.setDEVICETYPE(String.format("%02d", i + 1));
                break;
            }
        }
        assetsCreating.setSUMCOMPLETEMONTH(editSUMCOMPLETEMONTH.getText().toString());
        assetsCreating.setCOMPLETEMONTH(editCOMPLETEMONTH.getText().toString());
        assetsCreating.setMONDEPREVALUE(editMONDEPREVALUE.getText().toString());
        assetsCreating.setBOOKKEEPINGTIME(editBOOKKEEPINGTIME.getText().toString());
        assetsCreating.setVOUCHERNO(editVOUCHERNO.getText().toString());
        assetsCreating.setISSUEDATE(editIsSueDate.getText().toString());
        assetsCreating.setISUPLOAD("02");
        assetsCreating.setREMARKS(editREMARKS.getText().toString());

        assetsCreating.setDWCODE(editDWCODE.getText().toString());
        assetsCreating.setSAMPLDATE(editSAMPLDATE.getText().toString());
        assetsCreating.setSAMPLCODE(editSAMPLCODE.getText().toString());
        assetsCreating.setMONITORPRO(editMONITORPRO.getText().toString());
        assetsCreating.setSAVEMETHOD(editSAVEMETHOD.getText().toString());
        String samplStateStr = editSAMPLESTATE.getText().toString();
        for (int i = 0; i < sampleStateFields.length; i++) {
            if (sampleStateFields[i].equals(samplStateStr)) {
                assetsCreating.setSAMPLESTATE("0" + (i+1));
                break;
            }
        }
        assetsCreating.setCONCENTRATION(editCONCENTRATION.getText().toString());
        assetsCreating.setPREPARER(editPREPARER.getText().toString());
        assetsCreating.setMEDIA(editMEDIA.getText().toString());
        assetsCreating.setSTORAGENEED(editSTORAGENEED.getText().toString());


        assetsCreating.setSAMPLEUSER(editSAMPLEUSER.getText().toString());

        assetsCreating.setHARDID(editHARDID.getText().toString());
        assetsCreating.setIP(editIP.getText().toString());
        assetsCreating.setMAC(editMAC.getText().toString());
        assetsCreating.setINTERNETID(editINTERNETID.getText().toString());
        assetsCreating.setPORT(editPORT.getText().toString());
        assetsCreating.setPURPOSE(editPURPOSE.getText().toString());
        String purposeStr = editPURPOSE.getText().toString();
        for (int i = 0; i < devicePurposeFields.length; i++) {
            if (devicePurposeFields[i].equals(purposeStr)) {
                assetsCreating.setPURPOSE(i == devicePurposeFields.length - 1 ? "99" : String.format("%02d", i + 1));
                break;
            }
        }
        String netTypeStr = editNETTYPE.getText().toString();
        for (int i = 0; i < netTypeFields.length; i++) {
            if (netTypeFields[i].equals(netTypeStr)) {
                assetsCreating.setNETTYPE(i == netTypeFields.length - 1 ? "99" : String.format("%02d", i + 1));
                break;
            }
        }
        assetsCreating.setHARDSN(editHARDSN.getText().toString());
        assetsCreating.setOS(editOS.getText().toString());
        assetsCreating.setINSTALLDATE(editINSTALLDATE.getText().toString());
        String isgwcStr = editISGWC.getText().toString();
        for (int i = 0; i < gwcFields.length; i++) {
            if (gwcFields[i].equals(isgwcStr)) {
                assetsCreating.setISGWC(String.format("%02d", i + 1));
                break;
            }
        }
        // 所属处室
        assetsCreating.setDEPARTMENT(editDEPARTMENT.getText().toString());
    }

    /**
     * 公共方法：日期选择器通用逻辑（子类可直接调用）
     */
    public void setupDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        final int mYear = calendar.get(Calendar.YEAR);
        final int mMonth = calendar.get(Calendar.MONTH);
        final int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        editText.setOnClickListener(v -> new DatePickerDialog(mContext, (datePicker, year, month, dayOfMonth) -> {
            Calendar tCalendar = Calendar.getInstance();
            tCalendar.set(year, month, dayOfMonth);
            editText.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
        }, mYear, mMonth, mDay).show());
    }

    public boolean checkRequiredFields() {
        if (StringUtils.isNullOrEmpty(assetsCreating.getACCTSUITEID())) {
            showShortText("请选择账套");
            return false;
        }
        if (StringUtils.isNullOrEmpty(assetsCreating.getASSETSTYPE())) {
            showShortText("请选择资产类别");
            return false;
        }
        if (StringUtils.isNullOrEmpty(editCUSTODIAN.getText().toString())) {
            showShortText("请填写管理人");
            return false;
        }
        if (StringUtils.isNullOrEmpty(editASSETSDEPT.getText().toString())) {
            showShortText("请选择使用部门");
            return false;
        }
        if (StringUtils.isNullOrEmpty(editASSETSLAYADD.getText().toString())) {
            showShortText("请填写存放地点");
            return false;
        }
        if (StringUtils.isNullOrEmpty(editGROUNDMANAGEDEPT.getText().toString())) {
            showShortText("请选择管理部门");
            return false;
        }
        if (StringUtils.isNullOrEmpty(editASSETSUSER.getText().toString())) {
            showShortText("请填写使用人");
            return false;
        }
        if (StringUtils.isNullOrEmpty(assetsCreating.getASSETSSORTCODE())) {
            showShortText("请选择资产分类");
            return false;
        }
        if (StringUtils.isNullOrEmpty(editASSETSNAME.getText().toString())) {
            showShortText("请填写资产名称");
            return false;
        }
        if (StringUtils.isNullOrEmpty(application.getPrinterDevice())) {
            showShortText("登录超时，请重新登录！");
            return false;
        }
        return true;
    }

    public void fillAssetsData() {
        /*条码编号*/
        editBARCODEID.setText(assetsCreating.getBARCODEID());
        /*CardID*/
        editCARDID.setText(assetsCreating.getCARDID());
        /*取得方式*/
        editGETMODE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getGETMODE(), qdfsArray));
        /*备注*/
        editREMARKS.setText(assetsCreating.getREMARKS());
        /*产权单位*/
        editPROPERTY.setText(assetsCreating.getPROPERTY());
        /*发动机号*/
        editFDJH.setText(assetsCreating.getFDJH());
        /*所在楼层，空值*/
        editSZLC.setText(assetsCreating.getSZLC());
        /*使用(保管)人*/
        editCUSTODIAN.setText(assetsCreating.getCUSTODIAN());
        /*车架号*/
        editCJHM.setText(assetsCreating.getCJHM());
        /*账套编号*/
        Accounts accounts = AccountHelper.getACCTSUITEID(assetsCreating.getACCTSUITEID());
        editACCTSUITEID.setText(accounts == null ? "" : accounts.getZTMC());
        /*卡片序号*/
        editSERIALNUM.setText(assetsCreating.getSERIALNUM());
        /*资产类别*/
        editASSETSTYPE.setText(AssetsCategoryHelper.getAssetsCategoryTitle(assetsCreating.getASSETSTYPE()));
        if (!StringUtils.isNullOrEmpty(assetsCreating.getASSETSTYPE())) {
            // 启动异步任务
            String assetsType = editASSETSTYPE.getText().toString();
            asyncCategory(assetsType);
        }
        /*资产分类代码*/
        AssetsCategory assetsCategory = AssetsCategoryHelper.getAssetsCategory(assetsCreating.getASSETSSORTCODE());
        editASSETSSORTCODE.setText(assetsCategory == null ? "" : assetsCategory.getNAME());
        /*计量单位*/
        editASSETSMEASURE.setText(assetsCreating.getASSETSMEASURE());
        /*资产编号*/
        editSERIALCODE.setText(assetsCreating.getSERIALCODE());
        /*资产名称*/
        editASSETSNAME.setText(assetsCreating.getASSETSNAME());
        /*规格*/
        editASSETSMODEL.setText(assetsCreating.getASSETSMODEL());
        /*型号*/
        editASSETSSTANDARD.setText(assetsCreating.getASSETSSTANDARD());
        /*取得日期*/
        editASSETSUSEDATE.setText(assetsCreating.getASSETSUSEDATE());
        /*保修截止日期*/
        editASSETSENDUSEDATE.setText(assetsCreating.getASSETSENDUSEDATE());
        /*预计使用月份*/
        editYEARCOUNT.setText(assetsCreating.getYEARCOUNT());
        /*使用状况*/
        editUSESTATE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getUSESTATE(), syzkArray));
        /*折旧状态*/
        editDPRECIATIONTYPE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getDPRECIATIONTYPE(), zjztArray));
        /*存放地点*/
        editASSETSLAYADD.setText(assetsCreating.getASSETSLAYADD());

        /*资产位置*/
        editGROUNDSETPOS.setText(assetsCreating.getGROUNDSETPOS());
        /*使用人*/
        editASSETSUSER.setText(assetsCreating.getASSETSUSER());
        /*使用部门*/
        editASSETSDEPT.setText(assetsCreating.getASSETSDEPT());
        /*所属处室*/
        editDEPARTMENT.setText(assetsCreating.getDEPARTMENT());
        /*是否公物仓*/
        editISGWC.setText(DictionaryHelper.getDictionaryName(assetsCreating.getISGWC(), gwcFields));
        /*卡片金额-原值，设为空值*/
        editKPJE.setText(assetsCreating.getKPJE());
        editKPJE.setEnabled(false);
        /*累计折旧*/
        editDPRECIATIONADD.setText(assetsCreating.getDPRECIATIONADD());
        /*资产原值*/
        editASSETSCURRPRICE.setText(assetsCreating.getASSETSCURRPRICE());
        /*当前累计折旧*/
        editCURRDPRECIATIONADD.setText(assetsCreating.getCURRDPRECIATIONADD());
        /*净值*/
        editLEFTPRICE.setText(assetsCreating.getLEFTPRICE());
        /*管理部门*/
        editGROUNDMANAGEDEPT.setText(AccountHelper.getDanweiName(application.getLoginAccount().getZTBH(), assetsCreating.getGROUNDMANAGEDEPT()));
        /*文物等级*/
        editASSETSCULGRADE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getASSETSCULGRADE(), wwdjArray));
        /*车辆产地*/
        editASSETSPRODAREA.setText(assetsCreating.getASSETSPRODAREA());
        /*车牌号*/
        editVEHICLENO.setText(assetsCreating.getVEHICLENO());
        /*发证时间*/
        editASSETSDISPENSEDATE.setText(assetsCreating.getASSETSDISPENSEDATE());
        /*权属所有人*/
        editASSETSMANAGER.setText(assetsCreating.getASSETSMANAGER());
        /*厂牌型号*/
        editASSETSFACTORYNO.setText(assetsCreating.getASSETSFACTORYNO());
        /*排气量*/
        editVEHEXH.setText(assetsCreating.getVEHEXH());
        /*行驶里程*/
        editLONGMILES.setText(assetsCreating.getLONGMILES());
        /*分类用途，暂时取消词典*/
        editVEHPURPOSE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getVEHPURPOSE(), flytArray));//
        /*编制情况*/
        editASSETSORGANIZATION.setText(DictionaryHelper.getDictionaryName(assetsCreating.getASSETSORGANIZATION(), bzqkArray));
        /*数量*/
        editASSETSNUM.setText(String.valueOf(assetsCreating.getASSETSNUM()));
        editASSETSNUM.setEnabled(true);
        /*录入人*/
        editASSETSWRITER.setText(AccountHelper.getUserName(assetsCreating.getASSETSWRITER()));
        /*录入日期*/
        editASSETSWRITERDATE.setText(assetsCreating.getASSETSWRITERDATE());
        /*采购组织形式*/
        editPURCHASINGORGANIZATION.setText(DictionaryHelper.getDictionaryName(assetsCreating.getPURCHASINGORGANIZATION(), cgzzxsArray));
        /*品牌*/
        editBRAND.setText(assetsCreating.getBRAND());
        editPDZT.setText(assetsCreating.getPDZT());
        toggleISCZ.setChecked("02".equals(assetsCreating.getISCZ()));

        editXLH.setText(assetsCreating.getASSETSSERIALNUMBER());
        editSCCJ.setText(assetsCreating.getASSETSSUPPLIER());
        editVENDOR.setText(assetsCreating.getVENDOR());
        editCCBH.setText(assetsCreating.getFACTORYNO());
        editFACTORYDATE.setText(assetsCreating.getFACTORYDATE());
        editGLBH.setText(assetsCreating.getMANAGENO());
        editSPECIALDEVICE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getSPECIALDEVICE(), specialDeviceFields));
        editDEVICECODE.setText(assetsCreating.getDEVICECODE());
        editSPECIALFACTORYNO.setText(assetsCreating.getSPECIALFACTORYNO());
        editCERTIFICATENUMBER.setText(assetsCreating.getCERTIFICATENUMBER());
        editCANSHU.setText(assetsCreating.getPARAMETERS());
        editSYZQ.setText(assetsCreating.getTRACEABILITY());
        //editSYFS.setText(assetsCreating.getTRACEABILITYPE());
        editSYFS.setText(DictionaryHelper.getDictionaryName(assetsCreating.getTRACEABILITYPE(), syfsArray));
        editJDJZRQ.setText(assetsCreating.getCALIBRATIONDATE());
        editYXQZ.setText(assetsCreating.getVALIDDATE());
        editSSXM.setText(assetsCreating.getPROBELONG());

        editPRINTNO.setText(assetsCreating.getPRINTNO());
        editPRO.setText(assetsCreating.getPRO());
        editACCOUNTBOOK.setText(DictionaryHelper.getDictionaryName(assetsCreating.getACCOUNTBOOK(), accountBookFields));
        editDOWNINFO.setText(DictionaryHelper.getDictionaryName(assetsCreating.getDOWNINFO(), downInfoFields));
        editSUMCOMPLETEMONTH.setText(assetsCreating.getSUMCOMPLETEMONTH());
        editCOMPLETEMONTH.setText(assetsCreating.getCOMPLETEMONTH());
        editMONDEPREVALUE.setText(assetsCreating.getMONDEPREVALUE());
        editDEVICETYPE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getDEVICETYPE(), deviceTypeFields));


        editIsSueDate.setText(assetsCreating.getISSUEDATE());
        editClassification.setText(DictionaryHelper.getDictionaryName(assetsCreating.getCLASSIFICATION(), classifications));

        // 财务记账时间, 凭证号
        editVOUCHERNO.setText(assetsCreating.getVOUCHERNO());
        editBOOKKEEPINGTIME.setText(assetsCreating.getBOOKKEEPINGTIME());


        editDWCODE.setText(assetsCreating.getDWCODE());
        editSAMPLDATE.setText(assetsCreating.getSAMPLDATE());
        editSAMPLCODE.setText(assetsCreating.getSAMPLCODE());
        editMONITORPRO.setText(assetsCreating.getMONITORPRO());
        editSAVEMETHOD.setText(assetsCreating.getSAVEMETHOD());
        editSAMPLESTATE.setText(DictionaryHelper.getDictionaryName(assetsCreating.getSAMPLESTATE(), sampleStateFields));
        editSAMPLEUSER.setText(assetsCreating.getSAMPLEUSER());

        editCONCENTRATION.setText(assetsCreating.getCONCENTRATION());
        editPREPARER.setText(assetsCreating.getPREPARER());
        editMEDIA.setText(assetsCreating.getMEDIA());
        editSTORAGENEED.setText(assetsCreating.getSTORAGENEED());


        editHARDID.setText(assetsCreating.getHARDID());
        editIP.setText(assetsCreating.getIP());
        editMAC.setText(assetsCreating.getMAC());
        editINTERNETID.setText(assetsCreating.getINTERNETID());
        editPORT.setText(assetsCreating.getPORT());
        editPURPOSE.setText(assetsCreating.getPURPOSE());
        editHARDSN.setText(assetsCreating.getHARDSN());
        editNETTYPE.setText(assetsCreating.getNETTYPE());
        editOS.setText(assetsCreating.getOS());
//        editOS.setText(assetsCreating.getOS());
//        editOS.setText(DictionaryHelper.getDictionaryName(assetsCreating.getOS(), osArray));
        editINSTALLDATE.setText(assetsCreating.getINSTALLDATE());

    }

    public void viewClickSetting(){
        Calendar calendar = Calendar.getInstance();
        final int mYear = calendar.get(Calendar.YEAR);
        final int mMonth = calendar.get(Calendar.MONTH);
        final int mDay = calendar.get(Calendar.DAY_OF_MONTH);

        ArrayAdapter<String> userAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, AccountHelper.getUserList(loginAccount.getZTBH()));
        editASSETSUSER.setAdapter(userAdapter);
        editCUSTODIAN.setAdapter(userAdapter);
        editCUSTODIAN.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editCUSTODIAN.setText(AccountHelper.getUserList(application.getLoginAccount().getZTBH()).get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        editASSETSUSER.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editASSETSUSER.setText(AccountHelper.getUserList(application.getLoginAccount().getZTBH()).get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ArrayAdapter<String> departAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, bmArray);
        editASSETSDEPT.setAdapter(departAdapter);
        editASSETSDEPT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editASSETSDEPT.setText(departAdapter.getItem(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // 所属处室联想搜索（与使用部门逻辑一致）
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, bmArray);
        editDEPARTMENT.setAdapter(departmentAdapter);
        editDEPARTMENT.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editDEPARTMENT.setText(departmentAdapter.getItem(i));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        editGROUNDMANAGEDEPT.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(dwArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editGROUNDMANAGEDEPT.setText(dwArray[which]);
                        String danweiCode = AccountHelper.getDanweiCode(dwArray[which]);
                        assetsCreating.setGROUNDMANAGEDEPT(danweiCode);
                        dialog.dismiss();
                    }
                })
                .create(mCurrentDialogStyle).show());

        editSYFS.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(syfsArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editSYFS.setText(syfsArray[which]);
                        assetsCreating.setTRACEABILITYPE(String.format(Locale.CHINESE, "%02d", which + 1));
                        dialog.dismiss();
                    }
                })
                .create(mCurrentDialogStyle).show());

        // 监听editASSETSCURRPRICE
        editASSETSCURRPRICE.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 文本变化之后的操作，将值同步到卡片金额，净值
                String str = s.toString();
                if (StringUtils.isNullOrEmpty(str)) {
                    editLEFTPRICE.setText("");
                    editKPJE.setText("");
                    return;
                }
                editLEFTPRICE.setText(str);
                editKPJE.setText(str);
            }
        });

        editASSETSUSEDATE.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editASSETSUSEDATE.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());
        editASSETSENDUSEDATE.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editASSETSENDUSEDATE.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());
        editASSETSDISPENSEDATE.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editASSETSDISPENSEDATE.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());

        editJDJZRQ.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editJDJZRQ.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());
        editYXQZ.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editYXQZ.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());

        editYXQZ.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editYXQZ.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());

        editFACTORYDATE.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editFACTORYDATE.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());

        editIsSueDate.setOnClickListener(view -> new DatePickerDialog(mContext, (datePicker, year, month, dayOfMonth) -> {
            Calendar tCalendar = Calendar.getInstance();
            tCalendar.set(year, month, dayOfMonth);
            editIsSueDate.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            assetsCreating.setISSUEDATE(DataConvert.FormatDate2Day(tCalendar.getTime()));
        }, mYear, mMonth, mDay).show());

        editBOOKKEEPINGTIME.setOnClickListener(view -> new DatePickerDialog(mContext, (datePicker, year, month, dayOfMonth) -> {
            Calendar tCalendar = Calendar.getInstance();
            tCalendar.set(year, month, dayOfMonth);
            editBOOKKEEPINGTIME.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            assetsCreating.setBOOKKEEPINGTIME(DataConvert.FormatDate2Day(tCalendar.getTime()));
        }, mYear, mMonth, mDay).show());

        editACCOUNTBOOK.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(accountBookFields, (dialog, which) -> {
                    editACCOUNTBOOK.setText(accountBookFields[which]);
                    assetsCreating.setACCOUNTBOOK(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editDOWNINFO.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(downInfoFields, (dialog, which) -> {
                    editDOWNINFO.setText(downInfoFields[which]);
                    assetsCreating.setDOWNINFO(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editDEVICETYPE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(deviceTypeFields, (dialog, which) -> {
                    editDEVICETYPE.setText(deviceTypeFields[which]);
                    assetsCreating.setDEVICETYPE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editSAMPLDATE.setOnClickListener(view -> new DatePickerDialog(mContext, (datePicker, year, month, dayOfMonth) -> {
            Calendar tCalendar = Calendar.getInstance();
            tCalendar.set(year, month, dayOfMonth);
            editSAMPLDATE.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            assetsCreating.setSAMPLDATE(DataConvert.FormatDate2Day(tCalendar.getTime()));
        }, mYear, mMonth, mDay).show());
        editSYZQ.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (StringUtils.isNullOrEmpty(editable.toString().trim())) return;
                if (!StringUtils.isNullOrEmpty(editJDJZRQ.getText().toString().trim())) {
                    Date dt = DataConvert.format2Date(editJDJZRQ.getText().toString().trim());
                    if (dt == null) {
                        showShortText("检定校准日期有误，请重新选择！");
                        return;
                    }
                    int months = Integer.parseInt(editable.toString());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dt);
                    calendar.add(Calendar.MONTH, months);
                    calendar.add(Calendar.DATE, -1);
                    editYXQZ.setText(DataConvert.FormatDate2Day(calendar.getTime()));
                }
            }
        });

        editJDJZRQ.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (StringUtils.isNullOrEmpty(editable.toString().trim())) return;
                Date dt = DataConvert.format2Date(editable.toString().trim());
                if (dt == null || StringUtils.isNullOrEmpty(editable.toString())) return;

                int months = Integer.parseInt(editSYZQ.getText().toString().trim());
                if (!StringUtils.isNullOrEmpty(editable.toString().trim())) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dt);
                    calendar.add(Calendar.MONTH, months);
                    calendar.add(Calendar.DATE, -1);
                    editYXQZ.setText(DataConvert.FormatDate2Day(calendar.getTime()));
                }
            }
        });
        editClassification.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(classifications, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editClassification.setText(classifications[which]);
                    assetsCreating.setCLASSIFICATION(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());



        editSAMPLESTATE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(sampleStateFields, (dialog, which) -> {
                    editSAMPLESTATE.setText(sampleStateFields[which]);
                    assetsCreating.setSAMPLESTATE("0"+(which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        // 设备用途选择
        editPURPOSE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(devicePurposeFields, (dialog, which) -> {
                    if (assetsCreating == null) {
                        showShortText("请先选择资产");
                        return;
                    }
                    editPURPOSE.setText(devicePurposeFields[which]);
                    assetsCreating.setPURPOSE(which == devicePurposeFields.length - 1 ? "99" : String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        // 网络类型选择
        editNETTYPE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(netTypeFields, (dialog, which) -> {
                    if (assetsCreating == null) {
                        showShortText("请先选择资产");
                        return;
                    }
                    editNETTYPE.setText(netTypeFields[which]);
                    assetsCreating.setNETTYPE(which == netTypeFields.length - 1 ? "99" : String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        // 是否公物仓选择
        editISGWC.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(gwcFields, (dialog, which) -> {
                    if (assetsCreating == null) {
                        showShortText("请先选择资产");
                        return;
                    }
                    editISGWC.setText(gwcFields[which]);
                    assetsCreating.setISGWC(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        // 特种设备选择：01是 02否
        editSPECIALDEVICE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(specialDeviceFields, (dialog, which) -> {
                    if (assetsCreating == null) {
                        showShortText("请先选择资产");
                        return;
                    }
                    editSPECIALDEVICE.setText(specialDeviceFields[which]);
                    assetsCreating.setSPECIALDEVICE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editGETMODE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(qdfsArray, (dialog, which) -> {
                    editGETMODE.setText(qdfsArray[which]);
                    assetsCreating.setGETMODE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        //分类用途
        editVEHPURPOSE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(flytArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editVEHPURPOSE.setText(flytArray[which]);
                    assetsCreating.setVEHPURPOSE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editDPRECIATIONTYPE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(zjztArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editDPRECIATIONTYPE.setText(zjztArray[which]);
                    assetsCreating.setDPRECIATIONTYPE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editUSESTATE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(syzkArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editUSESTATE.setText(syzkArray[which]);
                    assetsCreating.setUSESTATE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editPURCHASINGORGANIZATION.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(cgzzxsArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editPURCHASINGORGANIZATION.setText(cgzzxsArray[which]);
                    assetsCreating.setPURCHASINGORGANIZATION(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editASSETSCULGRADE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(wwdjArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editASSETSCULGRADE.setText(wwdjArray[which]);
                    assetsCreating.setASSETSCULGRADE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editASSETSORGANIZATION.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(bzqkArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editASSETSORGANIZATION.setText(bzqkArray[which]);
                    assetsCreating.setASSETSORGANIZATION(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editPROPERTY.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(dwArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editPROPERTY.setText(dwArray[which]);
                    assetsCreating.setPROPERTY(dwArray[which]);
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        /*账套选择*/
        editACCTSUITEID.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(ztArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editACCTSUITEID.setText(ztArray[which]);
                    assetsCreating.setACCTSUITEID(SysConfig.getAccountsByZTMC(ztArray[which]).getZTBH());
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        /*资产类别选择*/
        editASSETSTYPE.setOnClickListener(view -> {
            if(assetsCreating == null){
                showShortText("请先扫码选择资产");
                return;
            }

            int assetsCategoryNewOrOld = AssetsCategoryHelper.getAssetsCategoryNewOrOld();
            String[] temp = null;
            if (assetsCategoryNewOrOld == 1) {
                temp = zclbNewArray;
            } else {
                temp = zclbArray;
            }
            // 高效复制临时变量
            String[] finalTemp = temp;
            new QMUIDialog.CheckableDialogBuilder(mContext)
                    .addItems(finalTemp, (dialog, which) -> {
                        String textType = finalTemp[which];
                        editASSETSTYPE.setText(textType);
                        assetsCreating.setASSETSTYPE("0" + (which + 1));
                        dialog.dismiss();
                        asyncCategory(textType);
                    })
                    .create(mCurrentDialogStyle).show();
        });
        /*资产代码选择*/
        editASSETSSORTCODE.setOnClickListener(view -> {

            if(assetsCreating == null){
                showShortText("请先扫码选择资产");
                return;
            }

            if (rootCategoryArray == null || rootCategoryArray.isEmpty()) {
                showShortText("请先选择资产类别，或者等待数据加载完成");
                return;
            }
            pvOptions.setSelectOptions(0, 0, 0);
            pvOptions.setPicker(rootCategoryArray, secondCategoryArray, thirdCategoryArray);
            pvOptions.show();
        });

/*        editOS.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)
                .addItems(osArray, (dialog, which) -> {
                    if(assetsCreating == null){
                        showShortText("请先扫码选择资产");
                        return;
                    }

                    editOS.setText(osArray[which]);
                    assetsCreating.setOS(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());*/


        editINSTALLDATE.setOnClickListener(view -> new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar tCalendar = Calendar.getInstance();
                tCalendar.set(year, month, dayOfMonth);
                editINSTALLDATE.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            }
        }, mYear, mMonth, mDay).show());
    }


    // ==================== AI拍照识图相关（子类共用） ====================

    protected AIPhotoCaptureHelper aiPhotoCaptureHelper;
    protected ActivityResultLauncher<Intent> cameraLauncher;

    // 记录上次AI填充的值，用于重拍时判断用户是否手动修改
    private Assets lastAiFilledAssets = new Assets();

    /**
     * 初始化相机 ActivityResultLauncher（在子类 onCreate 中调用）
     */
    protected void initCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        handlePhotoResult();
                    }
                }
        );
    }

    /**
     * 初始化AI拍照助手（在 ButterKnife.bind 之后调用）
     */
    protected void initAIPhotoHelper() {
        if (cameraLauncher == null) {
            initCameraLauncher();
        }
        aiPhotoCaptureHelper = new AIPhotoCaptureHelper(this, cameraLauncher, new AIPhotoCaptureHelper.OnAIPhotoCompleteListener() {
            @Override
            public void onAIPhotoComplete(Assets aiAssets) {
                onAIPhotoDataFilled(aiAssets);
            }

            @Override
            public void onAIPhotoError(String error) {
                showShortText("AI拍照识别失败: " + error);
            }
        });

        buttonAIPhoto.setOnClickListener(v -> {
            if (assetsCreating == null) {
                showShortText("请先保存基本信息");
                return;
            }
            aiPhotoCaptureHelper.startAIPhotoCapture(assetsCreating);
        });
    }

    private void handlePhotoResult() {
        String latestImagePath = aiPhotoCaptureHelper != null ?
                aiPhotoCaptureHelper.getCapturedPhotoPath() : null;
        if (latestImagePath != null && new File(latestImagePath).exists()) {
            showUploadProgress();
            uploadImageToServer(latestImagePath);
            return;
        }
        // 华为等机型相机异步写文件，延迟后重试
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (latestImagePath != null && new File(latestImagePath).exists()) {
                showUploadProgress();
                uploadImageToServer(latestImagePath);
            } else {
                // 回退：华为相机可能把照片写入相册而非临时文件
                String galleryPath = aiPhotoCaptureHelper != null ?
                        aiPhotoCaptureHelper.getLatestImageFromGalleryPublic() : null;
                if (galleryPath != null && new File(galleryPath).exists()) {
                    showUploadProgress();
                    uploadImageToServer(galleryPath);
                } else {
                    showShortText("无法获取拍摄的照片");
                }
            }
        }, 800);
    }

    protected void showUploadProgress() {
        if (aiPhotoCaptureHelper != null) {
            aiPhotoCaptureHelper.showUploadProgress();
        }
    }

    protected void uploadImageToServer(String imagePath) {
        if (aiPhotoCaptureHelper != null) {
            aiPhotoCaptureHelper.uploadImageToServerPublic(imagePath);
        }
    }


    /**
     * AI识别结果填充回调：首次识别直接填充；重新识别时仅覆盖未被用户手动修改的字段
     */
    protected void onAIPhotoDataFilled(Assets aiAssets) {
        if (assetsCreating != null) {
            fillAiField(aiAssets.getASSETSNAME(), lastAiFilledAssets.getASSETSNAME(),
                    assetsCreating.getASSETSNAME(), assetsCreating::setASSETSNAME, editASSETSNAME);
            fillAiField(aiAssets.getBRAND(), lastAiFilledAssets.getBRAND(),
                    assetsCreating.getBRAND(), assetsCreating::setBRAND, editBRAND);
            fillAiField(aiAssets.getASSETSMODEL(), lastAiFilledAssets.getASSETSMODEL(),
                    assetsCreating.getASSETSMODEL(), assetsCreating::setASSETSMODEL, editASSETSMODEL);
            fillAiField(aiAssets.getASSETSMEASURE(), lastAiFilledAssets.getASSETSMEASURE(),
                    assetsCreating.getASSETSMEASURE(), assetsCreating::setASSETSMEASURE, editASSETSMEASURE);
            // 资产类别：用户未手动选择时按AI结果写入并联动UI显示名称；AI未识别出则清空
            boolean typeUntouched = StringUtils.isNullOrEmpty(assetsCreating.getASSETSTYPE())
                    || Objects.equals(assetsCreating.getASSETSTYPE(), lastAiFilledAssets.getASSETSTYPE());
            if (typeUntouched) {
                if (StringUtils.isNullOrEmpty(aiAssets.getASSETSTYPE())) {
                    assetsCreating.setASSETSTYPE("");
                    editASSETSTYPE.setText("");
                } else {
                    assetsCreating.setASSETSTYPE(aiAssets.getASSETSTYPE());
                    String assetsTypeTitle = AssetsCategoryHelper.getAssetsCategoryTitle(aiAssets.getASSETSTYPE());
                    editASSETSTYPE.setText(assetsTypeTitle);
                    if (!StringUtils.isNullOrEmpty(assetsTypeTitle)) {
                        asyncCategory(assetsTypeTitle);
                    }
                }
            }
            // 资产分类：用户未手动选择时按AI结果写入并联动UI显示名称；AI未识别出则清空
            boolean sortCodeUntouched = StringUtils.isNullOrEmpty(assetsCreating.getASSETSSORTCODE())
                    || Objects.equals(assetsCreating.getASSETSSORTCODE(), lastAiFilledAssets.getASSETSSORTCODE());
            if (sortCodeUntouched) {
                if (StringUtils.isNullOrEmpty(aiAssets.getASSETSSORTCODE())) {
                    assetsCreating.setASSETSSORTCODE("");
                    editASSETSSORTCODE.setText("");
                } else {
                    assetsCreating.setASSETSSORTCODE(aiAssets.getASSETSSORTCODE());
                    AssetsCategory assetsCategory = AssetsCategoryHelper.getAssetsCategory(aiAssets.getASSETSSORTCODE());
                    editASSETSSORTCODE.setText(assetsCategory == null ? "" : assetsCategory.getNAME());
                }
            }
            // 记录本次AI填充值，供下次重拍时判断用户是否手动修改过
            lastAiFilledAssets.setASSETSNAME(aiAssets.getASSETSNAME());
            lastAiFilledAssets.setBRAND(aiAssets.getBRAND());
            lastAiFilledAssets.setASSETSMODEL(aiAssets.getASSETSMODEL());
            lastAiFilledAssets.setASSETSMEASURE(aiAssets.getASSETSMEASURE());
            lastAiFilledAssets.setASSETSTYPE(aiAssets.getASSETSTYPE());
            lastAiFilledAssets.setASSETSSORTCODE(aiAssets.getASSETSSORTCODE());
            Toast.makeText(this, "AI识别完成，已填充部分信息", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 通用AI字段填充：AI识别出的字段写入（首次填充或用户未手动修改时覆盖）；
     * AI未识别出（null）的字段，若未被用户手动修改则清空，避免残留上一物品的信息
     */
    private void fillAiField(String aiValue, String lastAiValue, String currentValue,
                             Consumer<String> setter, EditText editText) {
        boolean firstFill = StringUtils.isNullOrEmpty(currentValue);
        boolean untouched = Objects.equals(currentValue, lastAiValue);
        if (!firstFill && !untouched) {
            // 用户手动修改过该字段，不干预
            return;
        }
        // AI未识别出（空）时清空，防止上一物品的信息残留
        String targetValue = StringUtils.isNullOrEmpty(aiValue) ? "" : aiValue;
        setter.accept(targetValue);
        editText.setText(targetValue);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (aiPhotoCaptureHelper != null) {
            aiPhotoCaptureHelper.handlePermissionResult(requestCode, permissions, grantResults);
        }
    }


}

