package com.gsoft.inventory.entities;

import com.gsoft.inventory.utils.StringCallback;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.io.Serializable;

/**
 * @author 10904
 */
public class DisposeAsset extends SugarRecord implements Serializable {

    /**
     * 订单code
     */
    private String parentCode;

    /**
     * 自己的code和订单code不同
     */
    private String code;
    private String name;
    private String serial;
    private String specification;
    private String owner;
    private String place;
    private String price;
    /*docId*/
    private String dispose;


    /**
     * 1新增，2删除
     */
    private int status;


    @Ignore
    private boolean checked;

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    //"Z15875438493245550|便携式计算机|2020020000105|MacBooK|黎天辉|D1106|25800.00"


    public String getDispose() {
        return dispose;
    }

    public void setDispose(String dispose) {
        this.dispose = dispose;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

}
