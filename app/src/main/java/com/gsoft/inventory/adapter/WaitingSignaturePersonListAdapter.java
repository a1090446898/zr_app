package com.gsoft.inventory.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gsoft.inventory.R;
import com.gsoft.inventory.SignaturePhotoViewActivity;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.utils.HttpRequestUtils;
import com.gsoft.inventory.utils.StringCallback;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import org.w3c.dom.Text;

import java.util.List;

public class WaitingSignaturePersonListAdapter extends BaseAdapter {

    private List<SignaturePerson> personList;

    private LayoutInflater mInflator;
    private Context mContext;

    public WaitingSignaturePersonListAdapter(List<SignaturePerson> records, Context context) {
        super();
        personList = records;
        mContext = context;
        mInflator = LayoutInflater.from(context);
    }

    public void setSendMessageCallBack(StringCallback sendMessageCallBack) {
        this.sendMessageCallBack = sendMessageCallBack;
    }

    StringCallback sendMessageCallBack;

    @Override
    public int getCount() {
        return personList.size();
    }

    @Override
    public Object getItem(int i) {
        return personList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // TODO Auto-generated method stub
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

                viewContainer.button_signature = view.findViewById(R.id.button_signature);
                view.setTag(viewContainer);
            }
        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        final SignaturePerson dispose = personList.get(i);
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
    }
}
