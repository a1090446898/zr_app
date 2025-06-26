package com.gsoft.inventory.entities;

/**
 * @author 10904
 */
public class DressManage {

    private String docId;


    /**
     * 领用部门
     */
    private String receiveDepart;

    /**
     * 领用人
     */
    private String receiveUser;

    /**
     * 领用时间
     */
    private String receiveTime;

    /**
     * 领用地点
     */
    private String receivePlace;

    /**
     * 发放人
     */
    private String giveUser;

    /**
     * 发放时间
     */
    private String giveTime;


    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getReceiveDepart() {
        return receiveDepart;
    }

    public void setReceiveDepart(String receiveDepart) {
        this.receiveDepart = receiveDepart;
    }

    public String getReceiveUser() {
        return receiveUser;
    }

    public void setReceiveUser(String receiveUser) {
        this.receiveUser = receiveUser;
    }

    public String getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(String receiveTime) {
        this.receiveTime = receiveTime;
    }

    public String getReceivePlace() {
        return receivePlace;
    }

    public void setReceivePlace(String receivePlace) {
        this.receivePlace = receivePlace;
    }

    public String getGiveUser() {
        return giveUser;
    }

    public void setGiveUser(String giveUser) {
        this.giveUser = giveUser;
    }

    public String getGiveTime() {
        return giveTime;
    }

    public void setGiveTime(String giveTime) {
        this.giveTime = giveTime;
    }
}
