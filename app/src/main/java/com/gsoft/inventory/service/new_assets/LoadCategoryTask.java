package com.gsoft.inventory.service.new_assets;

import android.content.Context;
import com.gsoft.inventory.const_data.Log_file_name;
import com.gsoft.inventory.data.CategoryData;
import com.gsoft.inventory.utils.AssetsCategory;
import com.gsoft.inventory.utils.AssetsCategoryHelper;
import com.gsoft.inventory.utils.LogToFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author 10904
 */
public class LoadCategoryTask implements Callable<CategoryData> {
    private String assetsType;

    private Context context;

    public LoadCategoryTask() {
    }
    public LoadCategoryTask( Context context, String assetsType) {
        this.assetsType = assetsType;
        this.context = context;
    }
    @Override
    public CategoryData call() throws Exception {
        CategoryData categoryData = null;
        try{
            categoryData = loadCategoryData(assetsType);
        }catch (Exception e){
            LogToFile.toLogFile(context, Log_file_name.LOG_LOAD_CATEGORY_TASK_FILE_NAME, e.getMessage());
        }
        return categoryData;
    }

    private CategoryData loadCategoryData(String assetsType) {
        List<AssetsCategory> rootCategoryArray = AssetsCategoryHelper.getAssetsCategoryRootList(assetsType);
        List<List<AssetsCategory>> secondCategoryArray = new ArrayList<>();
        List<List<List<AssetsCategory>>> thirdCategoryArray = new ArrayList<>();

        if (rootCategoryArray != null) {
            for (AssetsCategory root : rootCategoryArray) {
                List<AssetsCategory> secondList = AssetsCategoryHelper.getAssetsCategoryList(root);
                secondCategoryArray.add(secondList);

                List<List<AssetsCategory>> thirdList = new ArrayList<>();
                for (AssetsCategory second : secondList) {
                    List<AssetsCategory> thirdListItem = AssetsCategoryHelper.getAssetsCategoryList(second);
                    thirdList.add(thirdListItem);
                }
                thirdCategoryArray.add(thirdList);
            }
        }

        return new CategoryData(rootCategoryArray, secondCategoryArray, thirdCategoryArray);
    }

}
