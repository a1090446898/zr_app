package com.gsoft.inventory;

import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gsoft.inventory.adapter.AssetsListAdapter;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.SysConfig;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;

public class QueryActivity extends BaseCompatActivity {
    RelativeLayout mHead;
    ArrayList<Assets> assetsList;
    AssetsListAdapter assetsAdapter;
    ListView listAssets;

    @BindView(R.id.topbar)
    QMUITopBar topBar;



    @BindView(R.id.tv_assets_msg)
    TextView tv_assets_msg;


    String[] ztArray = null;
    /*资产类别*/
    @BindArray(R.array.zclb)
    String[] zclbArray;
    /*编制情况*/
    @BindArray(R.array.bzqk)
    String[] bzqkArray;

    @BindView(R.id.refreshLayout)
    SmartRefreshLayout refreshLayout;

    int pageSize = 15;
    boolean hasMoreData = false;
    private int mCurrentDialogStyle = com.qmuiteam.qmui.R.style.QMUI_Dialog;

    long maxAssetsID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        topBar.setTitle("查看资产信息");
        topBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ztArray = new String[SysConfig.getZTList().size()];
        SysConfig.getZTList().toArray(ztArray);

        tv_assets_msg.setText("当前共有资产：" + Assets.count(Assets.class) + "条");


        //icon_right.setBounds(0, 0, 60, 60);//必须设置图片的大小否则没有作用

        mHead = (RelativeLayout) findViewById(R.id.head);
        mHead.setFocusable(true);
        mHead.setClickable(true);
        mHead.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());
        listAssets = (ListView) findViewById(R.id.listAssets);

        assetsList = new ArrayList<Assets>();
        assetsAdapter = new AssetsListAdapter(assetsList, mContext, mHead, null);
        listAssets.setOnTouchListener(new ListViewAndHeadViewTouchLinstener());
        listAssets.setAdapter(assetsAdapter);
        //new SearchAssetsTask().execute();

        //lyDrawLayout.openDrawer(lyQuery);
        //refreshLayout.setEnableRefresh(false);
        //refreshLayout.setEnableAutoLoadMore(false);
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                if (hasMoreData)
                    new SearchAssetsTask().execute();
                else
                    refreshLayout.finishLoadMore(100);
            }
        });
        new SearchAssetsTask().execute();
    }

    private class SearchAssetsTask extends AsyncTask<Void, Void, List<Assets>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            showProgressDialog("", "正在检索资产");
        }

        @Override
        protected List<Assets> doInBackground(Void... params) {

            StringBuilder queryWhere = new StringBuilder();
            queryWhere.append(" Id > " + maxAssetsID);
            List<String> whereParams = new ArrayList<>();

            String[] whereArray = new String[whereParams.size()];
            whereParams.toArray(whereArray);
            return Assets.find(Assets.class, queryWhere.toString(), whereArray, null, "Id ASC", pageSize + "");
        }

        @Override
        protected void onPostExecute(List<Assets> s) {
            super.onPostExecute(s);
            hideProgressDialog();
            if (s.size() < pageSize) {
                hasMoreData = false;
                //refreshLayout.setEnableLoadMore(false);
            } else {
                hasMoreData = true;
                //refreshLayout.setEnableLoadMore(true);
            }
            if (!s.isEmpty()) {
                assetsList.addAll(s);
                maxAssetsID = s.get(s.size() - 1).getId();
                assetsAdapter.notifyDataSetChanged();
            }
            refreshLayout.finishLoadMore(500/*,false*/);//传入false表示加载失败
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<Assets> s) {
            super.onCancelled(s);
            hideProgressDialog();
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
}
