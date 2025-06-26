package com.gsoft.inventory.utils;

import com.orm.query.Condition;
import com.orm.query.Select;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssetsCategoryHelper {

    private static final List<String> assetsCategory = new ArrayList<>(Arrays.asList("土地、房屋及建筑物", "通用设备", "专用设备", "文物和陈列品", "图书、档案", "家具、用具及动植物", "无形资产"));
    private static final List<String> assetsCategory_new = new ArrayList<>(Arrays.asList("房屋和构筑物", "设备", "文物和陈列品", "图书和档案", "家具和用具", "特种动植物", "物资", "无形资产"));

    private static int assetsCategoryNewOrOld = -1;

    public static List<String> getAssetsCategoryList() {
        long count = AssetsCategory.count(AssetsCategory.class, "NEWOROLD = ?", new String[]{"1"});
        if(count == 0){
            // 说明是老分类
            assetsCategoryNewOrOld = 0;
            return assetsCategory;
        }
        assetsCategoryNewOrOld = 1;
        return assetsCategory_new;

    }

    public static int getAssetsCategoryNewOrOld() {
        long count = AssetsCategory.count(AssetsCategory.class, "NEWOROLD = ?", new String[]{"1"});
        if(count == 0){
            // 说明是老分类
            assetsCategoryNewOrOld = 0;
            return 0;
        }
        assetsCategoryNewOrOld = 1;
        return 1;

    }

    private static String getCodeValue(AssetsCategory assetsCategory) {
        String codeStr = assetsCategory.getCODE();
        String codeLikeVal = "";
        if(assetsCategoryNewOrOld == 1){
            codeLikeVal = codeStr.substring(0, assetsCategory.getLEVEL() * 2 + 1) + "%";
        }else{
            codeLikeVal = codeStr.substring(0, assetsCategory.getLEVEL() * 2 - 1) + "%";
        }
        return codeLikeVal;
    }

    /**
     * 根据总大类文字获取代码
     *
     * @param categoryTitle
     * @return
     */
    public static String getAssetsCategoryCode(String categoryTitle) {
        List<String> assetsCategoryList = getAssetsCategoryList();
        int cateIndex = assetsCategoryList.indexOf(categoryTitle);
        return cateIndex == -1 ? "" : String.format("%02d", assetsCategoryList.indexOf(categoryTitle) + 1);
    }

    /**
     * 根据总大类代码获取文字
     *
     * @param categoryCode
     * @return
     */
    public static String getAssetsCategoryTitle(String categoryCode) {
        List<String> assetsCategoryList = getAssetsCategoryList();
        int cIndex = -1;
        try {
            String code = StringUtils.leftTrim(categoryCode, '0');
            cIndex = Integer.parseInt(code);
            if (cIndex >= 0 && cIndex < assetsCategoryList.size()) {
                // 索引合法性校验
                return assetsCategoryList.get(cIndex - 1);
            } else {
                // 默认值防止崩溃
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 根据大类名字获取第一级分类
     *
     * @param categoryTitle 原始的七个分类名称（资产类别）
     * @return 资产分类的第一级分类
     */
    public static List<AssetsCategory> getAssetsCategoryRootList(String categoryTitle) {
        List<String> assetsCategoryList = getAssetsCategoryList();
        int cateIndex = assetsCategoryList.indexOf(categoryTitle);
        String codeLikeVal = "";
        if(assetsCategoryNewOrOld == 1){
            codeLikeVal = "A0"+(cateIndex + 1) + "%";
        }else{
            codeLikeVal = (cateIndex + 1) + "%";
        }
        List<AssetsCategory> result = AssetsCategory.find(AssetsCategory.class, "LEVEL=? AND CODE LIKE ?", "2", codeLikeVal);
        return result;
    }

    /**
     * 根据资产分类返回子分类，如果最末级，则返回自身
     *
     * @param assetsCategory 资产分类
     * @return
     */
    public static List<AssetsCategory> getAssetsCategoryList(AssetsCategory assetsCategory) {
        if (assetsCategory.getISSHOW()) {
            return new ArrayList<>(Arrays.asList(assetsCategory));
        }
        String codeLikeVal = getCodeValue(assetsCategory);

        return AssetsCategory.find(AssetsCategory.class, "LEVEL=? AND CODE LIKE ?",
                String.valueOf(assetsCategory.getLEVEL() + 1), codeLikeVal);
    }


    /**
     * 根据资产分类代码Code返回子分类
     *
     * @param categoryCode 资产分类的代码CODE
     * @return
     */
    public static List<AssetsCategory> getAssetsCategoryList(String categoryCode) {
        List<AssetsCategory> assetsCategories = AssetsCategory.find(AssetsCategory.class, "CODE = ?", categoryCode);
        if (assetsCategories == null || assetsCategories.size() == 0) {
            return new ArrayList<>();
        }
        AssetsCategory assetsCategory = assetsCategories.get(0);
        if (assetsCategory.getISSHOW()) {
            return assetsCategories;
        }
        String codeLikeVal = getCodeValue(assetsCategory);

        return AssetsCategory.find(AssetsCategory.class, "LEVEL=? AND CODE LIKE ?",
                String.valueOf(assetsCategory.getLEVEL() + 1), codeLikeVal);
    }



    /**
     * 根据资产分类代码Code返回子分类
     *
     * @param categoryCode 资产分类的代码CODE
     * @return
     */
    public static AssetsCategory getAssetsCategory(String categoryCode) {
        if (StringUtils.isNullOrEmpty(categoryCode)) return null;
        List<AssetsCategory> assetsCategories = AssetsCategory.find(AssetsCategory.class, "CODE = ?", categoryCode);
        if (assetsCategories == null || assetsCategories.size() == 0) {
            return null;
        }
        return assetsCategories.get(0);
    }

    public static AssetsCategory searchCategoryByAssetsName(String asstesName) {
        if (StringUtils.isNullOrEmpty(asstesName)) return null;
        List<AssetsCategory> assetsCategories = AssetsCategory.find(AssetsCategory.class,
                "NAME LIKE ?", new String[]{"%" + asstesName + "%"}, null, "LEVEL DESC", null);
        if (assetsCategories == null || assetsCategories.size() == 0) {
            return null;
        }
        return assetsCategories.get(0);
    }
}
