package com.gsoft.inventory.utils;

import java.util.Date;

public class FileBean {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getDateStr() {
        return DataConvert.FormatDate2HMS(date * 1000);
    }

    private String name;
    private long size;
    private String path;
    private long date;
}
