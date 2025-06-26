package com.gsoft.inventory;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.client.android.Intents;
import com.gsoft.inventory.adapter.DisposeAssetsListAdapter;
import com.gsoft.inventory.entities.AssetDispose;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.utils.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;
import com.rscja.deviceapi.entity.BarcodeEntity;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
//import com.zebra.adc.decoder.Barcode2DWithSoft;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.PermissionUtils;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class DisposeAssetsActivity extends BaseCompatActivity {
    private static final int REQUEST_CAMERASCAN = 0;

    private static final String[] PERMISSION_CAMERASCAN = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE"};

    DisposeAssetsListAdapter listAdapter;
    ArrayList<DisposeAsset> assetList = new ArrayList<>();
    private Context context;
    ListView listView;
    private final Gson gson = new Gson();
    SmartRefreshLayout refreshLayout;
    int pageSize = 15;
    private int offset = 0; // 初始偏移量为0（第一页）
    boolean hasMoreData = false;

    RelativeLayout mHead;
    AssetDispose dispose;

    private boolean isOffline = false;

    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    BarcodeDecoder barcodeDecoder = BarcodeFactory.getInstance().getBarcodeDecoder();
    private final ScanCodeHandler scanCodeHandler = new ScanCodeHandler(this);
    String seldata = "utf-8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispose_assets);
        context = this;

        isOffline = getIntent().getBooleanExtra("isOffline", false);
        initializeView();
    }

    @Override
    public void initializeView() {
        QMUITopBar topBar = findViewById(R.id.topbar);
        topBar.setTitle("资产处置详情");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        //topBar.addRightTextButton("添加", R.id.topbar_right_about_button);
        topBar.addRightTextButton("删除", R.id.topbar_right_change_button).setOnClickListener(v -> {

            List<DisposeAsset> checkedAssets = new ArrayList<>();
            for (DisposeAsset asset : assetList) {
                if (asset.isChecked()) {
                    checkedAssets.add(asset);
                }
            }
            if (checkedAssets.isEmpty()) return;
            DisposeAsset[] assets = new DisposeAsset[checkedAssets.size()];
            if (!isOffline) {
                new RemoveDisposeAssetsTask().execute(checkedAssets.toArray(assets));
            }
            else{
                new RemoveDisposeAssetsOfflineTask().execute(checkedAssets.toArray(assets));
            }
        });
        topBar.addRightTextButton("扫码", R.id.topbar_right_query_button).setOnClickListener(v -> {
            // 扫码处理
           /* if (PermissionUtils.hasSelfPermissions(DisposeAssetsActivity.this, PERMISSION_CAMERASCAN)) {
                cameraScan();
            } else {
                ActivityCompat.requestPermissions(DisposeAssetsActivity.this, PERMISSION_CAMERASCAN, REQUEST_CAMERASCAN);
            }*/
            // 进行判断，如果当前设备安卓版本为7.0以上，正常调用扫码功能，
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                showShortText("请按手持设备侧边进行扫码");
                return;
            }
            // 扫码处理
            if (PermissionUtils.hasSelfPermissions(mContext, PERMISSION_CAMERASCAN)) {
                cameraScan();
            } else {
                ActivityCompat.requestPermissions(DisposeAssetsActivity.this, PERMISSION_CAMERASCAN, REQUEST_CAMERASCAN);
            }
        });
        dispose = (AssetDispose) getIntent().getSerializableExtra(SysDefine.TRANS_DISPOSE_CODE);
        if (dispose == null) return;
