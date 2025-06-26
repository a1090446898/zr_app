package com.gsoft.inventory.utils;


/**
 * Created by yumingyue on 2016/12/2.
 */

public class BluetoothDeviceDetail {
    public String address;
    public String name;
    public int rssi;

    public BluetoothDeviceDetail(String name, String address, int rssi){
        this.address = address;
        this.name = name;
        this.rssi = rssi;
    }


    public void setDetail(String name, String address, int rssi){
        this.address = address;
        this.name = name;
        this.rssi = rssi;
    }
}
