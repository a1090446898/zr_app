package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.utils.PhotoStorageHelper;
import com.gsoft.inventory.utils.StringUtils;

import java.util.List;

/**
 * 待上传照片卡片适配器
 * <p>
 * 照片名称作为卡片标题，账套ID/DOCID/条码编号/状态/路径竖向展示；
 * 本地照片文件缺失时在卡片底部追加一行红字提示。
 *
 * @author 10904
 */
public class WaitingUploadPhotoListAdapter extends BaseAdapter {

    private List<AssetPhoto> recordList;

    private LayoutInflater mInflator;
    private Context mContext;

    public WaitingUploadPhotoListAdapter(List<AssetPhoto> records, Context context) {
        super();
        recordList = records;
        mContext = context;
        mInflator = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return recordList.size();
    }

    @Override
    public Object getItem(int i) {
        return recordList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewContainer viewContainer;
        if (view == null) {
            view = mInflator.inflate(R.layout.list_waiting_photo_item, viewGroup, false);
            viewContainer = new ViewContainer();
            viewContainer.tvPhotoName = view.findViewById(R.id.tv_photo_name);
            viewContainer.tvAcctsuiteId = view.findViewById(R.id.tv_acctsuite_id);
            viewContainer.tvDoc = view.findViewById(R.id.tv_doc);
            viewContainer.tvBar = view.findViewById(R.id.tv_bar);
            viewContainer.tvStatus = view.findViewById(R.id.tv_status);
            viewContainer.tvPath = view.findViewById(R.id.tv_path);
            viewContainer.tvPhotoMissing = view.findViewById(R.id.tv_photo_missing);
            view.setTag(viewContainer);
        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        AssetPhoto photo = recordList.get(i);
        viewContainer.tvPhotoName.setText(displayText(photo.getName()));
        viewContainer.tvAcctsuiteId.setText(displayText(photo.getAcctsuiteId()));
        viewContainer.tvDoc.setText(displayText(photo.getDoc()));
        viewContainer.tvBar.setText(displayText(photo.getCode()));
        viewContainer.tvStatus.setText(displayText(photo.getStatus()));
        viewContainer.tvPath.setText(displayText(photo.getPath()));
        // 记录保留、只做提示：用户剪切外部目录后记录仍需可核对，不能静默删除
        viewContainer.tvPhotoMissing.setVisibility(PhotoStorageHelper.isPhotoFileMissing(photo)
                ? View.VISIBLE : View.GONE);
        return view;
    }

    /**
     * 空值统一显示为“-”，避免卡片出现空白项
     */
    private String displayText(String value) {
        return StringUtils.isNullOrEmpty(value) ? "-" : value;
    }

    public static class ViewContainer {
        /*照片名称*/
        TextView tvPhotoName;
        /*账套ID*/
        TextView tvAcctsuiteId;
        /*DOCID*/
        TextView tvDoc;
        /*条码编号*/
        TextView tvBar;
        /*照片状态*/
        TextView tvStatus;
        /*照片路径*/
        TextView tvPath;
        /*照片缺失提示*/
        TextView tvPhotoMissing;
    }
}
