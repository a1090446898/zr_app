package com.gsoft.inventory;

import android.content.Context;

import androidx.viewpager.widget.PagerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.chrisbanes.photoview.PhotoView;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.HackyViewPager;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PhotoViewActivity extends BaseCompatActivity {

    @BindView(R.id.view_pager)
    HackyViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);
        ButterKnife.bind(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        ArrayList<String> imageList = getIntent().getStringArrayListExtra(SysDefine.IntentExtra_PHOTOIMAGEARRAY);
        if (imageList != null && imageList.size() > 0) {
            while (imageList.contains("")) {
                imageList.remove("");
            }
            viewPager.setAdapter(new SamplePagerAdapter(mContext, imageList));
        } else {
            showShortText("暂无照片");
        }

    }

    static class SamplePagerAdapter extends PagerAdapter {
        List<String> imageArray;
        Context context;

        public SamplePagerAdapter(Context context, List<String> arrImage) {

            this.context = context;
            imageArray = arrImage;
        }

        @Override
        public int getCount() {
            return imageArray.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(container.getContext());
            String fileUrl = imageArray.get(position);
            if (StringUtils.isNullOrEmpty(fileUrl)) return photoView;
            //Uri uri = null;

            try {
               /* if (fileUrl.toLowerCase(Locale.ROOT).startsWith("http")) {
                    uri = Uri.parse(fileUrl);
                } else {
                    uri = Uri.fromFile(new File(imageArray.get(position)));
                }*/

                //photoView.setImageURI(uri);

                Glide.with(context)
                        .load(fileUrl)
                        .fitCenter()
                        .placeholder(R.mipmap.ic_xct)
                        .error(R.mipmap.ic_xct)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(photoView);

                // Now just add PhotoView to ViewPager and return it
                container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                return photoView;
            } catch (Exception ex) {
                Toast.makeText(container.getContext(), "照片预览失败，".concat(ex.getMessage() + ""), Toast.LENGTH_SHORT).show();
            }
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
}
