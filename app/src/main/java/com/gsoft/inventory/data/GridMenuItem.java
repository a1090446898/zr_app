package com.gsoft.inventory.data;

import com.gsoft.inventory.data.GridMenuItem;

import java.util.List;

/**
 * @author 10904
 */
public class GridMenuItem {
    // 菜单标题
    private String title;
    // 菜单图标（目录类型可传0）
    private int iconRes;
    // 是否是目录（true-有子菜单，false-功能项）
    private boolean isDirectory = false;
    // 子菜单列表（仅目录类型有效）
    private List<GridMenuItem> subMenus;
    // 功能类型（仅功能项有效，用于区分具体逻辑）
    private String functionType;

    public GridMenuItem() {
    }

    public GridMenuItem(String title, int iconRes) {
        this.title = title;
        this.iconRes = iconRes;
    }

    // 构造方法（目录）
    public GridMenuItem(String title, boolean isDirectory, int iconRes, List<GridMenuItem> subMenus) {
        this.title = title;
        this.isDirectory = isDirectory;
        this.subMenus = subMenus;
        this.iconRes = iconRes;
    }

    // 构造方法（功能项）
    public GridMenuItem(String title, Integer iconRes, String functionType) {
        this.title = title;
        if(iconRes == null){
            this.iconRes = -1;
        }else{
            this.iconRes = iconRes;
        }
        this.isDirectory = false;
        this.functionType = functionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public List<GridMenuItem> getSubMenus() {
        return subMenus;
    }

    public void setSubMenus(List<GridMenuItem> subMenus) {
        this.subMenus = subMenus;
    }

    public String getFunctionType() {
        return functionType;
    }

    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }
}
