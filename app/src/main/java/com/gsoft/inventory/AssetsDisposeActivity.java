package com.gsoft.inventory;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.adapter.AssetsDisposeListAdapter;
import com.gsoft.inventory.entities.AssetDispose;
import com.gsoft.inventory.entities.DisposeAsset;
import com.gsoft.inventory.pojo.DisposePojo;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DataTransmitHelper;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssetsDisposeActivity extends BaseCompatActivity {

    AssetsDisposeListAdapter listAdapter;
    List<AssetDispose> assetDisposes;

    private List<DisposePojo> disPosePojoList;
    private Context context;
    ListView listView;
    private Gson gson = new Gson();
    SmartRefreshLayout refreshLayout;

    int pageSize = 15;
    private int offset = 0; // 初始偏移量为0（第一页）
    boolean hasMoreData = false;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;
    /**
     * 是否离线模式
     */
    private boolean isOffline = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_dispose);
        context = this;

        // 获取Intent的值
        isOffline = getIntent().getBooleanExtra("isOffline", false);

        initializeView();
    }

    @Override
    public void initializeView() {
        QMUITopBar topBar = findViewById(R.id.topbar);
        topBar.setTitle("资产处置申请查询");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        // 如果是离线模式
        if(isOffline){
            topBar.addRightTextButton("导入", R.id.topbar_right_about_button).setOnClickListener(v -> {
                // 导入处理
                new ImportJsonTask().execute();
            });
            topBar.addRightTextButton("导出", R.id.topbar_right_change_button).setOnClickListener(v -> {
                // 导出处理
                new ExportJsonTask().execute();
            });
        }

        refreshLayout = findViewById(R.id.refreshLayout);
        listView = findViewById(R.id.listAssets);
        assetDisposes = new ArrayList<>();
        listAdapter = new AssetsDisposeListAdapter(assetDisposes, context);

        View headerView = getLayoutInflater().inflate(R.layout.list_assets_dispose_item, null);
        listView.addHeaderView(headerView);
        listView.setAdapter(listAdapter);
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                if (hasMoreData){
                    if(isOffline){
                        new GetDbAssetDisposeTask().execute();
                    }
                    else{
                        new SearchAssetDisposeTask().execute();
                    }
                }
                else{
                    refreshLayout.finishLoadMore(100);
                }
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            AssetDispose dispose = assetDisposes.get(position - 1);
            Intent intent = new Intent(context, DisposeAssetsActivity.class);
            intent.putExtra("isOffline", isOffline);
            intent.putExtra(SysDefine.TRANS_DISPOSE_CODE, dispose);
            startActivity(intent);
        });

        if(isOffline){
            // 读取数据库
            new GetDbAssetDisposeTask().execute();
        }else{
            new SearchAssetDisposeTask().execute();
        }
    }



    private class SearchAssetDisposeTask extends AsyncTask<Void, Void, List<AssetDispose>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            showProgressDialog("", "正在检索资产");
        }

        @Override
        protected List<AssetDispose> doInBackground(Void... params) {
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_APPLY(), application.getLoginAccount().getYHBH()));
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return null;
            }
            List<AssetDispose> disposes = new ArrayList<>();
            try {
                List<String> results = gson.fromJson(responseString, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.isEmpty()) {
                    return null;
                }
                for (String dataLine : results) {
                    AssetDispose dispose = DataTransmitHelper.addAssetDispose(dataLine);
                    if (dispose != null) disposes.add(dispose);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return disposes;
        }

        @Override
        protected void onPostExecute(List<AssetDispose> s) {
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
                assetDisposes.addAll(s);
                listAdapter.notifyDataSetChanged();
            }
            refreshLayout.finishLoadMore(500);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<AssetDispose> s) {
            super.onCancelled(s);
            hideProgressDialog();
        }
    }


    private class ExportJsonTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在导出中……");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            disPosePojoList = new ArrayList<>();
            assetDisposes = AssetDispose.listAll(AssetDispose.class);
            for(AssetDispose assetDispose : assetDisposes){
                DisposePojo disPosePojo = new DisposePojo();
                disPosePojo.setCode(assetDispose.getCode());
                disPosePojo.setName(assetDispose.getName());
                disPosePojo.setDate(assetDispose.getDate());
                disPosePojo.setDepart(assetDispose.getDepart());
                disPosePojo.setRemark(assetDispose.getRemark());

                String code = assetDispose.getCode();
                // 通过code查询资产详情
/*                String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_DISPOSE_LIST, code));
                if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                    return null;
                }
                List<DisposeAsset> disposes = new ArrayList<>();
                try {
                    List<String> results = gson.fromJson(responseString, new TypeToken<List<String>>() {
                    }.getType());
                    if (results == null || results.isEmpty()) {
                        return null;
                    }
                    for (String dataLine : results) {
                        DisposeAsset dispose = DataTransmitHelper.addDisposeAsset(dataLine);
                        if (dispose != null) disposes.add(dispose);
                    }
                    disPosePojo.setDisposeAssetList(disposes);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/

                List<DisposeAsset> disposeAssets = DisposeAsset.find(DisposeAsset.class, " PARENT_CODE = '" +code + "'");
                disPosePojo.setDisposeAssetList(disposeAssets);
                disPosePojoList.add(disPosePojo);
            }
            // 将disPosePojoList转换成json字符串
            String jsonString = gson.toJson(disPosePojoList);
            // 保存为json文件，写入到本地
            String path =  mContext.getExternalFilesDir(null) + "/dispose";

            File pathFile = new File(path);
            if (!pathFile.exists()) {
                boolean created = pathFile.mkdirs();
                if (!created) {
                    // UI线程执行
                    runOnUiThread(() -> {
                        showShortText("创建dispose目录失败");
                    });
                }
            }
            // 在目录下创建json文件，如果存在该文件则覆盖
            try {
                File jsonFile = new File(pathFile, "dispose.json");
                if (jsonFile.exists()) {
                    jsonFile.delete();
                }
                jsonFile.createNewFile();
                // 将json字符串写入到json文件中
                FileWriter writer = new FileWriter(jsonFile);
                writer.write(jsonString);
                writer.close();
            }catch (IOException e){
                Log.d("报错： ", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            hideProgressDialog();
        }
    }


    private class ImportJsonTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "正在导入中……");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                // 读取本地json文件
                String path =  mContext.getExternalFilesDir(null) + "/dispose/dispose.json";
                File jsonFile = new File(path);
                if (!jsonFile.exists()) {
                    runOnUiThread(() -> {
                        showShortText("找不到dispose.json文件");
                    });
                    return null;
                }
                // 如果存在则读取该json文件
                FileReader reader = new FileReader(jsonFile);
                // 将json字符串转换成disPosePojoList
                disPosePojoList = gson.fromJson(reader, new TypeToken<List<DisposePojo>>() {}.getType());
                // 将数据库数据清除，并重新插入
                AssetDispose.deleteAll(AssetDispose.class);
                DisposeAsset.deleteAll(DisposeAsset.class);
                for(DisposePojo disPosePojo : disPosePojoList){
                    AssetDispose assetDispose = new AssetDispose();
                    String code = disPosePojo.getCode();
                    assetDispose.setCode(code);
                    assetDispose.setName(disPosePojo.getName());
                    assetDispose.setDate(disPosePojo.getDate());
                    assetDispose.setDepart(disPosePojo.getDepart());
                    assetDispose.setRemark(disPosePojo.getRemark());
                    assetDispose.save();
                    List<DisposeAsset> disposeAssetList = disPosePojo.getDisposeAssetList();
                    for(DisposeAsset disposeAsset : disposeAssetList){
                        disposeAsset.setParentCode(code);
                        disposeAsset.save();
                    }
                }
            }catch (IOException e){
                Log.d("报错： ", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            hideProgressDialog();
        }

    }


    private class GetDbAssetDisposeTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String limit = offset + "," + pageSize;
            // 分页查询
            List<AssetDispose> itemList = AssetDispose.find(AssetDispose.class,null ,null, null, "ID ASC", limit);
            if (itemList.isEmpty()) {
                return null;
            }
            assetDisposes.addAll(itemList);
            // 更新hasMoreData和offset
            if (itemList.size() >= pageSize) {
                hasMoreData = true;
                offset += pageSize; // 下一页偏移量
            } else {
                hasMoreData = false;
            }
            refreshLayout.finishLoadMore(hasMoreData ? 500 : 100);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            listAdapter.notifyDataSetChanged();
        }
    }
}