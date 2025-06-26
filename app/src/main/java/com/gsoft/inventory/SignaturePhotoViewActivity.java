package com.gsoft.inventory;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.viewpager.widget.PagerAdapter;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.utils.*;

import java.util.ArrayList;
import java.util.List;

public class SignaturePhotoViewActivity extends BaseCompatActivity {
    private Gson gson = new Gson();
    HackyViewPager viewPager;
    String phoneCode;

    List<String> photoUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_photo_view);
        initializeView();
    }

    @Override
    public void initializeView() {
        viewPager = findViewById(R.id.view_pager);
        phoneCode = getIntent().getStringExtra(SysDefine.TRANS_SIGNATURE_PHOTO);
        if (StringUtils.isNullOrEmpty(phoneCode)) return;
        photoUrls = new ArrayList<>();
        new LoadSignaturePhotosTask().execute();
    }


    class SamplePagerAdapter extends PagerAdapter {
        List<String> imageArray;

        public SamplePagerAdapter(List<String> arrImage) {
            imageArray = arrImage;
        }

        @Override
        public int getCount() {
            return imageArray.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
//            ImageView photoView = new ImageView(mContext);
            PhotoView photoView = new PhotoView(container.getContext());
            photoView.setBackgroundColor(Color.BLACK);
            Glide.with(mContext).load(SysDefine.SERVERHOST.concat(imageArray.get(position)))
                    .placeholder(R.mipmap.logo)
                    .centerInside()
                    .error(R.mipmap.logo)
                    .into(photoView);
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }

    public void refreshPhotos(List<String> photos) {
        if (photos == null) {
            showShortText("暂无签字照片");
            finish();
            return;
        }
        photoUrls.addAll(photos);
        if (photoUrls != null && photoUrls.size() > 0) {
            while (photoUrls.contains("")) {
                photoUrls.remove("");
            }
            viewPager.setAdapter(new SamplePagerAdapter(photoUrls));
        } else {
            showShortText("暂无照片");
        }
    }

    class LoadSignaturePhotosTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            showProgressDialog("", "正在检索");
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_FETCH_SIGNATURES_LIST(), phoneCode));
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return null;
            }
            List<String> personList = new ArrayList<>();
            try {
                List<List<String>> results = gson.fromJson(responseString, new TypeToken<List<List<String>>>() {
                }.getType());
                if (results == null || results.isEmpty()) {
                    return null;
                }
                for (List<String> dataLine : results) {
                    personList.add(dataLine.get(0));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return personList;
        }

        @Override
        protected void onPostExecute(List<String> s) {
            super.onPostExecute(s);
            hideProgressDialog();
            refreshPhotos(s);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<String> s) {
            super.onCancelled(s);
            hideProgressDialog();
        }
    }
}