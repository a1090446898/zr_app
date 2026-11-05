package com.gsoft.inventory.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量复制列表卡片：资产名称/状态标签/DOCID/条码/使用部门/管理部门/使用人/管理人/存放地点
 * <p>
 * 状态标签：待删除(灰，禁止勾选) / 待上传(蓝) / 已同步(绿)；
 * 勾选模式用于批量删除，选中态按资产Id记录（列表分页/复用，不能用position）。
 *
 * @author 10904
 */
public class BatchCopyCardAdapter extends BaseAdapter {

    /** 卡片点击回调 */
    public interface OnItemClickListener {
        void onItemClick(Assets assets);
    }

    /** 勾选变化回调 */
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);

        void onSelectLimitReached(int maxCount);
    }

    private static final int COLOR_DELETED = 0xFF999999;
    private static final int COLOR_NOT_UPLOAD = 0xFF3E8EF7;
    private static final int COLOR_UPLOADED = 0xFF19A15F;

    private final LayoutInflater mInflater;
    private final Context mContext;
    private List<Assets> dataList = new ArrayList<>();
    private String accountZtbh = "";

    private boolean selectMode;
    private final Set<Long> selectedIds = new HashSet<>();
    private int maxSelectCount = Const.BATCH_COPY_DELETE_MAX;
    private OnSelectionChangedListener selectionListener;
    private OnItemClickListener itemClickListener;

    public BatchCopyCardAdapter(Context context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    public void setData(List<Assets> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    public void setAccountZtbh(String ztbh) {
        this.accountZtbh = ztbh == null ? "" : ztbh;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Assets getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_batch_copy_card, parent, false);
            holder = new ViewHolder();
            holder.tvAssetsName = convertView.findViewById(R.id.tvAssetsName);
            holder.tvStatusTag = convertView.findViewById(R.id.tvStatusTag);
            holder.tvDocId = convertView.findViewById(R.id.tvDocId);
            holder.tvBarcode = convertView.findViewById(R.id.tvBarcode);
            holder.tvUseDept = convertView.findViewById(R.id.tvUseDept);
            holder.tvManageDept = convertView.findViewById(R.id.tvManageDept);
            holder.tvUser = convertView.findViewById(R.id.tvUser);
            holder.tvCustodian = convertView.findViewById(R.id.tvCustodian);
            holder.tvLocation = convertView.findViewById(R.id.tvLocation);
            holder.cbSelect = convertView.findViewById(R.id.cbSelect);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Assets assets = dataList.get(position);
        holder.tvAssetsName.setText(StringUtils.isNullOrEmpty(assets.getASSETSNAME())
                ? "未命名资产" : assets.getASSETSNAME());
        holder.tvDocId.setText("DOCID：" + safeText(assets.getDOCID()));
        holder.tvBarcode.setText("条码：" + safeText(assets.getBARCODEID()));
        holder.tvUseDept.setText("使用部门：" + deptText(assets.getASSETSDEPT()));
        holder.tvManageDept.setText("管理部门：" + deptDisplayName(assets.getGROUNDMANAGEDEPT()));
        holder.tvUser.setText("使用人：" + safeText(assets.getASSETSUSER()));
        holder.tvCustodian.setText("管理人：" + safeText(assets.getCUSTODIAN()));
        holder.tvLocation.setText("存放地点：" + locationDisplayName(assets.getASSETSLAYADD()));

        bindStatusTag(holder.tvStatusTag, assets);

        boolean deletable = selectable(assets);
        bindSelection(holder.cbSelect, assets, deletable);

        final CheckBox cbSelect = holder.cbSelect;
        if (selectMode && deletable) {
            convertView.setOnClickListener(v -> cbSelect.toggle());
        } else if (!selectMode) {
            convertView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(assets);
                }
            });
        } else {
            convertView.setOnClickListener(null);
            convertView.setClickable(false);
        }
        return convertView;
    }

    // ==================== 批量删除勾选 ====================

    public void setSelectMode(boolean selectMode) {
        if (this.selectMode == selectMode) {
            return;
        }
        this.selectMode = selectMode;
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public boolean isAllSelected() {
        boolean hasSelectable = false;
        for (Assets assets : dataList) {
            if (!selectable(assets)) {
                continue;
            }
            hasSelectable = true;
            if (!selectedIds.contains(assets.getId())) {
                return false;
            }
        }
        return hasSelectable;
    }

    public void selectAll() {
        boolean limitReached = false;
        for (Assets assets : dataList) {
            if (!selectable(assets) || selectedIds.contains(assets.getId())) {
                continue;
            }
            if (selectedIds.size() >= maxSelectCount) {
                limitReached = true;
                break;
            }
            selectedIds.add(assets.getId());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
        if (limitReached && selectionListener != null) {
            selectionListener.onSelectLimitReached(maxSelectCount);
        }
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /** 已勾选资产（按列表顺序） */
    public List<Assets> getSelectedAssets() {
        List<Assets> result = new ArrayList<>();
        for (Assets assets : dataList) {
            if (selectable(assets) && selectedIds.contains(assets.getId())) {
                result.add(assets);
            }
        }
        return result;
    }

    private boolean selectable(Assets assets) {
        if (assets == null || assets.getId() == null) {
            return false;
        }
        return !Const.REMARKS_TO_DELETE.equals(StringUtils.trim(assets.getREMARKS()));
    }

    private void bindSelection(CheckBox checkBox, final Assets assets, final boolean deletable) {
        if (checkBox == null) {
            return;
        }
        checkBox.setOnCheckedChangeListener(null);
        if (!selectMode || !deletable) {
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
            return;
        }
        checkBox.setVisibility(View.VISIBLE);
        checkBox.setChecked(assets.getId() != null && selectedIds.contains(assets.getId()));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Long id = assets.getId();
            if (id == null) {
                buttonView.setChecked(false);
                return;
            }
            if (isChecked) {
                if (selectedIds.size() >= maxSelectCount) {
                    buttonView.setChecked(false);
                    if (selectionListener != null) {
                        selectionListener.onSelectLimitReached(maxSelectCount);
                    }
                    return;
                }
                selectedIds.add(id);
            } else {
                selectedIds.remove(id);
            }
            notifySelectionChanged();
        });
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedIds.size());
        }
    }

    // ==================== 展示辅助 ====================

    private void bindStatusTag(TextView tagView, Assets assets) {
        String text;
        int color;
        if (Const.REMARKS_TO_DELETE.equals(StringUtils.trim(assets.getREMARKS()))) {
            text = "待删除";
            color = COLOR_DELETED;
        } else if (Const.NOT_UPLOAD.equals(assets.getISUPLOAD())) {
            text = "待上传";
            color = COLOR_NOT_UPLOAD;
        } else if (Const.UPLOADED.equals(assets.getISUPLOAD())) {
            text = "已同步";
            color = COLOR_UPLOADED;
        } else {
            tagView.setVisibility(View.GONE);
            return;
        }
        tagView.setVisibility(View.VISIBLE);
        tagView.setText(text);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(QMUIDisplayHelper.dp2px(mContext, 3));
        background.setColor(color);
        tagView.setBackground(background);
    }

    /** 使用部门：存部门名称，空值显示"未分配部门" */
    private static String deptText(String raw) {
        String value = StringUtils.trim(raw);
        return StringUtils.isNullOrEmpty(value) ? RfidTagAdapter.DEPT_NONE_NAME : value;
    }

    /**
     * 管理部门显示值：按业务口径优先按部门编号(BMBH)转部门名称(BMMC)，
     * 再兼容历史单位编号(DWBH)按账套转单位名称(DWMC)
     */
    private String deptDisplayName(String raw) {
        String value = StringUtils.trim(raw);
        if (StringUtils.isNullOrEmpty(value)) {
            return RfidTagAdapter.DEPT_NONE_NAME;
        }
        String name = AccountHelper.getDepartName(value);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        name = AccountHelper.getDanweiName(accountZtbh, value);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        return value;
    }

    private static String locationDisplayName(String raw) {
        String value = StringUtils.trim(raw);
        return StringUtils.isNullOrEmpty(value) ? RfidTagAdapter.LOCATION_NONE_NAME : value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    static class ViewHolder {
        TextView tvAssetsName;
        TextView tvStatusTag;
        TextView tvDocId;
        TextView tvBarcode;
        TextView tvUseDept;
        TextView tvManageDept;
        TextView tvUser;
        TextView tvCustodian;
        TextView tvLocation;
        CheckBox cbSelect;
    }
}