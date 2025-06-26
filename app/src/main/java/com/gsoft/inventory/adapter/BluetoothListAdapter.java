package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BluetoothDeviceDetail;

import java.util.ArrayList;

public class BluetoothListAdapter extends BaseAdapter {

    private ArrayList<BluetoothDeviceDetail> devices;

    private LayoutInflater mInflator;
    private Context mContext;

    public BluetoothListAdapter(ArrayList<BluetoothDeviceDetail> devicesList, Context context) {
        super();
        devices = devicesList;
        mContext = context;
    }

    public void addDevice(BluetoothDeviceDetail device) {
        if (!devices.contains(device)) {
            devices.add(device);
        }
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        // TODO Auto-generated method stub
        TabelViewCell tabelViewCell;
        // General ListView optimization code.
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.bt_device_item, null);
            tabelViewCell = new TabelViewCell();
            tabelViewCell.tv_name = (TextView) view.findViewById(R.id.tv_bt_name);
            tabelViewCell.tv_addr = (TextView) view.findViewById(R.id.tv_bt_address);
            view.setTag(tabelViewCell);
        } else {
            tabelViewCell = (TabelViewCell) view.getTag();
        }

        BluetoothDeviceDetail device = devices.get(position);
        final String deviceName = device.name;
        String deviceAdd = device.address;
        int deviceRssi = device.rssi;
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

        //iBeacon 隐藏或显示
        /*if (deviceRssi <= -100) {
            deviceRssi = -100;
            tabelViewCell.tv_rssi.setTextColor(Color.RED);
        } else if (deviceRssi > 0) {
            deviceRssi = 0;
            tabelViewCell.tv_rssi.setTextColor(Color.GRAY);
        } else {
            tabelViewCell.tv_rssi.setTextColor(Color.GRAY);
        }
        String str_rssi = "(" + deviceRssi + ")";
        if (str_rssi.equals("(-100)")) {
            str_rssi = "null";
        }
        tabelViewCell.tv_rssi.setText(str_rssi);*/
        //tabelViewCell.pb_rssi.setProgress(100 + deviceRssi);
        return view;
    }

    //cell
    public class TabelViewCell {
        TextView tv_name;
        TextView tv_addr;
        TextView tv_rssi;
    }

}
