package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 公共选择资产页卡片：勾选框 + 名称/条码/使用部门/存放地点。
 * <p>
 * 选中态按资产 Id 记录，跨搜索/分页不丢失；点击整张卡片切换勾选。
 * 单选模式下点新卡片会替换原选中项。
 */
public class AssetsPickerAdapter extends BaseAdapter {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);

        void onSelectLimitReached(int maxCount);
    }

    private final LayoutInflater mInflater;
    private List<Assets> dataList = new ArrayList<>();
    private final Set<Long> selectedIds = new LinkedHashSet<>();
    private int maxSelectCount = Integer.MAX_VALUE;
    private boolean singleSelect;
    private OnSelectionChangedListener selectionListener;

    public AssetsPickerAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public void setData(List<Assets> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    public void setSelectionListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void setMaxSelectCount(int maxSelectCount) {
        this.maxSelectCount = maxSelectCount <= 0 ? Integer.MAX_VALUE : maxSelectCount;
    }

    public void setSingleSelect(boolean singleSelect) {
        this.singleSelect = singleSelect;
    }

    /** 初始化已选（调用方回传的已选资产） */
    public void setSelectedIds(List<Long> ids) {
        selectedIds.clear();
        if (ids != null) {
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                if (selectedIds.size() >= maxSelectCount) {
                    break;
                }
                selectedIds.add(id);
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    /** 全部已选资产 Id（跨搜索结果） */
    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public boolean isAllSelected() {
        boolean hasData = false;
        for (Assets assets : dataList) {
            if (assets.getId() == null) {
                continue;
            }
            hasData = true;
            if (!selectedIds.contains(assets.getId())) {
                return false;
            }
        }
        return hasData;
    }

    public void selectAll() {
        if (singleSelect) {
            return;
        }
        boolean limitReached = false;
        for (Assets assets : dataList) {
            if (assets.getId() == null || selectedIds.contains(assets.getId())) {
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
            convertView = mInflater.inflate(R.layout.item_assets_picker, parent, false);
            holder = new ViewHolder();
            holder.tvAssetsName = convertView.findViewById(R.id.tvAssetsName);
            holder.tvBarcode = convertView.findViewById(R.id.tvBarcode);
            holder.tvDept = convertView.findViewById(R.id.tvDept);
            holder.tvLocation = convertView.findViewById(R.id.tvLocation);
            holder.cbSelect = convertView.findViewById(R.id.cbSelect);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Assets assets = dataList.get(position);
        holder.tvAssetsName.setText(StringUtils.isNullOrEmpty(assets.getASSETSNAME())
                ? "未命名资产" : assets.getASSETSNAME());
        holder.tvBarcode.setText("条码：" + (assets.getBARCODEID() == null ? "" : assets.getBARCODEID()));
        holder.tvDept.setText("使用部门：" + (StringUtils.isNullOrEmpty(assets.getASSETSDEPT())
                ? RfidTagAdapter.DEPT_NONE_NAME : assets.getASSETSDEPT()));
        holder.tvLocation.setText("存放地点：" + (StringUtils.isNullOrEmpty(assets.getASSETSLAYADD())
                ? RfidTagAdapter.LOCATION_NONE_NAME : assets.getASSETSLAYADD()));

        bindSelection(holder.cbSelect, assets);
        final CheckBox cbSelect = holder.cbSelect;
        convertView.setOnClickListener(v -> cbSelect.toggle());
        return convertView;
    }

    private void bindSelection(CheckBox checkBox, final Assets assets) {
        if (checkBox == null) {
            return;
        }
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(assets.getId() != null && selectedIds.contains(assets.getId()));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Long id = assets.getId();
            if (id == null) {
                buttonView.setChecked(false);
                return;
            }
            if (isChecked) {
                if (singleSelect) {
                    selectedIds.clear();
                    selectedIds.add(id);
                    notifyDataSetChanged();
                    notifySelectionChanged();
                    return;
                }
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

    static class ViewHolder {
        TextView tvAssetsName;
        TextView tvBarcode;
        TextView tvDept;
        TextView tvLocation;
        CheckBox cbSelect;
    }
}