//        barcode2DWithSoft = Barcode2DWithSoft.getInstance();
        mHead = (RelativeLayout) findViewById(R.id.head);
        mHead.setFocusable(true);
        mHead.setClickable(true);
        mHead.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());

        refreshLayout = findViewById(R.id.refreshLayout);
        listView = findViewById(R.id.listAssets);
        assetList = new ArrayList<>();
        listAdapter = new DisposeAssetsListAdapter(assetList, DisposeAssetsActivity.this, mHead);
        listView.setAdapter(listAdapter);
        listView.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());

        barcodeLauncher = registerForActivityResult(new ScanContract(),
                result -> {
                    if (StringUtils.isNullOrEmpty(result.getContents())) {
                        Intent originalIntent = result.getOriginalIntent();
                        if (originalIntent == null) {
                            showShortText("取消扫码");
                        } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                            Toast.makeText(mContext, "授权失败，请允许使用您的摄像头扫码", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    String scanString = StringUtils.decodeQRCode(result.getContents());
                    onScanResult(scanString);
                });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                if (hasMoreData)
                    if(!isOffline){
                        new SearchDisposeAssetsTask().execute();
                    }
                    else{
                        new SearchDisposeAssetsOfflineTask().execute();
                    }
                else
                    refreshLayout.finishLoadMore(100);
            }
        });
        if(!isOffline){
            new SearchDisposeAssetsTask().execute();
        }
        else{
            new SearchDisposeAssetsOfflineTask().execute();
        }


    }

    private void refreshData() {
        assetList.clear();
        hasMoreData = false;
        if(!isOffline){
            new SearchDisposeAssetsTask().execute();
        }
        else{
            new SearchDisposeAssetsOfflineTask().execute();
        }
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE})
    public void cameraScan() {
        ScanOptions options = new ScanOptions();
        barcodeLauncher.launch(options);
    }


    public void requestDisposeAsset(String docId) {
        new RequestAddAssetTask().execute(docId);
    }

    private class RequestAddAssetTask extends AsyncTask<String, Void, String> {
        private String docId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            showProgressDialog("", "正在检索");
        }

        @Override
        protected String doInBackground(String... params) {
            docId = params[0];
            String code = dispose.getCode();
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_SCAN_UPLOAD(), code, docId));
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return "ERROR";
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (s.contains("\"result\":\"OK\"") || s.contains("OK")) {
                refreshData();
            } else {
                showShortText("上传失败，".concat(s));
                DisposeAsset assetAdd = new DisposeAsset();
                assetAdd.setDispose(dispose.getCode());
                assetAdd.setCode(docId);
                assetAdd.setStatus(1);
                assetAdd.save();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            hideProgressDialog();
        }
    }

    private class SearchDisposeAssetsTask extends AsyncTask<Void, Void, List<DisposeAsset>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            showProgressDialog("", "正在检索");
        }

        @Override
        protected List<DisposeAsset> doInBackground(Void... params) {
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_LIST(), dispose.getCode()));
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return null;
            }
            List<DisposeAsset> disposes = new ArrayList<>();
            try {
                List<String> results = gson.fromJson(responseString, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.isEmpty()) {
                    return disposes;
                }
                for (String dataLine : results) {
                    DisposeAsset dispose = DataTransmitHelper.addDisposeAsset(dataLine);
                    if (dispose != null) disposes.add(dispose);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return disposes;
        }

        @Override
        protected void onPostExecute(List<DisposeAsset> s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (s.size() < pageSize) {
                hasMoreData = false;
                //refreshLayout.setEnableLoadMore(false);
            } else {
                hasMoreData = true;
                //refreshLayout.setEnableLoadMore(true);
            }
            if (s.size() > 0) {
                assetList.addAll(s);
                listAdapter.notifyDataSetChanged();
            }
            refreshLayout.finishLoadMore(500/*,false*/);//传入false表示加载失败
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<DisposeAsset> s) {
            super.onCancelled(s);
            hideProgressDialog();
        }
    }

    private class RemoveDisposeAssetsTask extends AsyncTask<DisposeAsset, Void, String> {
        private DisposeAsset[] assets;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在提交");
        }

        @Override
        protected String doInBackground(DisposeAsset... params) {
            assets = params;
            StringBuilder stringBuilder = new StringBuilder();
            for (DisposeAsset asset : assets) {
                stringBuilder = stringBuilder.append(asset.getCode().concat(","));
            }
            String docIds = stringBuilder.substring(0, stringBuilder.length() - 1);
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_DELETE(), dispose.getCode(), docIds));
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return null;
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (s.contains("\"result\":\"OK\"") || s.contains("OK")) {
                refreshData();
            } else {
                for (DisposeAsset asset : assets) {
                    asset.setStatus(2);
                    asset.setDispose(dispose.getCode());
                    asset.save();
                }
                showShortText("删除失败，".concat(s));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            hideProgressDialog();
        }
    }


    private class SearchDisposeAssetsOfflineTask extends AsyncTask<Void, Void, List<DisposeAsset>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在检索");
        }

        @Override
        protected List<DisposeAsset> doInBackground(Void... params) {
            try{
                String code = dispose.getCode();
                // 分页查询
                String limit = offset + "," + pageSize;
                List<DisposeAsset> itemList = DisposeAsset.find(DisposeAsset.class, " PARENT_CODE = '" + code + "' ", null, null, "ID ASC", limit);
                if (itemList.isEmpty()) {
                    return new ArrayList<DisposeAsset>();
                }
                assetList.addAll(itemList);
                // 更新hasMoreData和offset
                if (itemList.size() >= pageSize) {
                    hasMoreData = true;
                    offset += pageSize; // 下一页偏移量
                } else {
                    hasMoreData = false;
                }
                refreshLayout.finishLoadMore(hasMoreData ? 500 : 100);
                return itemList;
            }catch (Exception e){
                Log.e("SearchDisposeAssetsOfflineTask error: ", e.getMessage());
            }
            return null;

        }

        @Override
        protected void onPostExecute(List<DisposeAsset> s) {
            super.onPostExecute(s);
            try {
                hideProgressDialog();
                listAdapter.notifyDataSetChanged();
                refreshLayout.finishLoadMore(500);
            }catch (Exception e){
                Log.e("SearchDisposeAssetsOfflineTask error: ", e.getMessage());
            }

        }
    }

    private class RemoveDisposeAssetsOfflineTask extends AsyncTask<DisposeAsset, Void, String> {
        private DisposeAsset[] assets;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在提交");
        }

        @Override
        protected String doInBackground(DisposeAsset... params) {
            assets = params;
            // 通过assets直接删除数据库数据
            for (DisposeAsset asset : assets) {
                asset.delete();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            refreshData();
        }

    }


    class ListViewAndHeadViewTouchLinstener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View arg0, MotionEvent motionEvent) {
            //当在列头 和 listView控件上touch时，将这个touch的事件分发给 ScrollView
            HorizontalScrollView headSrcrollView = (HorizontalScrollView) mHead
                    .findViewById(R.id.scrollView);
            headSrcrollView.onTouchEvent(motionEvent);
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
       /* if (barcode2DWithSoft == null) {
            barcode2DWithSoft = Barcode2DWithSoft.getInstance();
        }*/
        new InitTask().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeScanDevice();
    }

    private void closeScanDevice() {
        /*if (barcode2DWithSoft != null) {
            barcode2DWithSoft.stopScan();
            barcode2DWithSoft.close();
        }*/
        stop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 139) {
            if (event.getRepeatCount() == 0) {
                ScanBarcode();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 139) {
            if (event.getRepeatCount() == 0) {
//                barcode2DWithSoft.stopScan();
                stop();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void ScanBarcode() {start();}

    private void onScanResult(String barCode) {
        Message msgScanResult = new Message();
        try {
            if (barCode.toUpperCase().startsWith("HTTP")) {
                if (barCode.toUpperCase().contains("BARCODE")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BARCODE") + 8);
                } else if (barCode.toUpperCase().contains("BID")) {
                    barCode = barCode.substring(barCode.toUpperCase().indexOf("BID") + 4);
                }
            } else if (barCode.length() > 25 && barCode.contains("条码编号：")) {
                barCode = barCode.substring(5, barCode.indexOf("\n"));
            }
            showShortText(barCode);
            msgScanResult.arg1 = 1;
            msgScanResult.obj = barCode;
        } catch (Exception ex) {
            msgScanResult.arg1 = 0;
            msgScanResult.obj = ex.getMessage();
        }
        scanCodeHandler.sendMessage(msgScanResult);
    }

    private void start() {
        barcodeDecoder.startScan();
    }

    private void stop() {
        barcodeDecoder.stopScan();
    }

    private void open() {
        barcodeDecoder.open(this);

        barcodeDecoder.setDecodeCallback(new BarcodeDecoder.DecodeCallback() {
            @Override
            public void onDecodeComplete(BarcodeEntity barcodeEntity) {
                if (barcodeEntity.getResultCode() == BarcodeDecoder.DECODE_SUCCESS) {
                    String barCode = barcodeEntity.getBarcodeData();
                    byte[] bytes = barcodeEntity.getBarcodeBytesData();
                    Message msgScanResult = new Message();
                    try {
//                        barCode = new String(bytes, seldata);
                        if (barCode.toUpperCase().startsWith("HTTP") && barCode.toUpperCase().contains("BARCODE")) {   // 网络型二维码
                            barCode = barCode.substring(barCode.toUpperCase().indexOf("BARCODE") + 8);
                        } else if (barCode.length() > 25) {
                            if (barCode.contains("条码编号：")) {
                                barCode = barCode.substring(5, barCode.indexOf("\n"));
                            }
                        }
                        showShortText(barCode);
                        msgScanResult.arg1 = 1;
                        msgScanResult.obj = barCode;
                    } catch (Exception ex) {
                        msgScanResult.arg1 = 0;
                        msgScanResult.obj = ex.getMessage();
                    }
                    scanCodeHandler.sendMessage(msgScanResult);
                }
            }
        });
    }

    private void close() {
        barcodeDecoder.close();
    }


    /**
     * 设备上电异步类
     *
     * @author liuruifeng
     */
    class InitTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            boolean reuslt = false;
            /*if (barcode2DWithSoft != null) {
                reuslt = barcode2DWithSoft.open(mContext);
                Log.e("barcode2DWithSoft", "open=" + reuslt);
            }*/
            open();
            return reuslt;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mypDialog.cancel();

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mypDialog = new ProgressDialog(mContext);
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("正在初始化设备");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.show();
        }

    }

    public class ScanCodeHandler extends Handler {
        private final WeakReference<DisposeAssetsActivity> mActivty;

        private ScanCodeHandler(DisposeAssetsActivity mActivty) {
            this.mActivty = new WeakReference<DisposeAssetsActivity>(mActivty);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DisposeAssetsActivity activity = mActivty.get();
            if (msg.arg1 == 1) {
                String barCode = msg.obj.toString();
                if (activity != null) {
                    if (activity.application.getLoginAccount() == null) {
                        activity.showShortText("登录失效，请退出重新登录");
                        return;
                    }
                    List<Assets> assetsList = Assets.find(Assets.class, "BARCODEID=? AND ACCTSUITEID=?", barCode, activity.application.getLoginAccount().getZTBH());
                    if (assetsList.isEmpty()) {
                        activity.showShortText("未找到此编码资产！");
                    } else {
                        Assets assets = assetsList.get(0);
                        String code = dispose.getCode();
                        String docId = assets.getDOCID();
                        if (!activity.isOffline) {
                            // 更新接口
                            activity.requestDisposeAsset(docId);
                        }
                        else{
                            // 离线版本直接保存到数据库
                            DisposeAsset disposeAsset = new DisposeAsset();
                            disposeAsset.setCode(docId);
                            disposeAsset.setParentCode(code);
                            disposeAsset.setName(assets.getASSETSNAME());
                            disposeAsset.setSerial(assets.getBARCODEID());
                            disposeAsset.setSpecification(assets.getASSETSSTANDARD());
                            disposeAsset.setOwner(assets.getASSETSUSER());
                            disposeAsset.setPlace(assets.getASSETSLAYADD());
                            disposeAsset.setPrice(assets.getASSETSCURRPRICE());
                            disposeAsset.save();

                            assetList.add(disposeAsset);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
            } else {
                activity.showShortText("扫码失败！");
            }
        }
    }
}