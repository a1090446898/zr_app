package com.gsoft.inventory.service.photo;

import android.content.Context;

import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.utils.PhotoStorageHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.domain.Assets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 缓存照片批量迁移任务
 * <p>
 * 把记录中指向历史缓存目录（cache/images）的资产照片，按原有统一存储规则
 * 迁移到“外部私有目录/zcAppTempPic/账套编号/DOCID/”下。
 * 迁移遵循以下原则，保证照片不丢失、不错归：
 * 1. 先复制成完整临时文件并重命名成功，再更新数据库记录，最后才删除缓存源文件；
 * 2. 照片记录里已有账套编号时直接使用，没有时按单据号和条码查资产表；
 * 3. 账套无法确定（资产已删除、重新导入或属于其他账套）时，照片放入“_待认领”目录，
 *    不归到当前登录账套，避免一台设备切换多个账套后照片错归；
 * 4. 源文件缺失、复制失败时保留记录原样，只统计，不删除任何记录；
 * 5. 目标目录已有同名文件时按大小判断能否复用，大小不同追加序号，绝不覆盖。
 * <p>
 * 待认领照片保留原单据号和条码，登录对应账套并同步资产后可自动归位。
 * 进度写入静态字段由页面轮询刷新，避免Activity重建后回调丢失导致遮罩无法关闭。
 *
 * @author 10904
 */
public class PhotoMigrationTask implements Callable<PhotoMigrationTask.Result> {

    /** 历史压缩图路径特征 */
    private static final String LEGACY_PATH_PATTERN = "%/cache/images/%";
    /** 资产表查不到对应账套时的缓存哨兵值 */
    private static final String UNKNOWN_ACCTSUITE = "__UNKNOWN__";
    /** 待认领清单文件名（存放在 _待认领 根目录，用户插线到电脑可直接查看） */
    private static final String PENDING_MANIFEST_FILE = "待认领清单.txt";
    /** 进度刷新最小间隔（毫秒），避免几万条记录频繁刷新 */
    private static final long PROGRESS_INTERVAL = 200L;

    /** 全局运行标记：同一时间只允许一个迁移任务 */
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile int currentProgress = 0;
    private static volatile String currentMessage = "";
    private static volatile Result lastResult;
    private static volatile String errorMessage = "";

    private final Context context;
    /** 账套查询缓存：DOCID|BARCODEID -> 账套编号，避免几万条记录逐条查资产表 */
    private final Map<String, String> acctsuiteCache = new HashMap<>();
    /** 待认领清单写入器：首次遇到待认领照片时创建，任务结束时关闭 */
    private BufferedWriter pendingManifestWriter;

