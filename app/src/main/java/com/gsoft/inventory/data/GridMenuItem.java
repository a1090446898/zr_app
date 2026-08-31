package com.gsoft.inventory.data;

import com.gsoft.inventory.data.GridMenuItem;
import com.gsoft.inventory.utils.StringUtils;

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

    private String webUrl;

    private String badgeCount;

    /**
     * 角标code码
     */
    private String badgeCode;

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

    public GridMenuItem(String title, boolean isDirectory, int iconRes, String functionType, List<GridMenuItem> subMenus) {
        this.title = title;
        this.isDirectory = isDirectory;
        this.subMenus = subMenus;
        this.functionType = functionType;
        this.iconRes = iconRes;
    }


    public GridMenuItem(String title, Integer iconRes, String functionType, String webUrl) {
        this.title = title;
        if(iconRes == null){
            this.iconRes = -1;
        }else{
            this.iconRes = iconRes;
        }
        this.isDirectory = false;
        this.functionType = functionType;
        this.webUrl = webUrl;
        this.badgeCount = "0";
    }

    // 构造方法（功能项）
    public GridMenuItem(String title, Integer iconRes, String functionType, String webUrl, String badgeCount, String badgeCode) {
        this.title = title;
        if(iconRes == null){
            this.iconRes = -1;
        }else{
            this.iconRes = iconRes;
        }
        this.isDirectory = false;
        this.functionType = functionType;
        this.webUrl = webUrl;
        this.badgeCount = badgeCount;
        this.badgeCode = badgeCode;
    }

    public void calculateBadgeCount(){
        // 自动计算子菜单badgeCount
        if (isDirectory && subMenus != null) {
            int count = 0;
            for (GridMenuItem subMenu : subMenus) {
                String badgeCount1 = subMenu.getBadgeCount();
                if("99+".equals(badgeCount1)){
                    count = 100;
                    break;
                }
                if(!StringUtils.isNullOrEmpty(badgeCount1)){
                    try {
                        count += Integer.parseInt(badgeCount1);
                    } catch (NumberFormatException e) {
                        // 忽略无效数字
                    }
                }
            }
            this.badgeCount = count > 99 ? "99+" : String.valueOf(count);
        }
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

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(String badgeCount) {
        this.badgeCount = badgeCount;
    }

    public String getBadgeCode() {
        return badgeCode;
    }

    public void setBadgeCode(String badgeCode) {
        this.badgeCode = badgeCode;
    }
}
