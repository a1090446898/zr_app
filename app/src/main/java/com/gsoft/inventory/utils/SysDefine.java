package com.gsoft.inventory.utils;

import java.security.PublicKey;

/**
 * @author 10904
 */
public class SysDefine {

    public static final String IntentExtra_AssetsView = "IntentExtra_AssetsView";
    public static final String IntentExtra_AssetsID = "IntentExtra_AssetsID";
    public static final String IntentExtra_ASSETS_ARRAY_INDEX = "IntentExtra_ASSETS_ARRAY_INDEX";
    public static final String IntentExtra_SERIALCODE = "IntentExtra_SERIALCODE";
    public static final String IntentExtra_PHOTOIMAGEARRAY = "IntentExtra_PHOTOIMAGEARRAY";
    /** 照片查看页：当前点击照片在数组中的下标，用于直接定位到该张 */
    public static final String IntentExtra_PHOTOIMAGEINDEX = "IntentExtra_PHOTOIMAGEINDEX";
    /** RFID盘点一键变更跳转资产变更页时携带：true=页面加载后自动滚动到"使用人"字段 */
    public static final String IntentExtra_ScrollToUser = "IntentExtra_ScrollToUser";

    public static final int REQESTCODE_WAITINGBLUETOOTH = 101;
    public static final int REQESTCODE_WAITINGBLUETOOTH_QRCODE = 105;
    public static final int REQESTCODE_WAITINGBLUETOOTH_BARCODE = 106;
    /** 补打条码页：打印机未连接时等待蓝牙连接，连接成功后自动续打 */
    public static final int REQESTCODE_WAITINGBLUETOOTH_REPRINT = 107;
    public static final int REQUEST_CODE_CAPTURE_CAMEIA = 102;
    public static final String PROCESS_RESULT_SUCCESS = "SUCCESS";
    public static final String PROCESS_RESULT_ERROR = "ERROR";
    public static final String TRANS_DISPOSE_CODE = "TRANS_DISPOSE_CODE";
    public static final String TRANS_SIGNATURE_PHOTO = "TRANS_SIGNATURE_PHOTO";

    public static String SERVERHOST = "http://www.zrast.com/";
//    public static String SERVERHOST = "http://192.168.2.156:80/";

    public static String WEB_URL = "http://www.zrast.com:8034";
//    public static String WEB_URL = "http://192.168.2.102:8082";

    public static String AI_PHOTO_UPLOAD_API = "http://ea.zrast.com:9093/zr-ai/business/attachment/upload?status=2";
    public static String AI_PHOTO_UPLOAD_RECOGNITION_API = "http://ea.zrast.com:9093/zr-ai/ai/productVision/extract";

//    public static String AI_PHOTO_UPLOAD_API = "http://192.168.2.103:8080/business/attachment/upload?status=2";
//    public static String AI_PHOTO_UPLOAD_RECOGNITION_API = "http://192.168.2.102:8080/ai/productVision/extract";

    /** 批量打印：连接打印机后返回续打（与单条补打107区分，避免基类误清任务） */
    public static final int REQESTCODE_WAITINGBLUETOOTH_BATCH_PRINT = 108;

    /** 打印标签选择页：设置页/离线登录页跳转选择打印标签后返回 */
    public static final int REQESTCODE_SELECT_PRINT_LABEL = 109;

    public static String getServerHost() {
        return SERVERHOST;
    }

    public static void setServerHost(String serverHost) {
        SERVERHOST = serverHost;
    }

    public static String SERVERHOST_ASSETS () {
        return getServerHost() + "assets.do";
    }
    public static String PRINT_LOG_URL (){
        return getServerHost() + "getAssetsInterface.do?Act=uploadPrintLog";
    }
    public static String SERVERHOST_DEL (){
        return getServerHost() + "assets.do?barcodeid=";
    }
    public static String SERVERHOST_SYNC_ASSETCATEGORY () {
        return getServerHost() + "getAssetsInterface.do?Act=getCode";
    }
    public static String SERVERHOST_SYNC_ASSETCATEGORY_NEW () {
        return getServerHost() + "getAssetsInterface.do?Act=getCode&acctid=%1$s";
    }
    public static String SERVERHOST_SYNC_ACCOUNTS () {
        return getServerHost() + "getAssetsInterface.do?Act=getAccountId";
    }
    public static String SERVERHOST_SYNC_ASSETS () {
        return getServerHost() + "getAssetsInterface.do?Act=getAssetsList&acctid=%1$s&deptId=%2$s";
    }
    public static String SERVERHOST_SYNC_ASSETS_WITH_USER () {
        return getServerHost() + "getAssetsInterface.do?Act=getAssetsList&acctid=%1$s&deptId=%2$s&UserId=%3$s";
    }
    public static String SERVERHOST_SYNC_ASSETS_BY_ID () {
        return getServerHost() + "getAssetsInterface.do?Act=getAssetsListBySearch";
    }
    public static String SERVERHOST_SYNC_ASSETS_BY_ID_GET () {
        return getServerHost() + "getAssetsInterface.do?Act=getAssetsListBySearch&acctid=%1$s&data=%2$s";
    }
    public static String SERVER_SYNC_LOGINACCOUNTS () {
        return getServerHost() + "getAssetsInterface.do?Act=getAccountId&loginId=%1$s&pwd=%2$s";
    }
    public static String SERVER_PRINTERS_SYNC () {
        return getServerHost() + "getAssetsInterface.do?Act=getMd5Code";
    }
    public static String SERVER_APK_UPDATE () {
        return getServerHost() + "version.do?Act=getVersion&ACCTID=%1$s";
    }
    public static String SERVER_SYNC_ZCQCY () {
        return getServerHost() + "getAssetsInterface.do?Act=getUserList&acctsuiteId=%1$s";
//        return getServerHost() + "getAssetsInterface.do?Act=getUserList&acctid=%1$s&type=%2$s";
    }
    public static String SERVER_ASSET_DISPOSE_APPLY () {
        return getServerHost() + "getAssetsInterface.do?Userid=%1$s&Act=getDispose";
    }
    public static String SERVER_GET_ASSETS_PEOPLES () {
        return getServerHost() + "getAssetsInterface.do?Act=getUser&acctid=%1$s";
    }
    public static String SERVER_ASSET_DISPOSE_LIST () {
        return getServerHost() + "getAssetsInterface.do?Formid=%1$s&Act=getDisposeList";
    }
    public static String SERVER_ASSET_DISPOSE_SCAN_UPLOAD () {
        return getServerHost() + "getAssetsInterface.do?Formid=%1$s&Docid=%2$s&Act=uploadDisposeAssets";
    }
    public static String SERVER_ASSET_DISPOSE_DELETE () {
        return getServerHost() + "getAssetsInterface.do?Formid=%1$s&Docid=%2$s&Act=delDisposeAssets";
    }
    public static String SERVER_FETCH_SIGNATURES_LIST () {
        return getServerHost() + "getAssetsInterface.do?Act=GetImgInfo&code=%1$s";
    }
    public static String SERVER_ASSET_UPLOAD_PHOTO () {
        return getServerHost() + "assetsPageSizeSetServlet.do?Act=uploadImg";
    }

