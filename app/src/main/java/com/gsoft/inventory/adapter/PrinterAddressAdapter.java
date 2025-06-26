package com.gsoft.inventory.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dothantech.printer.IDzPrinter;
import com.dothantech.printer.IDzPrinter.PrinterAddress;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BluetoothDeviceDetail;

import java.util.List;

public class PrinterAddressAdapter extends BaseAdapter {

    private Context mContext;
    private List<PrinterAddress> printerArray;

    public PrinterAddressAdapter(Context context, List<PrinterAddress> printers) {
        mContext = context;
        printerArray = printers;
    }

    @Override
    public int getCount() {
        return printerArray.size();
    }

    @Override
    public Object getItem(int i) {
        return printerArray.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TabelViewCell tabelViewCell;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.bt_device_item, null);
            tabelViewCell = new TabelViewCell();
            tabelViewCell.tv_name = (TextView) view.findViewById(R.id.tv_bt_name);
            tabelViewCell.tv_addr = (TextView) view.findViewById(R.id.tv_bt_address);
            view.setTag(tabelViewCell);
        } else {
            tabelViewCell = (TabelViewCell) view.getTag();
        }

        PrinterAddress device = printerArray.get(i);
        final String deviceName = device.shownName;
        String deviceAdd = device.macAddress;
        Log.i("anqii", "name=" + deviceName + ", deviceAddr=" + deviceAdd);
        if (deviceAdd == null) {
            tabelViewCell.tv_addr.setText(mContext.getResources().getString(R.string.unknow_address));
        } else {
            tabelViewCell.tv_addr.setText(deviceAdd);
        }
        if (deviceName != null && deviceName.length() > 0) {
            tabelViewCell.tv_name.setText(deviceName);
        } else {
            tabelViewCell.tv_name.setText(mContext.getResources().getString(R.string.unknow_name));
        }
        return view;
    }

    //cell
    public class TabelViewCell {
        TextView tv_name;
        TextView tv_addr;
        TextView tv_rssi;
    }
}
