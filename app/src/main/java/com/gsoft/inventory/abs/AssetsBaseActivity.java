package com.gsoft.inventory.abs;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import butterknife.BindArray;
import butterknife.BindDrawable;
import butterknife.BindView;
import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.view.OptionsPickerView;
import com.gsoft.inventory.R;
import com.gsoft.inventory.data.CategoryData;
import com.gsoft.inventory.service.new_assets.LoadCategoryTask;
import com.gsoft.inventory.utils.*;
import com.qmuiteam.qmui.layout.QMUIButton;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    /*卡片编号*/
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
    /*使用人*/
    @BindView(R.id.editASSETSUSER)
    public AutoCompleteTextView editASSETSUSER;
    /*使用部门,名称*/
    @BindView(R.id.editASSETSDEPT)
    public AutoCompleteTextView editASSETSDEPT;
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
    @BindView(R.id.editGLBH)
    public EditText editGLBH;
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

    @BindView(R.id.editVENDOR)
    public EditText editVENDOR;

    @BindView(R.id.editClassification)
    public TextView editClassification;

    @BindView(R.id.editIsSueDate)
    public TextView editIsSueDate;


    @BindDrawable(R.drawable.icon_right)
    public Drawable icon_right;

    @BindView(R.id.editBOOKKEEPINGTIME)
    public TextView editBOOKKEEPINGTIME;

    @BindView(R.id.editVOUCHERNO)
    public TextView editVOUCHERNO;


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
                    initCategoryPicker(); // 调用公共初始化方法
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

    public void setBarCode(String barCode) {
        if (StringUtils.isNullOrEmpty(editBARCODEID.getText().toString())
                && StringUtils.isNullOrEmpty(editSERIALCODE.getText().toString())) {
            assetsCreating.setSERIALCODE(barCode);
            assetsCreating.setBARCODEID(barCode);
        } else if (!StringUtils.isNullOrEmpty(editSERIALCODE.getText().toString())
                && StringUtils.isNullOrEmpty(editBARCODEID.getText().toString())) {
            assetsCreating.setSERIALCODE(editSERIALCODE.getText().toString());
            assetsCreating.setBARCODEID(editSERIALCODE.getText().toString());
        } else if (StringUtils.isNullOrEmpty(editSERIALCODE.getText().toString())
                && !StringUtils.isNullOrEmpty(editBARCODEID.getText().toString())) {
            assetsCreating.setSERIALCODE(editBARCODEID.getText().toString());
            assetsCreating.setBARCODEID(editBARCODEID.getText().toString());
        } else {
            assetsCreating.setSERIALCODE(editSERIALCODE.getText().toString());
            assetsCreating.setBARCODEID(editBARCODEID.getText().toString());
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
        assetsCreating.setMANAGENO(editGLBH.getText().toString());
        assetsCreating.setPARAMETERS(editCANSHU.getText().toString());
        assetsCreating.setTRACEABILITY(editSYZQ.getText().toString());
        assetsCreating.setCALIBRATIONDATE(editJDJZRQ.getText().toString());
        assetsCreating.setVALIDDATE(editYXQZ.getText().toString());
        assetsCreating.setPROBELONG(editSSXM.getText().toString());
        assetsCreating.setISSUEDATE(editIsSueDate.getText().toString());
        assetsCreating.setISUPLOAD("0");
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
        if (StringUtils.isNullOrEmpty(editASSETSDEPT.getText().toString())) {
            showShortText("请选择使用部门");
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
        /*卡片编号*/
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
        /*使用人*/
        editASSETSUSER.setText(assetsCreating.getASSETSUSER());
        /*使用部门*/
        editASSETSDEPT.setText(assetsCreating.getASSETSDEPT());
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
        editASSETSNUM.setEnabled(false);
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
        editGLBH.setText(assetsCreating.getMANAGENO());
        editCANSHU.setText(assetsCreating.getPARAMETERS());
        editSYZQ.setText(assetsCreating.getTRACEABILITY());
        //editSYFS.setText(assetsCreating.getTRACEABILITYPE());
        editSYFS.setText(DictionaryHelper.getDictionaryName(assetsCreating.getTRACEABILITYPE(), syfsArray));
        editJDJZRQ.setText(assetsCreating.getCALIBRATIONDATE());
        editYXQZ.setText(assetsCreating.getVALIDDATE());
        editSSXM.setText(assetsCreating.getPROBELONG());

        editIsSueDate.setText(assetsCreating.getISSUEDATE());
        editClassification.setText(DictionaryHelper.getDictionaryName(assetsCreating.getCLASSIFICATION(), classifications));

        // 财务记账时间, 凭证号
        editVOUCHERNO.setText(assetsCreating.getVOUCHERNO());
        editBOOKKEEPINGTIME.setText(assetsCreating.getBOOKKEEPINGTIME());

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
        editIsSueDate.setOnClickListener(view -> new DatePickerDialog(mContext, (datePicker, year, month, dayOfMonth) -> {
            Calendar tCalendar = Calendar.getInstance();
            tCalendar.set(year, month, dayOfMonth);
            editIsSueDate.setText(DataConvert.FormatDate2Day(tCalendar.getTime()));
            assetsCreating.setISSUEDATE(DataConvert.FormatDate2Day(tCalendar.getTime()));
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
                    editClassification.setText(classifications[which]);
                    assetsCreating.setCLASSIFICATION(String.format("%02d", which + 1));
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
                    editVEHPURPOSE.setText(flytArray[which]);
                    assetsCreating.setVEHPURPOSE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editDPRECIATIONTYPE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(zjztArray, (dialog, which) -> {
                    editDPRECIATIONTYPE.setText(zjztArray[which]);
                    assetsCreating.setDPRECIATIONTYPE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editUSESTATE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(syzkArray, (dialog, which) -> {
                    editUSESTATE.setText(syzkArray[which]);
                    assetsCreating.setUSESTATE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editPURCHASINGORGANIZATION.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(cgzzxsArray, (dialog, which) -> {
                    editPURCHASINGORGANIZATION.setText(cgzzxsArray[which]);
                    assetsCreating.setPURCHASINGORGANIZATION(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editASSETSCULGRADE.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(wwdjArray, (dialog, which) -> {
                    editASSETSCULGRADE.setText(wwdjArray[which]);
                    assetsCreating.setASSETSCULGRADE(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());

        editASSETSORGANIZATION.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(bzqkArray, (dialog, which) -> {
                    editASSETSORGANIZATION.setText(bzqkArray[which]);
                    assetsCreating.setASSETSORGANIZATION(String.format("%02d", which + 1));
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        editPROPERTY.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(dwArray, (dialog, which) -> {
                    editPROPERTY.setText(dwArray[which]);
                    assetsCreating.setPROPERTY(dwArray[which]);
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        /*账套选择*/
        editACCTSUITEID.setOnClickListener(view -> new QMUIDialog.CheckableDialogBuilder(mContext)

                .addItems(ztArray, (dialog, which) -> {
                    editACCTSUITEID.setText(ztArray[which]);
                    assetsCreating.setACCTSUITEID(SysConfig.getAccountsByZTMC(ztArray[which]).getZTBH());
                    dialog.dismiss();
                })
                .create(mCurrentDialogStyle).show());
        /*资产类别选择*/
        editASSETSTYPE.setOnClickListener(view -> {
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
            if (rootCategoryArray == null || rootCategoryArray.isEmpty()) {
                showShortText("请先选择资产类别，或者等待数据加载完成");
                return;
            }
            pvOptions.setSelectOptions(0, 0, 0);
            pvOptions.setPicker(rootCategoryArray, secondCategoryArray, thirdCategoryArray);
            pvOptions.show();
        });
    }


}

