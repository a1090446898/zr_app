package com.gsoft.inventory.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.print_batch.BatchPrintConfig;
import com.qmuiteam.qmui.layout.QMUIButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 补打条码列表卡片：资产名称/条码/管理部门(编号转名称)/使用人/存放地点 + 打印按钮
 * <p>
 * 已打印的条目(ISPRINTBARCODE=02，本次会话中打印后保留显示)按钮置灰为"已打印"不可再点；
 * 页面重新加载后已打印数据不再出现在列表中
 * <p>
 * 勾选模式：用于批量补打，选中态按资产Id记录（列表分页/复用，不能用position），
 * 已打印条目不参与勾选。
 *
 * @author 10904
 */
public class ReprintBarcodeAdapter extends BaseAdapter {

    /** 打印按钮点击回调 */
    public interface OnPrintClickListener {
        void onPrintClick(Assets assets);
    }

    /** 勾选变化回调（批量补打模式） */
    public interface OnSelectionChangedListener {
        /** 选中条数变化（用于更新底部统计与全选态） */
        void onSelectionChanged(int selectedCount);

        /** 勾选数达到上限（用于提示分批打印） */
        void onSelectLimitReached(int maxCount);
    }

    private final LayoutInflater mInflater;
    private List<Assets> dataList = new ArrayList<>();
    private OnPrintClickListener onPrintClickListener;
    /** 账套编号：管理部门字段存单位编号(DWBH)时，展示按账套转单位名称(DWMC)用 */
    private String accountZtbh = "";

    /** 勾选模式开关（批量补打） */
    private boolean selectMode;
    /** 已勾选资产Id（稳定key，跨分页/复用不丢） */
    private final Set<Long> selectedIds = new HashSet<>();
    /** 单次最多勾选数 */
    private int maxSelectCount = BatchPrintConfig.MAX_BATCH_SIZE;
    private OnSelectionChangedListener selectionListener;

    public ReprintBarcodeAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
    }

    public void setData(List<Assets> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    /** 注册打印按钮点击回调 */
    public void setOnPrintClickListener(OnPrintClickListener listener) {
        this.onPrintClickListener = listener;
    }

    /** 设置账套编号：管理部门展示时编号转中文名称用（与盘点扫描卡片同一口径） */
    public void setAccountZtbh(String ztbh) {
        this.accountZtbh = ztbh == null ? "" : ztbh;
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
            convertView = mInflater.inflate(R.layout.item_reprint_barcode, parent, false);
            holder = new ViewHolder();
            holder.tvAssetsName = convertView.findViewById(R.id.tvAssetsName);
            holder.tvBarcode = convertView.findViewById(R.id.tvBarcode);
            holder.tvManageDept = convertView.findViewById(R.id.tvManageDept);
            holder.tvUser = convertView.findViewById(R.id.tvUser);
            holder.tvLocation = convertView.findViewById(R.id.tvLocation);
            holder.btnPrint = convertView.findViewById(R.id.btnPrint);
            holder.cbSelect = convertView.findViewById(R.id.cbSelect);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Assets assets = dataList.get(position);
        holder.tvAssetsName.setText(StringUtils.isNullOrEmpty(assets.getASSETSNAME()) ? "未命名资产" : assets.getASSETSNAME());
        holder.tvBarcode.setText("条码：" + safeText(assets.getBARCODEID()));
        holder.tvManageDept.setText("管理部门：" + deptDisplayName(assets.getGROUNDMANAGEDEPT()));
        holder.tvUser.setText("使用人员：" + userName(assets));
        holder.tvLocation.setText("存放地点：" + locationDisplayName(assets.getASSETSLAYADD()));
        // 已打印条目按钮置灰不可点（本次会话内保留显示，重新加载后不再出现）
        boolean printed = Const.PRINTED.equals(assets.getISPRINTBARCODE());
        holder.btnPrint.setEnabled(!printed);
        holder.btnPrint.setAlpha(printed ? 0.4f : 1f);
        holder.btnPrint.setText(printed ? "已打印" : "打印");
        // 批量模式下隐藏单条打印按钮，统一走底部批量打印
        holder.btnPrint.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        holder.btnPrint.setOnClickListener(v -> {
            if (onPrintClickListener != null && !Const.PRINTED.equals(assets.getISPRINTBARCODE())) {
                onPrintClickListener.onPrintClick(assets);
            }
        });

        bindSelection(holder.cbSelect, assets, printed);

        final CheckBox cbSelect = holder.cbSelect;
        if (selectMode && !printed) {
            // 批量模式下点整张卡片即切换勾选，扩大点击热区
            convertView.setOnClickListener(v -> cbSelect.toggle());
        } else {
            convertView.setOnClickListener(null);
            convertView.setClickable(false);
        }
        return convertView;
    }

    // ==================== 批量补打勾选 ====================

    /** 进入/退出勾选模式：退出时清空已选，避免脏状态带到下次 */
    public void setSelectMode(boolean selectMode) {
        if (this.selectMode == selectMode) {
            return;
        }
        this.selectMode = selectMode;
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public boolean isSelectMode() {
        return selectMode;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void setMaxSelectCount(int maxSelectCount) {
        this.maxSelectCount = maxSelectCount;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    /** 当前已加载列表中可选数据是否全部勾选（用于全选框状态） */
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

    /** 全选当前已加载的未打印数据：超出上限只选前N条并提示 */
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

    /** 已勾选资产（按列表顺序，仅返回当前列表内仍可打印的数据） */
    public List<Assets> getSelectedAssets() {
        List<Assets> result = new ArrayList<>();
        for (Assets assets : dataList) {
            if (selectable(assets) && selectedIds.contains(assets.getId())) {
                result.add(assets);
            }
        }
        return result;
    }

    /** 只有未打印且有Id的资产可参与批量勾选 */
    private boolean selectable(Assets assets) {
        return assets != null && assets.getId() != null && !Const.PRINTED.equals(assets.getISPRINTBARCODE());
    }

    /** 绑定单项勾选态：先解监听再setChecked，避免列表复用触发出错误的选中事件 */
    private void bindSelection(CheckBox checkBox, final Assets assets, final boolean printed) {
        if (checkBox == null) {
            return;
        }
        checkBox.setOnCheckedChangeListener(null);
        if (!selectMode || printed) {
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

    /** 使用人显示值：ASSETSUSER优先，为空时用CUSTODIAN（与打印标题口径一致） */
    private static String userName(Assets assets) {
        if (!StringUtils.isNullOrEmpty(assets.getASSETSUSER())) {
            return assets.getASSETSUSER();
        }
        if (!StringUtils.isNullOrEmpty(assets.getCUSTODIAN())) {
            return assets.getCUSTODIAN();
        }
        return "-";
    }

    /**
     * 管理部门显示值：优先按部门编号(BMBH)转部门名称(BMMC)，再兼容历史单位编号(DWBH)按账套转单位名称(DWMC)，
     * 空值显示"未分配部门"（与盘点统计/扫描卡片同一口径）
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

    /** 存放地点显示值：空值显示"未设置存放地点"（与盘点统计/扫描卡片同一口径） */
    private static String locationDisplayName(String raw) {
        String value = StringUtils.trim(raw);
        return StringUtils.isNullOrEmpty(value) ? RfidTagAdapter.LOCATION_NONE_NAME : value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    static class ViewHolder {
        TextView tvAssetsName;
        TextView tvBarcode;
        TextView tvManageDept;
        TextView tvUser;
        TextView tvLocation;
        QMUIButton btnPrint;
        CheckBox cbSelect;
    }
}