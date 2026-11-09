package com.gsoft.inventory.view.common;

import android.app.Activity;
import android.content.Intent;

import java.io.Serializable;

/**
 * 公共选择资产页。
 * <p>
 * 调用方通过 {@link AssetsPickerOptions} 描述本次业务范围，不需要关心页面内部实现：
 * <pre>
 *   AssetsPickerOptions options = AssetsPickerOptions.builder()
 *           .title("选择资产")
 *           .maxSelectCount(500)
 *           .queryLimit(500)
 *           .addCondition(AssetsQueryCondition.notEqIgnoringNull("PDZT", "01"))
 *           .initialSelectedIds(ids)
 *           .build();
 *   AssetsPickerActivity.pick(this, REQ_SELECT_ASSETS, options);
 *   long[] ids = AssetsPickerActivity.readSelectedIds(data);
 * </pre>
 */
public class AssetsPickerActivity extends AssetsPickerBaseActivity {

    private static final String EXTRA_OPTIONS = "extra_assets_picker_options";

    @Override
    protected AssetsPickerOptions createOptions() {
        Serializable extra = getIntent().getSerializableExtra(EXTRA_OPTIONS);
        if (extra instanceof AssetsPickerOptions) {
            return (AssetsPickerOptions) extra;
        }
        // 未传配置时使用默认范围：当前账套全部资产、多选、全部筛选项
        return AssetsPickerOptions.builder().build();
    }

    /**
     * 打开选择资产页
     */
    public static void pick(Activity activity, int requestCode, AssetsPickerOptions options) {
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(activity, AssetsPickerActivity.class);
        intent.putExtra(EXTRA_OPTIONS, options == null ? AssetsPickerOptions.builder().build() : options);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 统一解析回传的资产 Id，调用方无需关心 extra key
     */
    public static long[] readSelectedIds(Intent data) {
        if (data == null) {
            return new long[0];
        }
        long[] ids = data.getLongArrayExtra(RESULT_SELECTED_IDS);
        return ids == null ? new long[0] : ids;
    }
}
