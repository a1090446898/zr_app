package com.gsoft.inventory.entities;

import java.io.Serializable;

public class SignaturePerson implements Serializable {
    public SignaturePerson() {
    }

    public SignaturePerson(String name) {
        this.name = name;
        this.status = "未确认";
        this.amount = 1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if ("1".equals(status))
            this.status = "已签名";
    }

    private String name;

    public String getDepart() {
        return depart;
    }

    public void setDepart(String depart) {
        this.depart = depart;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    private String depart;
    private String telephone;
    private int amount;
    private String status = "未确认";
}
