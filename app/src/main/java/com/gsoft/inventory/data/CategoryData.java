package com.gsoft.inventory.data;


import com.gsoft.inventory.utils.AssetsCategory;

import java.util.List;

/**
 * @author 10904
 */
public  class CategoryData {
    public List<AssetsCategory> rootCategories;
    public List<List<AssetsCategory>> secondCategories;
    public List<List<List<AssetsCategory>>> thirdCategories;

    public CategoryData(List<AssetsCategory> rootCategories,
                        List<List<AssetsCategory>> secondCategories,
                        List<List<List<AssetsCategory>>> thirdCategories) {
        this.rootCategories = rootCategories;
        this.secondCategories = secondCategories;
        this.thirdCategories = thirdCategories;
    }
}