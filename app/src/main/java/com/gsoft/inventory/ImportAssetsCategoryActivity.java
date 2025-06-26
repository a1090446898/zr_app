package com.gsoft.inventory;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.gsoft.inventory.adapter.FileListAdapter;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DataTransmitHelper;
import com.gsoft.inventory.utils.FileBean;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class ImportAssetsCategoryActivity extends BaseCompatActivity {

    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_assets_category);
        initializeView();
    }

    private QMUIPullRefreshLayout mPullRefreshLayout;
    private ListView bt_list;
    private FileListAdapter fileListAdapter;
    private List<FileBean> fileArrayList;
    QMUITopBar topBar;

    @Override
    public void initializeView() {
        topBar = (QMUITopBar) findViewById(R.id.topbar);
        topBar.setTitle("导入资产分类");
        topBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mPullRefreshLayout = (QMUIPullRefreshLayout) findViewById(R.id.pull_to_refresh);
        bt_list = (ListView) findViewById(R.id.bt_list);
        fileArrayList = new ArrayList<>();
        fileListAdapter = new FileListAdapter(fileArrayList, mContext);
        bt_list.setAdapter(fileListAdapter);
        mPullRefreshLayout.setOnPullListener(new QMUIPullRefreshLayout.OnPullListener() {
            @Override
            public void onMoveTarget(int offset) {

            }

            @Override
            public void onMoveRefreshView(int offset) {

            }

            @Override
            public void onRefresh() {
                mPullRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fileArrayList = DataTransmitHelper.searchImportFiles(mContext);
                        mPullRefreshLayout.finishRefresh();
                    }
                }, 200);
            }
        });
        new SearchImportFileTask().execute();
        bt_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                new ImportAssetsTask().execute(fileArrayList.get(i).getPath());
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (resultCode != Activity.RESULT_OK) {
            Log.e("ImportAssets", "onActivityResult() error, resultCode: " + resultCode);
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (requestCode == FILE_SELECT_CODE) {
            Uri uri = data.getData();
            Log.i("ImportAssets", "------->" + uri.getPath());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private class ImportAssetsTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DataTransmitHelper.clearAssetsCategory();
            showProgressDialog("", "正在导入文件");
        }

        @Override
        protected String doInBackground(String... params) {
            String filePath = params[0];
            return DataTransmitHelper.importAssetsCategory(filePath);
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

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (TextUtils.isEmpty(s)) {
                showShortText("数据导入成功！");
            } else {
                showShortText(s);
            }
        }
    }

    private class SearchImportFileTask extends AsyncTask<Void, Void, List<FileBean>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("", "搜索导入文件");
        }

        @Override
        protected List<FileBean> doInBackground(Void... params) {
            return DataTransmitHelper.searchImportFiles(mContext);
        }

        @Override
        protected void onPostExecute(List<FileBean> s) {
            super.onPostExecute(s);
            hideProgressDialog();
            fileArrayList.addAll(s);
            fileListAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<FileBean> fileBeans) {
            super.onCancelled(fileBeans);
            hideProgressDialog();
        }
    }
}
