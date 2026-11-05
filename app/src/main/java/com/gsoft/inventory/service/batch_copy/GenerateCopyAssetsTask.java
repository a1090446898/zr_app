package com.gsoft.inventory.service.batch_copy;

import com.gsoft.inventory.const_data.Const;
import com.gsoft.inventory.data.BatchCopyOptions;
import com.gsoft.inventory.data.CopyResult;
import com.gsoft.inventory.utils.AssetsUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.domain.Assets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 批量生成复制数据：直接写入 Assets 表并打上本地标识，
 * 只标记待上传，不联网、不同步
 *
 * @author 10904
 */
public class GenerateCopyAssetsTask implements Callable<CopyResult> {

    private final List<Long> sourceIds;

    private final BatchCopyOptions options;

    private final String deviceNo;

    private final String writerCode;

    private final String writerDate;

    private final BatchCopyCallback<CopyResult> callback;

    public GenerateCopyAssetsTask(List<Long> sourceIds, BatchCopyOptions options,
                                  String deviceNo, String writerCode, String writerDate,
                                  BatchCopyCallback<CopyResult> callback) {
        this.sourceIds = sourceIds == null ? new ArrayList<>() : sourceIds;
        this.options = options == null ? new BatchCopyOptions() : options;
        this.deviceNo = deviceNo;
        this.writerCode = writerCode;
        this.writerDate = writerDate;
        this.callback = callback;
    }

    @Override
    public CopyResult call() throws Exception {
        long start = System.currentTimeMillis();
        CopyResult result = new CopyResult();
        // 写入前先确保 IS_COPY_DATA 列存在，否则整批 INSERT 会因缺列失败
        BatchCopyStore.ensureCopyColumn();
        int times = Math.max(1, Math.min(options.getCopyTimes(), Const.BATCH_COPY_TIMES_MAX));
        int total = sourceIds.size() * times;
        int current = 0;
        List<Assets> pending = new ArrayList<>(Const.BATCH_COPY_SAVE_BATCH_SIZE);

        for (Long sourceId : sourceIds) {
            Assets source = Assets.findById(Assets.class, sourceId);
            if (source == null) {
                for (int i = 0; i < times; i++) {
                    result.addFail("源资产不存在，Id=" + sourceId);
                }
                current += times;
                continue;
            }
            for (int i = 0; i < times; i++) {
                Assets copy = buildCopyAsset(source, options);
                pending.add(copy);
                current++;
                if (pending.size() >= Const.BATCH_COPY_SAVE_BATCH_SIZE) {
                    saveBatch(pending, result);
                    notifyProgress(current, total);
                }
            }
        }
        saveBatch(pending, result);
        notifyProgress(current, total);
        result.setCostMs(System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 构造一条复制数据
     */
    private Assets buildCopyAsset(Assets source, BatchCopyOptions options) {
        Assets copy = AssetsUtils.cloneAssets(source);
        copy.setDOCID(AssetsUtils.generalDocId(deviceNo));
        String barCode = SysConfig.getNewAssetsBarcodeByDevice(deviceNo);
        copy.setBARCODEID(barCode);
        copy.setSERIALCODE(barCode);
        applyOptions(copy, options);
        boolean copyImg = options.isCopyImg();
        copy.setISUPIMG(copyImg ? Const.ISUPIMG_NEED : Const.ISUPIMG_NONE);
        copy.setCOPY_DOC_ID(copyImg ? source.getDOCID() : "");
        copy.setIS_COPY_DATA(Const.IS_COPY_DATA);
        copy.setPDZT(Const.PDZT_PROFIT);
        copy.setZT(Const.ZT_NOT_CHECKED);
        copy.setREMARKS(Const.REMARKS_PROFIT);
        copy.setASSETSWRITER(writerCode);
        copy.setASSETSWRITERDATE(writerDate);
        copy.setISPRINTBARCODE(Const.NOT_PRINT);
        return copy;
    }

    /**
     * 应用批量设置：只覆盖非空字段
     */
    private void applyOptions(Assets copy, BatchCopyOptions options) {
        if (!StringUtils.isNullOrEmpty(options.getAssetsLayAdd())) {
            copy.setASSETSLAYADD(options.getAssetsLayAdd());
        }
        if (!StringUtils.isNullOrEmpty(options.getAssetsDept())) {
            copy.setASSETSDEPT(options.getAssetsDept());
        }
        if (!StringUtils.isNullOrEmpty(options.getGroundManageDept())) {
            copy.setGROUNDMANAGEDEPT(options.getGroundManageDept());
        }
        if (!StringUtils.isNullOrEmpty(options.getCustodian())) {
            copy.setCUSTODIAN(options.getCustodian());
        }
        if (!StringUtils.isNullOrEmpty(options.getAssetsUser())) {
            copy.setASSETSUSER(options.getAssetsUser());
        }
    }

    /**
     * 分批落库：逐条保存并校验返回的 Id。
     * <p>
     * 不能用 {@code Assets.saveInTx}：SugarORM 1.5 的事务方法内部 catch 掉异常并回滚，
     * 业务层拿到的是“全部成功”，用户看到复制成功、列表里却一条都没有。
     */
    private void saveBatch(List<Assets> list, CopyResult result) {
        if (list.isEmpty()) {
            return;
        }
        for (Assets asset : list) {
            try {
                long id = asset.save();
                if (id > 0) {
                    result.addSuccess(1);
                } else {
                    result.addFail("保存失败：返回 Id=" + id);
                }
            } catch (Exception e) {
                result.addFail("保存失败：" + e.getMessage());
            }
        }
        list.clear();
    }

    private void notifyProgress(int current, int total) {
        if (callback != null) {
            callback.onProgress(current, total);
        }
    }
}