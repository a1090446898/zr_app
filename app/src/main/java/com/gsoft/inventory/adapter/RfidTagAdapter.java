package com.gsoft.inventory.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.StringUtils;
import com.qmuiteam.qmui.layout.QMUIButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RFID盘点标签卡片列表
 * <p>
 * 卡片内容：资产名称/条码编号/管理部门/存放地点/盘点状态/盘点情况 + 修改/一键变更按钮
 * 标题颜色规则：
 * 1. 存放地点与当前盘点地点不一致 → 红色(即使已盘也红)，同时显示红色提示文字与"一键变更"按钮
 * 2. 存放地点一致：已盘(ZT=02) → 绿色；未盘 → 黑色
 */
public class RfidTagAdapter extends RecyclerView.Adapter<RfidTagAdapter.RfidTagViewHolder> {

    /** 未设置存放地点时的占位名称（与统计页口径一致） */
    public static final String LOCATION_NONE_NAME = "未设置存放地点";
    /** 未分配部门时的占位名称（与统计页口径一致） */
    public static final String DEPT_NONE_NAME = "未分配部门";

    /** 已盘标题绿色 */
    private static final int COLOR_COUNTED = 0xFF2E9E5B;
    /** 未盘标题黑色 */
    private static final int COLOR_NOT_COUNTED = 0xFF333333;
    /** 地点不一致标题红色 */
    private static final int COLOR_MISMATCH = 0xFFE53935;

    /** 标签卡片数据 */
    public static class RfidTagItem {
        /** 标签EPC（十六进制） */
        public String epc;
        /** EPC解码出的资产条码 */
        public String barcode;
        /** 信号强度 */
        public String rssi;
        /** 查到的资产，null表示未入库 */
        public Assets assets;
    }

    /**
     * 卡片交互监听：卡片点击 / 修改按钮 / 一键变更按钮
     */
    public interface OnCardActionListener {
        /** 点击卡片（跳转资产详情） */
        void onItemClick(RfidTagItem item);
        /** 点击"修改"按钮（跳转资产变更页） */
        void onModifyClick(RfidTagItem item);
        /** 点击"一键变更"按钮（存放地点变更为当前盘点地点） */
        void onChangeLocationClick(RfidTagItem item);
    }

    /** 标签数据源（仅主线程操作） */
    private final List<RfidTagItem> dataList = new ArrayList<>();
    /** 卡片交互回调 */
    private OnCardActionListener cardActionListener;
    /** 当前盘点存放地点（展示名，"未设置存放地点"表示未设置） */
    private String currentLocation = "";
    /** 当前盘点部门匹配值集合（中文名称/部门编号/单位编号），空集合表示不过滤部门 */
    private final Set<String> currentDeptValues = new HashSet<>();

    /** 注册卡片交互监听 */
    public void setOnCardActionListener(OnCardActionListener cardActionListener) {
        this.cardActionListener = cardActionListener;
    }

    /** 设置当前盘点存放地点，用于判断标签地点是否一致 */
    public void setCurrentLocation(String location) {
        this.currentLocation = location == null ? "" : location;
    }

    /**
     * 设置当前盘点部门匹配值（中文名称+部门编号+单位编号），用于判断标签管理部门是否一致；
     * "未分配部门"占位名同时匹配数据库空值
     */
    public void setCurrentDept(String deptName, String deptCode, String unitCode) {
        currentDeptValues.clear();
        if (!StringUtils.isNullOrEmpty(deptName)) {
            currentDeptValues.add(deptName);
            if (DEPT_NONE_NAME.equals(deptName)) {
                currentDeptValues.add("");
            }
        }
        if (!StringUtils.isNullOrEmpty(deptCode)) {
            currentDeptValues.add(deptCode);
        }
        if (!StringUtils.isNullOrEmpty(unitCode)) {
            currentDeptValues.add(unitCode);
        }
    }

    /** 获取标签数据源（供页面统计数量/判断空视图） */
    public List<RfidTagItem> getDataList() {
        return dataList;
    }

    /** 追加一条标签并局部刷新：盘点命中资产时在列表末尾插入，避免整表重建闪烁 */
    public void addItem(RfidTagItem item) {
        dataList.add(item);
        notifyItemInserted(dataList.size() - 1);
    }

    /**
     * 初始化/重载卡片列表：进入页面时先把当前范围资产全部渲染出来（全量替换并整表刷新）
     */
    public void setDataList(List<RfidTagItem> list) {
        dataList.clear();
        if (list != null) {
            dataList.addAll(list);
        }
        notifyDataSetChanged();
    }

