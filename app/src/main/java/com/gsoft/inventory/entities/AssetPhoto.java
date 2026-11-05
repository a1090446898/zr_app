package com.gsoft.inventory.entities;

import com.orm.SugarRecord;

import java.io.Serializable;

public class AssetPhoto extends SugarRecord implements Serializable {
    /**
     * 对应资产的编号
     */
    private String code;
    /**
     * 照片名称
     */
    private String name;
    /**
     * 照片上传状态
     */
    private String status;
    /**
     * 对应资产的docId
     */
    private String doc;
    /**
     * 对应资产的用户id
     */
    private String user;

    /**
     * 路径地址
     */
    private String path;

    /**
     * 照片所属账套编号：拍照时写入，迁移/归位时优先使用，避免切换账套后归属错误
     */
    private String acctsuiteId;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }


    public String getAcctsuiteId() {
        return acctsuiteId;
    }

    public void setAcctsuiteId(String acctsuiteId) {
        this.acctsuiteId = acctsuiteId;
    }
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