    /**
     * 照片上传地址：单据号/条码/用户必须放在查询串
     * <p>
     * assetsPageSizeSetServlet 未声明 @MultipartConfig 时，request.getParameter
     * 取不到 multipart 请求体里的表单域，服务端会收到文件但 DOCID/BARCODEID 为空，
     * 接口返回 "true" 却查不到照片。
     */
    public static String SERVER_ASSET_UPLOAD_PHOTO (String docId, String barCodeId, String userId) {
        return SERVER_ASSET_UPLOAD_PHOTO()
                + "&DOCID=" + urlEncode(docId)
                + "&BARCODEID=" + urlEncode(barCodeId)
                + "&USERID=" + urlEncode(userId);
    }

    public static String SERVER_ASSET_PHOTO_LIST () {
        return getServerHost() + "assetsPageSizeSetServlet.do?Act=GetImgList&DOCID=%1$s&BARCODEID=%2$s";
    }

    /**
     * 查询服务器已上传照片列表（参数先做 URL 编码，条码可能含中文/空格）
     */
    public static String SERVER_ASSET_PHOTO_LIST (String docId, String barCodeId) {
        return String.format(SERVER_ASSET_PHOTO_LIST(), urlEncode(docId), urlEncode(barCodeId));
    }

    /**
     * 查询串参数编码
     */
    public static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static String SERVER_ASSET_PHOTO_DELETE () {
        return getServerHost() + "assetsPageSizeSetServlet.do?Act=DelImgInfo&ID=%1$s";
    }
    public static String RUNTIME_MODEL_ONLINE = "ONLINE";

    public static String SEND_ASSETS_SIGNATURE_SMS = "https://ea.zrast.com/common/smsAssetsSignature?telephone=%1$S&name=%2$S";
    public static String COPY_IMG () {
        return getServerHost() + "getAssetsInterface.do?Act=copyImg";
    }

    public static String getLy(){return getServerHost() + "getAssetsInterface.do?Act=getLy&Userid=%1$s";}
    public static String getLyDetailList(){return getServerHost() + "getAssetsInterface.do?Act=getLyList&Formid=%1$s";}
    public static String getLySelectList(){return getServerHost() + "getAssetsInterface.do?Act=getGwcList&Userid=%1$s";}
    public static String getJy(){return getServerHost() + "getAssetsInterface.do?Act=getDevice&Userid=%1$s&Type=02";}
    public static String getJyDetailList(){return getServerHost() + "getAssetsInterface.do?Act=getDeviceList&Formid=%1$s";}

    public static String getGh(){return getServerHost() + "getAssetsInterface.do?Act=getComeList&Userid=%1$s";}

    public static String getYj(){return getServerHost() + "getAssetsInterface.do?Act=getHandon&Userid=%1$s";}
    public static String getYjDetailList(){return getServerHost() + "getAssetsInterface.do?Act=getHandonList&Formid=%1$s";}

    public static String getCg(){return getServerHost() + "getAssetsInterface.do?Act=getPurchase&Userid=%1$s";}
    public static String getCgDetailList(){return getServerHost() + "getAssetsInterface.do?Act=getPurchaseList&Formid=%1$s";}

    public static String getBx(){return getServerHost() + "getAssetsInterface.do?Act=getRepair&Userid=%1$s";}
    public static String getBxDetailList(){return getServerHost() + "getAssetsInterface.do?Act=getRepairList&Formid=%1$s";}

    public static String getFs(){return getServerHost() + "getAssetsInterface.do?Act=getClothes&Userid=%1$s";}
    public static String getFsDetailList(){return getServerHost() + "getAssetsInterface.do?Act=getClothesList&Formid=%1$s";}

    public static String getMenuRole(){return getServerHost() + "getAssetsInterface.do?Act=getMenuList&Userid=%1$s";}
    public static String getBadgeAndNotificationApi(){return getServerHost() + "getAssetsInterface.do?Act=getDealList&Userid=%1$s";}

    public enum RuntimeModel {Online, Offline}
}
