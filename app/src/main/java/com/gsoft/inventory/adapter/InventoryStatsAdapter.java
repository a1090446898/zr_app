package com.gsoft.inventory.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.utils.AccountHelper;
import com.gsoft.inventory.utils.DictionaryHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;
import com.qmuiteam.qmui.layout.QMUIButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 盘点统计卡片列表（竖向卡片）：
 * 资产名称/条码编号/盘点情况(PDZT)/盘点状态(ZT)/使用部门/管理部门/存放地点/补打状态(ISPRINTBARCODE)，
 * 卡片底部"补打条码"按钮把资产标记为补打（01待补打），标记后进入补打条码页统一打印
 */
public class InventoryStatsAdapter extends BaseAdapter {

    /** 补打按钮点击回调 */
    public interface OnMarkReprintClickListener {
        void onMarkReprintClick(Assets assets);
    }

    private final LayoutInflater mInflater;
    /** 盘点情况字典：01待盘点 02盘实 03盘盈 04待核实 */
    private final String[] pdztFields;
    /** 盘点状态字典：01未盘 02已盘 */
    private final String[] ztFields;
    /** 补打状态字典：01待补打 02已打印 */
    private final String[] reprintStateFields;
    private List<Assets> dataList = new ArrayList<>();
    private OnMarkReprintClickListener onMarkReprintClickListener;
    /** 账套编号：管理部门/使用部门字段存编号时转名称展示用 */
    private String accountZtbh = "";

    public InventoryStatsAdapter(Context context) {
        this.mInflater = LayoutInflater.from(context);
        // 字典数组构造时一次性加载，避免getView重复读取资源
        this.pdztFields = context.getResources().getStringArray(R.array.pdztFields);
        this.ztFields = context.getResources().getStringArray(R.array.ztFields);
        this.reprintStateFields = context.getResources().getStringArray(R.array.reprintStateFields);
    }

    public void setData(List<Assets> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
    }

    /** 注册补打按钮点击回调 */
    public void setOnMarkReprintClickListener(OnMarkReprintClickListener listener) {
        this.onMarkReprintClickListener = listener;
    }

    /** 设置账套编号：部门编号转中文名称展示用（与盘点扫描卡片同一口径） */
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

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.inventory_stats_item, parent, false);
            holder = new ViewHolder();
            holder.tvASSETSNAME = convertView.findViewById(R.id.tvASSETSNAME);
            holder.tvBARCODEID = convertView.findViewById(R.id.tvBARCODEID);
            holder.tvPDZT = convertView.findViewById(R.id.tvPDZT);
            holder.tvZT = convertView.findViewById(R.id.tvZT);
            holder.tvASSETSDEPT = convertView.findViewById(R.id.tvASSETSDEPT);
            holder.tvGROUNDMANAGEDEPT = convertView.findViewById(R.id.tvGROUNDMANAGEDEPT);
            holder.tvASSETSLAYADD = convertView.findViewById(R.id.tvASSETSLAYADD);
            holder.tvISPRINTBARCODE = convertView.findViewById(R.id.tvISPRINTBARCODE);
            holder.btnMarkReprint = convertView.findViewById(R.id.btnMarkReprint);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Assets assets = dataList.get(position);
        holder.tvASSETSNAME.setText(StringUtils.isNullOrEmpty(assets.getASSETSNAME()) ? "未命名资产" : assets.getASSETSNAME());
        holder.tvBARCODEID.setText("条码编号：" + safeText(assets.getBARCODEID()));
        holder.tvPDZT.setText("盘点情况：" + pdztName(assets.getPDZT()));
        holder.tvZT.setText("盘点状态：" + ztName(assets.getZT()));
        holder.tvASSETSDEPT.setText("使用部门：" + deptDisplayName(assets.getASSETSDEPT()));
        holder.tvGROUNDMANAGEDEPT.setText("管理部门：" + manageDeptDisplayName(assets.getGROUNDMANAGEDEPT()));
        holder.tvASSETSLAYADD.setText("存放地点：" + locationDisplayName(assets.getASSETSLAYADD()));
        holder.tvISPRINTBARCODE.setText("补打标记：" + printStateName(assets.getISPRINTBARCODE()));
        // 已标记待补打(01)的条目按钮置灰防重复；已打印(02)与未标记(空)均可再次标记补打
        boolean marked = Const.NOT_PRINT.equals(assets.getISPRINTBARCODE());
        holder.btnMarkReprint.setEnabled(!marked);
        holder.btnMarkReprint.setAlpha(marked ? 0.4f : 1f);
        holder.btnMarkReprint.setText(marked ? "已标记" : "标记补打");
        holder.btnMarkReprint.setOnClickListener(v -> {
            if (onMarkReprintClickListener != null && !Const.NOT_PRINT.equals(assets.getISPRINTBARCODE())) {
                onMarkReprintClickListener.onMarkReprintClick(assets);
            }
        });
        return convertView;
    }

    /** 使用部门显示值：部门编号(BMBH)转部门名称(BMMC)，空值显示"未分配部门" */
    private String deptDisplayName(String raw) {
        String value = StringUtils.trim(raw);
        if (StringUtils.isNullOrEmpty(value)) {
            return RfidTagAdapter.DEPT_NONE_NAME;
        }
        String name = AccountHelper.getDepartName(value);
        if (!StringUtils.isNullOrEmpty(name)) {
            return name;
        }
        return value;
    }

    /** 管理部门显示值：优先按部门编号转部门名称，再兼容历史单位编号按账套转单位名称，空值显示"未分配部门" */
    private String manageDeptDisplayName(String raw) {
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

    /** 存放地点显示值：空值显示"未设置存放地点" */
    private static String locationDisplayName(String raw) {
        String value = StringUtils.trim(raw);
        return StringUtils.isNullOrEmpty(value) ? RfidTagAdapter.LOCATION_NONE_NAME : value;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }


    /** 补打状态字典：01待补打 02已打印，空值或非法值显示"未标记" */
    private String printStateName(String code) {
        String name = DictionaryHelper.getDictionaryName(code, reprintStateFields);
        return StringUtils.isNullOrEmpty(name) ? "未标记" : name;
    }

    /**
     * 盘点情况字典：01待盘点 02盘实 03盘盈 04待核实
     */
    private String pdztName(String pdzt) {
        return DictionaryHelper.getDictionaryName(pdzt, pdztFields);
    }

    /**
     * 盘点状态字典：02已盘，空值或非法值按字典第一项（未盘）显示
     */
    private String ztName(String zt) {
        String name = DictionaryHelper.getDictionaryName(zt, ztFields);
        return StringUtils.isNullOrEmpty(name) ? ztFields[0] : name;
    }

    static class ViewHolder {
        TextView tvASSETSNAME;
        TextView tvBARCODEID;
        TextView tvPDZT;
        TextView tvZT;
        TextView tvASSETSDEPT;
        TextView tvGROUNDMANAGEDEPT;
        TextView tvASSETSLAYADD;
        TextView tvISPRINTBARCODE;
        QMUIButton btnMarkReprint;
    }
}