    /**
     * 判断资产是否已在卡片列表中（按资产ID匹配，用于扫描时去重）
     */
    public boolean containsAssets(Assets assets) {
        if (assets == null || assets.getId() == null) {
            return false;
        }
        for (RfidTagItem item : dataList) {
            if (item.assets != null && item.assets.getId() != null
                    && item.assets.getId().equals(assets.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 刷新指定资产的已展示卡片（盘点标记或一键变更后调用），
     * 标题颜色、提示文字、按钮可见性同步联动
     */
    public void refreshAssets(Assets assets) {
        if (assets == null) {
            return;
        }
        for (int i = 0; i < dataList.size(); i++) {
            RfidTagItem item = dataList.get(i);
            if (item.assets != null && item.assets.getId() != null
                    && item.assets.getId().equals(assets.getId())) {
                notifyItemChanged(i);
            }
        }
    }

    /** 全量刷新卡片（页面恢复时同步资产编辑后的最新状态） */
    public void refreshAll() {
        notifyDataSetChanged();
    }

    /**
     * 创建卡片ViewHolder：加载item_rfid_tag布局
     */
    @NonNull
    @Override
    public RfidTagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rfid_tag, parent, false);
        return new RfidTagViewHolder(view);
    }

    /**
     * 绑定卡片数据：
     * 1. 已入库资产展示各字段，按"地点是否一致 + 盘点状态"决定标题颜色与"一键变更"按钮可见性
     * 2. 未入库标签以红色提示未知标签
     */
    @Override
    public void onBindViewHolder(@NonNull RfidTagViewHolder holder, int position) {
        RfidTagItem item = dataList.get(position);
        if (item.assets != null) {
            bindAssetsCard(holder, item);
        } else {
            // 未入库标签：红色标题提示
            holder.tvName.setText("未知标签（未入库）");
            holder.tvName.setTextColor(COLOR_MISMATCH);
            holder.tvBarcode.setText("EPC：" + item.epc
                    + (StringUtils.isNullOrEmpty(item.barcode) ? "" : "（" + item.barcode + "）"));
            holder.tvManageDept.setText("该条码未在资产台账中登记");
            holder.tvLocation.setText("");
            holder.tvZt.setText("");
            holder.tvPdzt.setText("");
            holder.tvHint.setVisibility(View.GONE);
            holder.btnModify.setVisibility(View.GONE);
            holder.btnChange.setVisibility(View.GONE);
        }
        holder.tvRssi.setText(StringUtils.isNullOrEmpty(item.rssi) ? "" : item.rssi + "dBm");
        holder.itemView.setOnClickListener(v -> {
            if (cardActionListener != null) {
                cardActionListener.onItemClick(item);
            }
        });
    }

    /**
     * 绑定已入库资产卡片：字段展示 + 标题颜色 + 提示与"一键变更"按钮联动
     * <p>
     * 标题颜色规则：
     * 1. 存放地点不一致 → 红色(即使已盘也红) + 红色提示 + 一键变更按钮
     * 2. 地点一致但管理部门不一致 → 红色 + 红色提示（不属于当前盘点范围，无按钮）
     * 3. 范围一致：已盘(ZT=02)绿、未盘黑
     */
    private void bindAssetsCard(RfidTagViewHolder holder, RfidTagItem item) {
        Assets assets = item.assets;
        // 资产存放地点归一化：空值统一为"未设置存放地点"，与当前盘点地点比较
        String location = normalizeLocation(assets.getASSETSLAYADD());
        boolean locationMatch = location.equals(currentLocation);
        // 管理部门判断：未过滤部门(集合为空)恒为一致；否则资产管理部门原文(去空格)需命中匹配值集合
        String rawDept = StringUtils.trim(assets.getGROUNDMANAGEDEPT());
        boolean deptMatch = currentDeptValues.isEmpty() || currentDeptValues.contains(rawDept);

        holder.tvName.setText(assets.getASSETSNAME());
        if (!locationMatch) {
            // 地点不一致：红色标题(即使已盘也红) + 红色提示 + 一键变更按钮
            holder.tvName.setTextColor(COLOR_MISMATCH);
            holder.tvHint.setVisibility(View.VISIBLE);
            holder.tvHint.setText("该资产存放地点(" + location + ")与当前盘点地点(" + currentLocation + ")不一致，可一键变更到当前地点");
            holder.btnChange.setVisibility(View.VISIBLE);
        } else if (!deptMatch) {
            // 地点一致但管理部门不一致：红色标题 + 红色提示，不属于当前盘点范围，不显示一键变更
            holder.tvName.setTextColor(COLOR_MISMATCH);
            holder.tvHint.setVisibility(View.VISIBLE);
            holder.tvHint.setText("该资产管理部门(" + (StringUtils.isNullOrEmpty(rawDept) ? "未分配部门" : rawDept)
                    + ")与当前盘点部门不一致，无法纳入当前盘点");
            holder.btnChange.setVisibility(View.GONE);
        } else if ("02".equals(assets.getZT())) {
            // 范围一致且已盘：绿色标题
            holder.tvName.setTextColor(COLOR_COUNTED);
            holder.tvHint.setVisibility(View.GONE);
            holder.btnChange.setVisibility(View.GONE);
        } else {
            // 范围一致且未盘：黑色标题
            holder.tvName.setTextColor(COLOR_NOT_COUNTED);
            holder.tvHint.setVisibility(View.GONE);
            holder.btnChange.setVisibility(View.GONE);
        }
        holder.tvBarcode.setText("条码编号：" + assets.getBARCODEID());
        holder.tvManageDept.setText("管理部门：" + assets.getGROUNDMANAGEDEPT());
        holder.tvLocation.setText("存放地点：" + location);
        holder.tvZt.setText("盘点状态：" + ztName(assets.getZT()));
        holder.tvPdzt.setText("盘点情况：" + pdztName(assets.getPDZT()));
        // 按钮事件回调
        holder.btnModify.setOnClickListener(v -> {
            if (cardActionListener != null) {
                cardActionListener.onModifyClick(item);
            }
        });
        holder.btnChange.setOnClickListener(v -> {
            if (cardActionListener != null) {
                cardActionListener.onChangeLocationClick(item);
            }
        });
    }

    /**
     * 存放地点归一化：去除首尾空格，空值统一为"未设置存放地点"
     */
    private static String normalizeLocation(String location) {
        String value = StringUtils.trim(location);
        return StringUtils.isNullOrEmpty(value) ? LOCATION_NONE_NAME : value;
    }

    /**
     * 盘点状态名称：02已盘，其余(含空)未盘
     */
    private static String ztName(String zt) {
        return "02".equals(zt) ? "已盘" : "未盘";
    }

    /**
     * 盘点情况名称：01待盘点 02盘实 03盘盈 04待核实
     */
    private static String pdztName(String pdzt) {
        if ("01".equals(pdzt)) {
            return "待盘点";
        }
        if ("02".equals(pdzt)) {
            return "盘实";
        }
        if ("03".equals(pdzt)) {
            return "盘盈";
        }
        if ("04".equals(pdzt)) {
            return "待核实";
        }
        return "";
    }

    /** 列表项数量 */
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /** 标签卡片ViewHolder：名称/信号/条码/管理部门/存放地点/盘点状态/盘点情况/提示/按钮 */
    static class RfidTagViewHolder extends RecyclerView.ViewHolder {
        /** 资产名称（未入库时显示"未知标签"） */
        TextView tvName;
        /** 信号强度 */
        TextView tvRssi;
        /** 资产条码（未入库时显示EPC） */
        TextView tvBarcode;
        /** 管理部门 */
        TextView tvManageDept;
        /** 存放地点 */
        TextView tvLocation;
        /** 盘点状态 */
        TextView tvZt;
        /** 盘点情况 */
        TextView tvPdzt;
        /** 地点不一致红色提示 */
        TextView tvHint;
        /** 修改按钮 */
        QMUIButton btnModify;
        /** 一键变更按钮（仅地点不一致时可见） */
        QMUIButton btnChange;

        /** 绑定卡片内各控件 */
        RfidTagViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTagName);
            tvRssi = itemView.findViewById(R.id.tvTagRssi);
            tvBarcode = itemView.findViewById(R.id.tvTagBarcode);
            tvManageDept = itemView.findViewById(R.id.tvTagManageDept);
            tvLocation = itemView.findViewById(R.id.tvTagLocation);
            tvZt = itemView.findViewById(R.id.tvTagZt);
            tvPdzt = itemView.findViewById(R.id.tvTagPdzt);
            tvHint = itemView.findViewById(R.id.tvTagHint);
            btnModify = itemView.findViewById(R.id.btnTagModify);
            btnChange = itemView.findViewById(R.id.btnTagChange);
        }
    }
}