    public PhotoMigrationTask(Context context) {
        this.context = context.getApplicationContext();
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static int getProgress() {
        return currentProgress;
    }

    public static String getProgressMessage() {
        return currentMessage;
    }

    public static String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 取出并清空上一次迁移结果，保证结果弹窗只展示一次
     */
    public static Result consumeLastResult() {
        synchronized (PhotoMigrationTask.class) {
            Result result = lastResult;
            lastResult = null;
            return result;
        }
    }

    private static void publishProgress(int progress, String message) {
        currentProgress = progress;
        currentMessage = message;
    }

    @Override
    public Result call() {
        if (!RUNNING.compareAndSet(false, true)) {
            throw new IllegalStateException("照片迁移正在进行中");
        }
        Result result = new Result();
        errorMessage = "";
        publishProgress(0, "正在统计缓存照片...");
        try {
            List<AssetPhoto> photoList = AssetPhoto.find(AssetPhoto.class, "PATH LIKE ?", LEGACY_PATH_PATTERN);
            if (photoList == null || photoList.isEmpty()) {
                publishProgress(100, "没有需要迁移的缓存照片");
                lastResult = result;
                return result;
            }
            result.total = photoList.size();
            long lastPublishTime = 0L;
            for (int i = 0; i < photoList.size(); i++) {
                migrateOne(photoList.get(i), result);
                int progress = (int) ((i + 1) * 100.0 / photoList.size());
                long now = System.currentTimeMillis();
                // 按时间节流上报进度，避免频繁刷新UI
                if (now - lastPublishTime >= PROGRESS_INTERVAL || i == photoList.size() - 1) {
                    lastPublishTime = now;
                    publishProgress(progress, String.format(Locale.CHINESE,
                            "正在迁移 %1$d/%2$d，成功%3$d，待认领%4$d，缺失%5$d，失败%6$d",
                            i + 1, photoList.size(), result.success, result.unassigned, result.missing, result.failed));
                }
            }
            publishProgress(100, String.format(Locale.CHINESE,
                    "迁移完成，成功%1$d张，待认领%2$d张", result.success, result.unassigned));
            lastResult = result;
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return result;
        } finally {
            closePendingManifest();
            RUNNING.set(false);
        }
    }

    /**
     * 迁移单条记录：只有记录更新成功后才删除缓存源文件
     */
    private void migrateOne(AssetPhoto photo, Result result) {
        String path = photo == null ? null : photo.getPath();
        if (StringUtils.isNullOrEmpty(path)) {
            result.missing++;
            return;
        }
        File sourceFile = new File(path);
        if (!sourceFile.exists()) {
            // 源文件已被系统清理：保留记录原样，等待人工核对或后续重跑
            result.missing++;
            return;
        }
        if (sourceFile.length() == 0) {
            // 0字节是历史拍照失败留下的空文件，不迁移也不删除，避免污染照片目录
            result.empty++;
            return;
        }
        String acctsuiteId = resolveAcctsuiteId(photo);
        if (StringUtils.isNullOrEmpty(acctsuiteId)) {
            // 账套无法确定：放入待认领目录，绝不归到当前登录账套
            migrateToPendingClaim(photo, sourceFile, result);
            return;
        }
        File targetDir = PhotoStorageHelper.getOrCreatePhotoDir(context, acctsuiteId, photo.getDoc());
        if (targetDir == null) {
            // 存储不可用或目录创建失败：保留源文件和记录
            result.failed++;
            return;
        }
        if (!movePhoto(sourceFile, targetDir, photo, acctsuiteId, null)) {
            result.failed++;
            return;
        }
        result.success++;
    }

    /**
     * 迁移到待认领目录：文件可查、记录保留、不参与一键上传
     */
    private void migrateToPendingClaim(AssetPhoto photo, File sourceFile, Result result) {
        File targetDir = PhotoStorageHelper.getOrCreatePendingClaimDir(context, photo.getDoc());
        if (targetDir == null) {
            result.failed++;
            return;
        }
        String originalPath = sourceFile.getAbsolutePath();
        if (!movePhoto(sourceFile, targetDir, photo, "", PhotoStorageHelper.STATUS_PENDING_CLAIM)) {
            result.failed++;
            return;
        }
        appendPendingManifest(originalPath, photo);
        result.unassigned++;
    }

    /**
     * 把源文件复制到目标目录并更新照片记录，成功后删除缓存源文件
     *
     * @param acctsuiteId 目标账套编号；待认领时传空串
     * @param status      迁移后的记录状态；为空表示保持原状态
     */
    private boolean movePhoto(File sourceFile, File targetDir, AssetPhoto photo,
                              String acctsuiteId, String status) {
        File targetFile = PhotoStorageHelper.resolveAvailableTargetFile(targetDir, sourceFile);
        if (targetFile == null) {
            return false;
        }
        // 目标文件已存在且大小一致：视为上次中断留下的完整副本，直接复用
        if (!targetFile.exists() || targetFile.length() != sourceFile.length()) {
            if (!PhotoStorageHelper.copyToTempThenRename(sourceFile, targetFile)) {
                return false;
            }
        }
        try {
            photo.setPath(targetFile.getAbsolutePath());
            photo.setName(targetFile.getName());
            photo.setAcctsuiteId(acctsuiteId);
            if (!StringUtils.isNullOrEmpty(status)) {
                photo.setStatus(status);
            }
            photo.save();
        } catch (Exception e) {
            // 数据库更新失败：源文件保持不动，目标文件留待下次重跑复用
            e.printStackTrace();
            return false;
        }
        // 记录更新成功之后才允许删除缓存源文件
        if (sourceFile.exists()) {
            sourceFile.delete();
        }
        return true;
    }

    /**
     * 解析记录所属账套：优先使用拍照时写入的账套，其次按单据号和条码查资产表，
     * 都确定不了时返回null，由调用方转入待认领目录
     */
    private String resolveAcctsuiteId(AssetPhoto photo) {
        String recordAcctsuiteId = photo.getAcctsuiteId();
        if (!StringUtils.isNullOrEmpty(recordAcctsuiteId)) {
            return recordAcctsuiteId;
        }
        String docId = photo.getDoc();
        String code = photo.getCode();
        if (StringUtils.isNullOrEmpty(docId)) {
            return null;
        }
        String cacheKey = docId + "|" + safeText(code);
        String cached = acctsuiteCache.get(cacheKey);
        if (cached != null) {
            return UNKNOWN_ACCTSUITE.equals(cached) ? null : cached;
        }
        Assets assets = PhotoStorageHelper.findMatchedAssets(docId, code);
        String acctsuiteId = assets == null ? null : assets.getACCTSUITEID();
        if (StringUtils.isNullOrEmpty(acctsuiteId)) {
            acctsuiteCache.put(cacheKey, UNKNOWN_ACCTSUITE);
            return null;
        }
        acctsuiteCache.put(cacheKey, acctsuiteId);
        return acctsuiteId;
    }

    /**
     * 追加一条待认领记录到清单文件，便于用户插线到电脑查看明细
     * <p>
     * 清单写入失败不影响迁移结果。
     */
    private void appendPendingManifest(String originalPath, AssetPhoto photo) {
        try {
            if (pendingManifestWriter == null) {
                File photoRoot = PhotoStorageHelper.getPhotoRootDir(context);
                if (photoRoot == null) {
                    return;
                }
                File pendingRoot = new File(photoRoot, PhotoStorageHelper.PENDING_CLAIM_DIR);
                if (!pendingRoot.exists() && !pendingRoot.mkdirs()) {
                    return;
                }
                File manifestFile = new File(pendingRoot, PENDING_MANIFEST_FILE);
                pendingManifestWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(manifestFile, true), "UTF-8"));
                String runTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date());
                pendingManifestWriter.write("==== " + runTime + " 照片待认领清单 ====");
                pendingManifestWriter.newLine();
                pendingManifestWriter.write("原缓存路径\tDOCID\t条码\t拍摄人\t现存放路径");
                pendingManifestWriter.newLine();
            }
            pendingManifestWriter.write(safeText(originalPath) + "\t" + safeText(photo.getDoc()) + "\t"
                    + safeText(photo.getCode()) + "\t" + safeText(photo.getUser()) + "\t"
                    + safeText(photo.getPath()));
            pendingManifestWriter.newLine();
            pendingManifestWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    /**
     * 关闭清单写入器，保证文件内容完整落盘
     */
    private void closePendingManifest() {
        if (pendingManifestWriter == null) {
            return;
        }
        try {
            pendingManifestWriter.close();
        } catch (Exception ignored) {
        }
        pendingManifestWriter = null;
    }

    /**
     * 迁移统计结果
     */
    public static class Result {
        /** 待处理记录总数 */
        public int total;
        /** 迁移成功（账套已确定） */
        public int success;
        /** 账套无法确定，已迁移到“_待认领”目录，未计入迁移成功 */
        public int unassigned;
        /** 源文件缺失，记录已保留 */
        public int missing;
        /** 0字节空文件，跳过未动 */
        public int empty;
        /** 迁移失败，源文件已保留 */
        public int failed;
    }
}