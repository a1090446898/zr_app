package com.gsoft.inventory.data;

import java.io.Serializable;

/**
 * 通用选择项：部门、人员共用
 * <p>
 * name 为展示值（部门名称/姓名），code 为对应编号（部门编号 BMBH/工号 YHBH），
 * subTitle 为副标题，用于人员显示所属部门与工号，避免同名混淆。
 */
public class SelectItem implements Serializable {

    private final String name;
    private final String code;
    private final String subTitle;

    public SelectItem(String name, String code, String subTitle) {
        this.name = name == null ? "" : name;
        this.code = code == null ? "" : code;
        this.subTitle = subTitle == null ? "" : subTitle;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getSubTitle() {
        return subTitle;
    }

    /**
     * 关键字匹配：名称、编号、副标题任一命中即可
     */
    public boolean matches(String keyword) {
        if (keyword == null || keyword.length() == 0) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return name.toLowerCase().contains(lower)
                || code.toLowerCase().contains(lower)
                || subTitle.toLowerCase().contains(lower);
    }
}
