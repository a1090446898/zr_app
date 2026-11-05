package com.gsoft.inventory.utils;

import android.content.Context;

import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.utils.domain.Assets;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 资产照片本地存储工具
 * <p>
 * 统一约定：照片原图和压缩图都保存在
 * {@code 外部私有目录/zcAppTempPic/账套编号/DOCID/} 下，
 * {@link AssetPhoto#getPath()} 始终指向该目录中的文件。
 * 这样可避免历史版本把压缩图写入 {@code cache/images} 后被系统清理，
 * 或被不同资产的同名压缩文件覆盖。
 *
 * @author 10904
 */
public final class PhotoStorageHelper {

    /** 资产照片根目录（外部私有目录下） */
    private static final String PHOTO_ROOT_DIR = "zcAppTempPic";
    /** 历史版本 Compressor 默认输出目录（cache/images） */
    private static final String LEGACY_CACHE_DIR = "images";
    /** 待认领照片状态：迁移时账套无法确定，单独存放，不参与一键上传 */
    public static final String STATUS_PENDING_CLAIM = "待认领";
    /** 正常待上传状态 */
    public static final String STATUS_WAIT_UPLOAD = "待上传";
    /** 已上传状态：服务端确认后写入，本地记录和照片一律保留 */
    public static final String STATUS_UPLOADED = "已上传";
    /** 待认领目录名（zcAppTempPic 下）：账套未知的照片统一存放，避免错归到当前账套 */
    public static final String PENDING_CLAIM_DIR = "_待认领";
    /** 同名冲突时追加序号的最大尝试次数 */
    private static final int MAX_NAME_SUFFIX = 1000;

    private PhotoStorageHelper() {
    }

    /**
     * 获取资产照片目录（不创建）
     *
     * @param context     上下文
     * @param acctsuiteId 账套编号
     * @param docId       资产单据号
     * @return 照片目录
     */
    public static File getPhotoDir(Context context, String acctsuiteId, String docId) {
        File root = new File(context.getExternalFilesDir(null), PHOTO_ROOT_DIR);
        return new File(new File(root, safeDirName(acctsuiteId)), safeDirName(docId));
    }

    /**
     * 获取资产照片目录，不存在时创建
     *
     * @return 照片目录；创建失败返回null
     */
    public static File getOrCreatePhotoDir(Context context, String acctsuiteId, String docId) {
        File dir = getPhotoDir(context, acctsuiteId, docId);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return dir;
    }

    /**
     * 获取资产照片根目录（外部私有目录/zcAppTempPic，不创建）
     *
     * @return 根目录；外部存储不可用（如SD卡卸载）时返回null
     */
    public static File getPhotoRootDir(Context context) {
        File externalDir = context.getExternalFilesDir(null);
        if (externalDir == null) {
            return null;
        }
        return new File(externalDir, PHOTO_ROOT_DIR);
    }

    /**
     * 获取待认领目录（不创建）：外部私有目录/zcAppTempPic/_待认领/单据号
     */
    public static File getPendingClaimDir(Context context, String docId) {
        File root = getPhotoRootDir(context);
        if (root == null) {
            return null;
        }
        return new File(new File(root, PENDING_CLAIM_DIR), safeDirName(docId));
    }

    /**
     * 获取待认领目录，不存在时创建
     *
     * @return 目录；创建失败返回null
     */
    public static File getOrCreatePendingClaimDir(Context context, String docId) {
        File dir = getPendingClaimDir(context, docId);
        if (dir == null) {
            return null;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return dir;
    }

    /**
     * 是否待认领照片（迁移时账套无法确定，等待登录对应账套后归位）
     */
    public static boolean isPendingClaim(AssetPhoto photo) {
        return photo != null && STATUS_PENDING_CLAIM.equals(photo.getStatus());
    }

    /**
     * 判断照片本地文件是否缺失：路径为空或文件不存在都视为缺失
     * <p>
     * 记录本身保留，由照片上传页用红字提示，用户可通过“清空记录”主动移除。
     *
     * @param photo 照片记录
     * @return true 表示本地文件缺失
     */
    public static boolean isPhotoFileMissing(AssetPhoto photo) {
        return photo == null || StringUtils.isNullOrEmpty(photo.getPath())
                || !new File(photo.getPath()).exists();
    }

    /**
     * 按单据号/条码匹配资产，返回第一条记录
     * <p>
     * 资产表只保留当前账套数据，匹配成功即说明照片属于当前登录账套。
     *
     * @return 匹配到的资产；匹配不到返回null
     */
    public static Assets findMatchedAssets(String docId, String code) {
        if (StringUtils.isNullOrEmpty(docId)) {
            return null;
        }
        List<Assets> assetsList = StringUtils.isNullOrEmpty(code)
                ? Assets.find(Assets.class, "DOCID=?", docId)
                : Assets.find(Assets.class, "DOCID=? AND BARCODEID=?", docId, code);
        if (assetsList == null || assetsList.isEmpty()) {
            return null;
        }
        return assetsList.get(0);
    }

    /**
     * 认领待认领照片：把文件挪入资产所属账套目录并恢复为待上传
     * <p>
     * 只有当前账套资产表能匹配到单据号/条码时才认领，避免再次归错账套。
     *
     * @return true 表示认领成功
     */
    public static boolean claimPendingPhoto(Context context, Assets assets, AssetPhoto photo) {
        if (context == null || assets == null || photo == null || !isPendingClaim(photo)) {
            return false;
        }
        String acctsuiteId = assets.getACCTSUITEID();
        String path = photo.getPath();
        if (StringUtils.isNullOrEmpty(acctsuiteId) || StringUtils.isNullOrEmpty(path)) {
            return false;
        }
        File sourceFile = new File(path);
        if (!sourceFile.exists() || sourceFile.length() == 0) {
            return false;
        }
        File targetDir = getOrCreatePhotoDir(context, acctsuiteId, assets.getDOCID());
        if (targetDir == null) {
            return false;
        }
        File targetFile = resolveAvailableTargetFile(targetDir, sourceFile);
        if (targetFile == null) {
            return false;
        }
        if (!targetFile.exists() || targetFile.length() != sourceFile.length()) {
            if (!copyToTempThenRename(sourceFile, targetFile)) {
                return false;
            }
        }
        try {
            photo.setPath(targetFile.getAbsolutePath());
            photo.setName(targetFile.getName());
            photo.setAcctsuiteId(acctsuiteId);
            photo.setStatus(STATUS_WAIT_UPLOAD);
            photo.save();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (sourceFile.exists()) {
            sourceFile.delete();
        }
        return true;
    }

    /**
     * 待认领照片自动归位：当前资产表能匹配到的记录，挪入正确账套目录
     * <p>
     * 在打开照片上传页等时机后台执行；匹配不到（照片仍不属于当前账套）的保持待认领。
     *
     * @return 本次归位的照片数量
     */
    public static int reclaimPendingPhotos(Context context) {
        List<AssetPhoto> pendingList = AssetPhoto.find(AssetPhoto.class, "STATUS=?", STATUS_PENDING_CLAIM);
        if (pendingList == null || pendingList.isEmpty()) {
            return 0;
        }
        int claimed = 0;
        for (AssetPhoto photo : pendingList) {
            Assets assets = findMatchedAssets(photo.getDoc(), photo.getCode());
            if (assets == null) {
                continue;
            }
            if (claimPendingPhoto(context, assets, photo)) {
                claimed++;
            }
        }
        return claimed;
    }

    /**
     * 冲突消解：同名且大小一致直接复用，大小不同追加序号，绝不覆盖已有照片
     */
    public static File resolveAvailableTargetFile(File targetDir, File sourceFile) {
        if (targetDir == null || sourceFile == null) {
            return null;
        }
        String fileName = sourceFile.getName();
        File targetFile = new File(targetDir, fileName);
        if (!targetFile.exists()) {
            return targetFile;
        }
        if (targetFile.length() == sourceFile.length()) {
            return targetFile;
        }
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }
        for (int i = 1; i < MAX_NAME_SUFFIX; i++) {
            File candidate = new File(targetDir, baseName + "_" + i + extension);
            if (!candidate.exists()) {
                return candidate;
            }
            if (candidate.length() == sourceFile.length()) {
                return candidate;
            }
        }
        return new File(targetDir, baseName + "_" + System.currentTimeMillis() + extension);
    }

    /**
     * 复制源文件到临时文件，长度校验通过后再重命名
     * <p>
     * 保证目标文件要么完整、要么不存在，不会出现半截jpg；
     * 任何失败都不动源文件，重跑时可复用已完整的目标文件。
     */
    public static boolean copyToTempThenRename(File sourceFile, File targetFile) {
        if (sourceFile == null || targetFile == null) {
            return false;
        }
        File parentDir = targetFile.getParentFile();
        if (parentDir == null) {
            return false;
        }
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            return false;
        }
        File tempFile = new File(parentDir, targetFile.getName() + ".tmp");
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            outputStream = null;
            inputStream.close();
            inputStream = null;
            // 长度校验：防止把不完整文件当成迁移成功
            if (tempFile.length() != sourceFile.length()) {
                return false;
            }
            if (targetFile.exists() && !targetFile.delete()) {
                return false;
            }
            return tempFile.renameTo(targetFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
            // 失败时清理临时文件；重命名成功后临时文件已不存在
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 统一照片文件存储位置
     * <p>
     * 1. 记录指向历史缓存目录(cache/images)且文件存在：复制到资产照片目录并更新记录，删除旧缓存文件；
     * 2. 记录文件不存在但资产照片目录存在同名文件：修正记录路径；
     * 3. 两边都不存在：返回null，由调用方删除失效记录。
     *
     * @param context 上下文
     * @param assets  照片所属资产，用于计算账套/DOCID目录；为空时只做存在性校验
     * @param photo   照片记录
     * @return 可用的本地文件路径；文件不存在返回null
     */
    public static String ensurePhotoFile(Context context, Assets assets, AssetPhoto photo) {
        if (photo == null || StringUtils.isNullOrEmpty(photo.getPath())) {
            return null;
        }
        File currentFile = new File(photo.getPath());
        boolean legacyCacheFile = isLegacyCachePath(context, photo.getPath());
        // 正常外部目录内的文件直接返回，不创建多余目录
        if (currentFile.exists() && !legacyCacheFile) {
            return currentFile.getAbsolutePath();
        }
        File targetDir = null;
        if (assets != null) {
            targetDir = legacyCacheFile
                    ? getOrCreatePhotoDir(context, assets.getACCTSUITEID(), assets.getDOCID())
                    : getPhotoDir(context, assets.getACCTSUITEID(), assets.getDOCID());
        }
        File targetFile = targetDir == null ? null : new File(targetDir, currentFile.getName());
        // 历史版本压缩图在cache目录：迁移到资产照片目录，保证剪切外部目录能带走全部本地照片
        if (currentFile.exists() && legacyCacheFile && targetFile != null) {
            try {
                copyFile(currentFile, targetFile);
                updatePhotoPath(photo, targetFile, assets.getACCTSUITEID());
                currentFile.delete();
                return targetFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                // 迁移失败时保留缓存文件，避免照片直接不可用
                return currentFile.getAbsolutePath();
            }
        }
        // 记录指向的文件被剪切/清理：尝试使用资产目录下的同名文件修复记录
        if (targetFile != null && targetFile.exists()) {
            updatePhotoPath(photo, targetFile, assets.getACCTSUITEID());
            return targetFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * 删除本地照片：记录、记录指向的文件、资产照片目录下同名文件、历史缓存目录下同名文件一并删除
     *
     * @param context 上下文
     * @param assets  照片所属资产；为空时按文件名递归清理历史目录残留（照片清除场景）
     * @param photo   照片记录
     */
    public static void deleteLocalPhoto(Context context, Assets assets, AssetPhoto photo) {
        if (photo == null) {
            return;
        }
        String path = photo.getPath();
        if (!StringUtils.isNullOrEmpty(path)) {
            File file = new File(path);
            String fileName = file.getName();
            if (file.exists()) {
                file.delete();
            }
            if (assets != null) {
                File sameNameFile = new File(getPhotoDir(context, assets.getACCTSUITEID(), assets.getDOCID()), fileName);
                if (sameNameFile.exists()) {
                    sameNameFile.delete();
                }
            } else {
                // 未指定资产：按文件名递归清理照片根目录下的历史残留
                deletePhotoFileByName(context, fileName);
            }
            File legacyFile = new File(new File(context.getCacheDir(), LEGACY_CACHE_DIR), fileName);
            if (legacyFile.exists()) {
                legacyFile.delete();
            }
        }
        photo.delete();
    }

    /**
     * 清理文件已不存在的照片记录，并对同一路径的重复记录去重
     * <p>
     * 用于照片列表/待上传列表显示前校验：文件已被剪切或系统清理后，
     * 不再保留无法显示、无法上传的脏记录。
     *
     * @return 清理的记录数
     */
    public static int removeInvalidPhotos() {
        List<AssetPhoto> photoList = AssetPhoto.listAll(AssetPhoto.class, "ID ASC");
        Set<String> pathSet = new HashSet<>();
        int removed = 0;
        for (AssetPhoto photo : photoList) {
            if (StringUtils.isNullOrEmpty(photo.getPath()) || !new File(photo.getPath()).exists()) {
                photo.delete();
                removed++;
                continue;
            }
            // 历史cache同名覆盖可能产生多条记录指向同一文件，只保留第一条
            if (!pathSet.add(photo.getPath())) {
                photo.delete();
                removed++;
            }
        }
        return removed;
    }

    /**
     * 按文件名递归删除照片根目录下的文件（兼容历史账套/DOCID目录中的同名残留）
     */
    public static void deletePhotoFileByName(Context context, String fileName) {
        if (StringUtils.isNullOrEmpty(fileName)) {
            return;
        }
        File root = new File(context.getExternalFilesDir(null), PHOTO_ROOT_DIR);
        deleteFileByName(root, fileName);
    }

    private static void updatePhotoPath(AssetPhoto photo, File targetFile, String acctsuiteId) {
        photo.setPath(targetFile.getAbsolutePath());
        photo.setName(targetFile.getName());
        if (!StringUtils.isNullOrEmpty(acctsuiteId)) {
            photo.setAcctsuiteId(acctsuiteId);
        }
        photo.save();
    }

    private static boolean isLegacyCachePath(Context context, String path) {
        String cachePath = new File(context.getCacheDir(), LEGACY_CACHE_DIR).getAbsolutePath();
        return new File(path).getAbsolutePath().startsWith(cachePath + File.separator);
    }

    private static String safeDirName(String name) {
        return StringUtils.isNullOrEmpty(name) ? "unknown" : name;
    }

    private static void copyFile(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("创建照片目录失败：" + parent.getAbsolutePath());
        }
        try (InputStream inputStream = new FileInputStream(source);
             OutputStream outputStream = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private static void deleteFileByName(File dir, String fileName) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteFileByName(file, fileName);
            } else if (fileName.equals(file.getName())) {
                file.delete();
            }
        }
    }
}