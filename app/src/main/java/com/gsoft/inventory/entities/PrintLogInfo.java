package com.gsoft.inventory.entities;

import com.gsoft.inventory.utils.SysConfig;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrintLogInfo implements Serializable {


    private String account;


    /**
     * 资产条码
     */
    private String barCode;


    private String handheldDeviceSerialNumber;

    /**
     * 设备码
     */
    private String deviceName;


    private String zcqcyName;

    private String zcqcyCode;

    private String version;


    private Date createTime;



    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }


    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }


    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getHandheldDeviceSerialNumber() {
        return handheldDeviceSerialNumber;
    }

    public void setHandheldDeviceSerialNumber(String handheldDeviceSerialNumber) {
        this.handheldDeviceSerialNumber = handheldDeviceSerialNumber;
    }

    public String getZcqcyName() {
        return zcqcyName;
    }

    public void setZcqcyName(String zcqcyName) {
        this.zcqcyName = zcqcyName;
    }

    public String getZcqcyCode() {
        return zcqcyCode;
    }

    public void setZcqcyCode(String zcqcyCode) {
        this.zcqcyCode = zcqcyCode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String toLineString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getAccount()).append("|");
        sb.append(this.getBarCode()).append("|");
        sb.append(this.getHandheldDeviceSerialNumber()).append("|");
        String deviceName1 = this.getDeviceName();
        int firstDashIndex = deviceName1.indexOf('-');
        int secondDashIndex = deviceName1.indexOf('-', firstDashIndex + 1);

        // 提取第二个"-"之前的部分
        if (firstDashIndex != -1 && secondDashIndex != -1) {
            String deviceName1Str = deviceName1.substring(secondDashIndex + 1);
            sb.append(deviceName1Str).append("|");
        }

        Date creatStr = this.getCreateTime();
        // 创建一个SimpleDateFormat对象，指定日期时间格式
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 使用format方法将Date转换为字符串
        String formattedDate = formatter.format(creatStr);
        sb.append(formattedDate).append("|");
        sb.append(this.getZcqcyName()).append("|");
        sb.append(this.getZcqcyCode()).append("|");
        sb.append(this.getVersion()).append("|");

        return sb.substring(0, sb.length() - 1);
    }
}
