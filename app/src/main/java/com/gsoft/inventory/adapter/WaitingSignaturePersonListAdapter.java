package com.gsoft.inventory.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.gsoft.inventory.R;
import com.gsoft.inventory.SignaturePhotoViewActivity;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.utils.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WaitingSignaturePersonListAdapter extends BaseAdapter {

    private final List<SignaturePerson> dataList;

    private LayoutInflater mInflator;
    private final Context mContext;
    private boolean isMultiSelectMode = false;
    private OnSelectionChangeListener selectionListener;

    // 修改构造函数，接收 selectedPositions 参数
    public WaitingSignaturePersonListAdapter(List<SignaturePerson> records, Context context) {
        super();
        this.dataList = records;
        this.mContext = context;
        mInflator = LayoutInflater.from(context);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int position);
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
    }

    public void setSelectionListener(OnSelectionChangeListener listener) {
        this.selectionListener = listener;
    }



    public void setSendMessageCallBack(StringCallback sendMessageCallBack) {
        this.sendMessageCallBack = sendMessageCallBack;
    }

    StringCallback sendMessageCallBack;

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int i) {
        return dataList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewContainer viewContainer;

        // General ListView optimization code.
        if (view == null) {
            synchronized (mContext) {
                view = mInflator.inflate(R.layout.list_waiting_signature_item, null);
                viewContainer = new ViewContainer();
                viewContainer.tv_depart = view.findViewById(R.id.tv_depart);
                viewContainer.tv_status = view.findViewById(R.id.tv_status);
                viewContainer.tv_name = view.findViewById(R.id.tv_name);
                viewContainer.tv_look = view.findViewById(R.id.tv_look);
                viewContainer.tv_checkBox = view.findViewById(R.id.tv_checkBox);
                viewContainer.button_signature = view.findViewById(R.id.button_signature);
                viewContainer.button_copy = view.findViewById(R.id.button_copy);
                view.setTag(viewContainer);
            }
        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final SignaturePerson dispose = dataList.get(i);
        viewContainer.tv_depart.setText(dispose.getDepart());
        viewContainer.tv_status.setText(dispose.getStatus());
        viewContainer.tv_name.setText(dispose.getName());

        viewContainer.tv_look.setOnClickListener(v -> {
            if (StringUtils.isNullOrEmpty(dispose.getTelephone())) return;
            Intent intent = new Intent(mContext, SignaturePhotoViewActivity.class);
            intent.putExtra(SysDefine.TRANS_SIGNATURE_PHOTO, dispose.getTelephone());
            mContext.startActivity(intent);
        });

        viewContainer.button_signature.setOnClickListener(v -> {
            HttpRequestUtils.getAsync(String.format(SysDefine.SEND_ASSETS_SIGNATURE_SMS, dispose.getTelephone(), dispose.getName()), sendMessageCallBack);
        });


        viewContainer.button_copy.setOnClickListener(v -> {
            String urlContent = String.format("固定资产确认通知：尊敬的 %s，请点击链接http://www.zrast.com/signature.html?phone=%s核实您名下的固定资产信息，并签字确认。", dispose.getName(), dispose.getTelephone());
            // 将urlContent复制到用户剪切板，并弹框提示已经复制到剪切板
            ClipboardUtils.copy(mContext, urlContent);
        });

        if (viewContainer.tv_checkBox != null) {
            if (isMultiSelectMode) {
                viewContainer.tv_checkBox.setVisibility(View.VISIBLE);
                // 从数据模型获取选中状态
                viewContainer.tv_checkBox.setChecked(dispose.getSelectStatus());
                viewContainer.tv_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // 更新数据模型
                    dispose.setSelectStatus(isChecked);
                    if (selectionListener != null) {
                        // 回调触发给外面方法
                        selectionListener.onSelectionChanged(i);
                    }
                });
            } else {
                viewContainer.tv_checkBox.setVisibility(View.GONE);
            }
        }

        return view;
    }

    public class ViewContainer {
        /*条码编号*/
        TextView tv_name;
        /*资产类别*/
        TextView tv_depart;
        /*资产名称*/
        TextView tv_status;
        TextView tv_look;
        Button button_signature;
        Button button_copy;
        CheckBox tv_checkBox;
    }

    // 全选
    public void toggleSelectAll() {

        // 先判断是否全选，如果没有，则全选，否则取消全选
        boolean isAllSelected = true;
        for (int i = 0; i < dataList.size(); i++) {
            if (!dataList.get(i).getSelectStatus()) {
                isAllSelected = false;
                break;
            }
        }
        if (isAllSelected) {
            for (int i = 0; i < dataList.size(); i++) {
                dataList.get(i).setSelectStatus(false);
            }
        }
        else{
            for (int i = 0; i < dataList.size(); i++) {
                dataList.get(i).setSelectStatus(true);
            }
        }
        notifyDataSetChanged();

    }

    // 提供获取选中项的方法给Activity使用
    public boolean getSelectedStatus() {
        Set<Integer> selected = new HashSet<>();
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).getSelectStatus()) {
                return true;
            }
        }
        return false;
    }

}
