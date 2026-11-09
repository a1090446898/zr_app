package com.gsoft.inventory.view.common;

import com.gsoft.inventory.view.common.query.AssetsQueryCondition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 公共选择资产页的配置。
 * <p>
 * 调用方通过它描述“本次选择的业务范围”，页面本身不关心具体业务：
 * <ul>
 *     <li>固定查询条件：例如批量复制只选未盘资产、补打条码只选待补打资产；</li>
 *     <li>可见筛选项：不同入口只展示自己需要的查询字段；</li>
 *     <li>选择规则：单选/多选、选择上限、查询上限；</li>
 *     <li>回显与文案：初始已选 Id、标题、空态文案。</li>
 * </ul>
 * 账套条件和分页由页面统一处理，调用方只追加业务条件。
 */
public class AssetsPickerOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产名称筛选 */
    public static final int FILTER_ASSETS_NAME = 1;
    /** 条码编号筛选 */
    public static final int FILTER_BARCODE = 1 << 1;
    /** 使用部门筛选 */
    public static final int FILTER_ASSETS_DEPT = 1 << 2;
    /** 管理部门筛选 */
    public static final int FILTER_MANAGE_DEPT = 1 << 3;
    /** 使用人筛选 */
    public static final int FILTER_ASSETS_USER = 1 << 4;
    /** 存放地点筛选 */
    public static final int FILTER_LOCATION = 1 << 5;
    /** 全部筛选项 */
    public static final int FILTER_ALL = FILTER_ASSETS_NAME | FILTER_BARCODE | FILTER_ASSETS_DEPT
            | FILTER_MANAGE_DEPT | FILTER_ASSETS_USER | FILTER_LOCATION;

    private final String title;
    private final int maxSelectCount;
    private final int queryLimit;
    private final boolean singleSelect;
    private final int visibleFilters;
    private final List<AssetsQueryCondition> fixedConditions;
    private final long[] initialSelectedIds;
    private final boolean selectAllEnabled;
    private final boolean validateSelectionOnConfirm;
    private final String emptyText;

    private AssetsPickerOptions(Builder builder) {
        this.title = builder.title;
        this.maxSelectCount = builder.maxSelectCount;
        this.queryLimit = builder.queryLimit;
        this.singleSelect = builder.singleSelect;
        this.visibleFilters = builder.visibleFilters;
        this.fixedConditions = new ArrayList<>(builder.fixedConditions);
        this.initialSelectedIds = builder.initialSelectedIds.clone();
        this.selectAllEnabled = builder.selectAllEnabled;
        this.validateSelectionOnConfirm = builder.validateSelectionOnConfirm;
        this.emptyText = builder.emptyText;
    }

    public String getTitle() {
        return title;
    }

    public int getMaxSelectCount() {
        return maxSelectCount;
    }

    /**
     * 实际选择上限：单选恒为 1
     */
    public int getEffectiveMaxSelectCount() {
        return singleSelect ? 1 : maxSelectCount;
    }

    /**
     * 查询结果上限：只影响列表能加载多少条，不影响选择上限
     */
    public int getQueryLimit() {
        return queryLimit;
    }

    public boolean isSingleSelect() {
        return singleSelect;
    }

    public int getVisibleFilters() {
        return visibleFilters;
    }

    public List<AssetsQueryCondition> getFixedConditions() {
        return fixedConditions;
    }

    public long[] getInitialSelectedIds() {
        return initialSelectedIds.clone();
    }

    /**
     * 单选或上限为 1 时不显示“全选”
     */
    public boolean isSelectAllEnabled() {
        return selectAllEnabled && !singleSelect && getEffectiveMaxSelectCount() > 1;
    }

    /**
     * 确认时是否按固定条件校验已选 Id。
     * 开启后，跨搜索保留但已不在业务范围内的资产会被自动剔除。
     */
    public boolean isValidateSelectionOnConfirm() {
        return validateSelectionOnConfirm;
    }

    public String getEmptyText() {
        return emptyText;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String title = "选择资产";
        private int maxSelectCount = Integer.MAX_VALUE;
        private int queryLimit = Integer.MAX_VALUE;
        private boolean singleSelect;
        private int visibleFilters = FILTER_ALL;
        private final List<AssetsQueryCondition> fixedConditions = new ArrayList<>();
        private long[] initialSelectedIds = new long[0];
        private boolean selectAllEnabled = true;
        private boolean validateSelectionOnConfirm = true;
        private String emptyText = "没有符合条件的资产";

        public Builder title(String title) {
            if (title != null && !title.trim().isEmpty()) {
                this.title = title.trim();
            }
            return this;
        }

        public Builder maxSelectCount(int maxSelectCount) {
            this.maxSelectCount = maxSelectCount <= 0 ? Integer.MAX_VALUE : maxSelectCount;
            return this;
        }

        public Builder queryLimit(int queryLimit) {
            this.queryLimit = queryLimit <= 0 ? Integer.MAX_VALUE : queryLimit;
            return this;
        }

        public Builder singleSelect(boolean singleSelect) {
            this.singleSelect = singleSelect;
            return this;
        }

        public Builder visibleFilters(int visibleFilters) {
            this.visibleFilters = visibleFilters & FILTER_ALL;
            return this;
        }

        public Builder addCondition(AssetsQueryCondition condition) {
            if (condition != null) {
                fixedConditions.add(condition);
            }
            return this;
        }

        public Builder initialSelectedIds(long[] ids) {
            this.initialSelectedIds = ids == null ? new long[0] : ids.clone();
            return this;
        }

        public Builder selectAllEnabled(boolean selectAllEnabled) {
            this.selectAllEnabled = selectAllEnabled;
            return this;
        }

        public Builder validateSelectionOnConfirm(boolean validateSelectionOnConfirm) {
            this.validateSelectionOnConfirm = validateSelectionOnConfirm;
            return this;
        }

        public Builder emptyText(String emptyText) {
            if (emptyText != null && !emptyText.trim().isEmpty()) {
                this.emptyText = emptyText.trim();
            }
            return this;
        }

        public AssetsPickerOptions build() {
            return new AssetsPickerOptions(this);
        }
    }
}
