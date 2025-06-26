package com.gsoft.inventory.utils;

import android.text.TextUtils;

import com.orm.SugarRecord;

import java.io.Serializable;

public class AssetsCategory extends SugarRecord implements Serializable {

    private int NEWOROLD;

    private String CODE;
    private String NAME;
    private int LEVEL;

    public String getCODE() {
        return CODE;
    }

    public void setCODE(String CODE) {
        this.CODE = CODE;
    }

    public int getNewOrOld() {
        return NEWOROLD;
    }

    public void setNewOrOld(int newOrOld) {
        this.NEWOROLD = newOrOld;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public int getLEVEL() {
        return LEVEL;
    }

    public void setLEVEL(int LEVEL) {
        this.LEVEL = LEVEL;
    }

    public Boolean getISSHOW() {
        return ISSHOW;
    }

    public void setISSHOW(Boolean ISSHOW) {
        this.ISSHOW = ISSHOW;
    }

    private Boolean ISSHOW;

    public String getDEFUNIT() {
        return DEFUNIT;
    }

    public void setDEFUNIT(String DEFUNIT) {
        this.DEFUNIT = DEFUNIT;
    }

    private String DEFUNIT;

    @Override
    public String toString() {
        return NAME == null ? "" : NAME;
    }


}
