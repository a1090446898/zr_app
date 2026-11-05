package com.gsoft.inventory;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.gsoft.inventory.adapter.PhotoGridAdapter;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.service.photo.PhotoMigrationTask;
import com.gsoft.inventory.service.photo.RequestAssetPhotosTask;
import com.gsoft.inventory.service.photo.RequestDeleteAssetPhotoTask;
import com.gsoft.inventory.utils.domain.Assets;
import com.gsoft.inventory.utils.AsyncManager;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.FileToolUtils;
import com.gsoft.inventory.utils.IGridItemDeleteListener;
import com.gsoft.inventory.utils.MenuGridView;
import com.gsoft.inventory.utils.PhotoStorageHelper;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import id.zelory.compressor.Compressor;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.gsoft.inventory.utils.SysDefine.REQUEST_CODE_CAPTURE_CAMEIA;


@RuntimePermissions
public class PhotoActivity extends BaseCompatActivity {

    @BindView(R.id.photoGridView)
    MenuGridView photoGridView;
    PhotoGridAdapter photoAdapter = null;
    ArrayList<String> photoArray = null;

    @BindView(R.id.topbar)
    QMUITopBar topBar;

    @BindView(R.id.btnTakePhoto)
    QMUIRoundButton btnTakePhoto;

    Assets assets = null;
    File tmpPhotoFile;
    Uri uri;

    private List<AssetPhoto> photoRecords;
    private static final int REQUEST_WRITE_PERMISSION = 1;

    /** 当前拍照目录：压缩图统一输出到该目录，避免写入cache/images后被系统清理或同名覆盖 */
    private String currentPhotoDirPath;
    /** 当前拍照文件路径：相机取消/失败时清理空文件，避免干扰照片序号 */
    private String currentPhotoFilePath;

    /** 拍照结果处理中：禁用拍照按钮，避免重复拍照覆盖当前文件 */
    private boolean isProcessingPhoto;

    /** 进入页面时的照片加载任务数：本地记录与服务器照片都完成后关闭加载提示 */
    private int pendingPhotoLoadTasks = 0;

    /** 相机返回后文件可能尚未完全落盘，解码失败时的最大重试次数 */
    private static final int MAX_PHOTO_DECODE_RETRY = 3;
    /** 照片解码重试间隔（毫秒） */
    private static final long PHOTO_DECODE_RETRY_DELAY = 300L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        ButterKnife.bind(this);
        initializeView();
    }

    /**
     * 打开系统相机拍摄资产照片
     * <p>
     * 照片和压缩图统一保存在 zcAppTempPic/账套/DOCID 目录，
     * 文件名保持“条码_三位序号.jpg”规则不变。
     */
    private void openCameraForCapture() {
        // 迁移期间禁止拍照，避免与迁移任务同时操作照片记录
        if (PhotoMigrationTask.isRunning()) {
            showShortText("照片迁移进行中，请稍候再拍照");
            return;
        }
        if (isProcessingPhoto) {
            showShortText("照片处理中，请稍候");
            return;
        }
        if (assets == null) {
            showShortText("资产信息未找到，请退出重试！");
            return;
        }
        // 高版本Android不允许在外置目录随意创建，只能在私有目录storage/emulated/0/Android/data/com.microhabit/files进行创建
        String path =  mContext.getExternalFilesDir(null) + "/zcAppTempPic";

        File pathFile = new File(path);
        if (!pathFile.exists()) {
            boolean created = pathFile.mkdirs();
            if (!created) {
                showShortText("创建zcAppTempPic目录失败");
                return;
            }
        }
        String acctsuiteId = assets.getACCTSUITEID();
        // 在path目录下检查有没有acctsuiteId命名的文件夹
        String subDirPath = path + File.separator + acctsuiteId;
        File subDir = new File(subDirPath);
        if (!subDir.exists()) {
            boolean created = subDir.mkdirs();
            if (!created) {
                showShortText("创建" + acctsuiteId + "目录失败");
                return; // 中止后续操作
            }
        }
        // 在subDirPath路径下检查有没有docId命名的文件夹，如果没有则创建一个
        String docId = assets.getDOCID();
        String docDirPath = subDirPath + File.separator + docId;
        File docDir = new File(docDirPath);
        if (!docDir.exists()) {
            boolean created = docDir.mkdirs();
            if (!created) {
                showShortText("创建" + docId + "目录失败");
                return; // 中止后续操作
            }
        }
        // 获取subDirPath目录下有多少文件
        String barcodeId = assets.getBARCODEID();
        // 图片命名规范为docId+编号，编号规则为三位数，左边不足补零
        // 序号取目录内同条码照片的最大序号+1：删除照片后编号不回退，
        // 避免新照片复用旧路径后，列表命中旧缩略图缓存显示成别的照片
        int nextIndex = findMaxPhotoIndex(docDir, barcodeId) + 1;
        File targetPhotoFile;
        do {
            String imgName = String.format(Locale.CHINESE, "%1$s_%2$03d.jpg", barcodeId, nextIndex);
            targetPhotoFile = new File(docDir, imgName);
            nextIndex++;
        } while (targetPhotoFile.exists());
        String filePathStr = targetPhotoFile.getAbsolutePath();

        tmpPhotoFile = new File(filePathStr);
        currentPhotoDirPath = docDirPath;
        currentPhotoFilePath = filePathStr;
        PhotoActivityPermissionsDispatcher.takePhotoWithPermissionCheck(PhotoActivity.this);

    }

    @Override
    public void initializeView() {
        topBar.setTitle("查看资产照片");
        long assetsID = getIntent().getLongExtra(SysDefine.IntentExtra_AssetsID, 0);
        if (assetsID == 0) {
            showShortText("资产信息错误，请退出重试！");
            return;
        }
        assets = Assets.findById(Assets.class, assetsID);
        if (assets == null) {
            showShortText("资产信息未找到，请退出重试！");
            return;
        }
        topBar.setTitle(assets.getASSETSNAME() + "照片");
        topBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // 拍照按钮已从标题栏移到页面底部悬浮，点击逻辑见 openCameraForCapture()
        btnTakePhoto.setOnClickListener(v -> openCameraForCapture());

        photoArray = new ArrayList<String>();
        photoRecords = new ArrayList<>();
        // 先挂空适配器，本地照片改到后台加载，扫描文件不会阻塞主线程导致加载提示无法显示
        photoAdapter = new PhotoGridAdapter(mContext, photoArray);
        photoGridView.setAdapter(photoAdapter);
        photoAdapter.setImageDeleteListener(new IGridItemDeleteListener() {
            @Override
            public void onGridItemDelete(String imgPath) {
                if (StringUtils.isNullOrEmpty(imgPath)) return;

                new QMUIDialog.MessageDialogBuilder(mContext)
                        .setTitle("系统提示")
                        .setMessage("删除后不可恢复，确定要删除此照片吗")
                        .addAction("取消", (dialog, index) -> dialog.dismiss())
                        .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                            if (imgPath.toLowerCase(Locale.ROOT).startsWith("http")) {
                                // 服务器照片：通过服务层任务删除
                                AsyncManager.getInstance().submitWithCallback(
                                        new RequestDeleteAssetPhotoTask(imgPath),
                                        result -> mainHandler.post(() -> {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }
                                            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                                                showShortText("照片已删除");
                                                photoArray.remove(imgPath);
                                                photoAdapter.notifyDataSetChanged();
                                            } else {
                                                showShortText("删除失败，请联系技术人员");
                                            }
                                        }));
                            } else {
                                // 本地照片：记录与文件同步删除（统一存储后两者路径一致）
                                AssetPhoto targetPhoto = null;
                                String imgName = subFileName(imgPath);
                                for (AssetPhoto item : photoRecords) {
                                    if (item.getPath() != null && imgPath.equals(item.getPath())) {
                                        targetPhoto = item;
                                        break;
                                    }
                                    if (item.getPath() != null && imgName.equals(subFileName(item.getPath()))) {
                                        targetPhoto = item;
                                        break;
                                    }
                                }
                                if (targetPhoto != null) {
                                    PhotoStorageHelper.deleteLocalPhoto(mContext, assets, targetPhoto);
                                    photoRecords.remove(targetPhoto);
                                } else {
                                    FileToolUtils.deleteFile(imgPath);
                                }
                                photoArray.remove(imgPath);
                                resetPhotoArray();
                                photoAdapter.notifyDataSetChanged();
                            }
                            dialog.dismiss();
                        })
                        .show();
            }
        });
        photoGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intentView = new Intent(mContext, PhotoViewActivity.class);
                intentView.putStringArrayListExtra(SysDefine.IntentExtra_PHOTOIMAGEARRAY, photoArray);
                // 传出点击位置，放大页直接定位到该张，避免总显示第一张
                intentView.putExtra(SysDefine.IntentExtra_PHOTOIMAGEINDEX, i);
                startActivity(intentView);
            }
        });

        // 长按图片：弹框显示本地绝对路径；网络图片显示URL
        photoGridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= photoArray.size()) {
                return false;
            }
            String imagePath = photoArray.get(position);
            if (StringUtils.isNullOrEmpty(imagePath)) {
                return false;
            }
            boolean isNetImage = imagePath.toLowerCase(Locale.ROOT).startsWith("http");
            new QMUIDialog.MessageDialogBuilder(mContext)
                    .setTitle(isNetImage ? "图片URL" : "图片路径")
                    .setMessage(imagePath)
                    .addAction("关闭", (dialog, index) -> dialog.dismiss())
                    .show();
            return true;
        });

        // 查询服务器已上传照片（服务层任务，避免Activity内嵌Task）
        // 进入页面显示加载提示：本地照片扫描与服务器照片获取都完成后自动关闭
        pendingPhotoLoadTasks = 2;
        showProgressDialog("", "正在加载照片");
        loadLocalPhotosAsync();
        requestAssetPhotos();
    }

    /**
     * 后台加载本地照片记录
     * <p>
     * 文件存在性校验、历史缓存迁移、待认领归位都涉及磁盘和数据库操作，
     * 放到线程池执行，避免照片较多时阻塞主线程导致加载提示无法显示。
     */
    private void loadLocalPhotosAsync() {
        AsyncManager.getInstance().execute(() -> {
            List<AssetPhoto> loadedRecords = new ArrayList<>();
            List<String> loadedPaths = new ArrayList<>();
            // 只加载当前资产（DOC+CODE）的照片记录：历史记录DOC为空时补全，避免跨账套同条码串图
            List<AssetPhoto> localPhotos = AssetPhoto.find(AssetPhoto.class, "CODE=?", assets.getBARCODEID());
            if (localPhotos != null) {
                // 最新的照片排最前：ID越大越新，避免新拍照片总在列表末尾
                Collections.sort(localPhotos, (first, second) -> {
                    long firstId = first.getId() == null ? 0L : first.getId();
                    long secondId = second.getId() == null ? 0L : second.getId();
                    return Long.compare(secondId, firstId);
                });
                for (AssetPhoto photo : localPhotos) {
                    String photoDoc = photo.getDoc();
                    if (StringUtils.isNullOrEmpty(photoDoc)) {
                        // 兼容旧版本未写DOC的记录
                        photo.setDoc(assets.getDOCID());
                        photo.save();
                    } else if (!photoDoc.equals(assets.getDOCID())) {
                        continue;
                    }
                    // 待认领照片：当前资产（DOC+CODE）能匹配上时自动认领，挪入正确账套目录
                    if (PhotoStorageHelper.isPendingClaim(photo)) {
                        PhotoStorageHelper.claimPendingPhoto(mContext, assets, photo);
                    }
                    // 统一存储：历史缓存目录图片迁移到外部照片目录
                    String validPath = PhotoStorageHelper.ensurePhotoFile(mContext, assets, photo);
                    if (StringUtils.isNullOrEmpty(validPath)) {
                        // 文件被用户删除/剪切时保留记录（照片上传页仍可见），本页显示占位图便于核对
                        if (!StringUtils.isNullOrEmpty(photo.getPath()) && !loadedPaths.contains(photo.getPath())) {
                            loadedRecords.add(photo);
                            loadedPaths.add(photo.getPath());
                        }
                        continue;
                    }
                    if (!loadedPaths.contains(validPath)) {
                        loadedRecords.add(photo);
                        loadedPaths.add(validPath);
                    }
                    // 同一路径的多条记录只显示第一条，其余记录保留不删，避免路径异常时误删照片数据
                }
            }
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                photoRecords.clear();
                photoRecords.addAll(loadedRecords);
                // 服务器照片可能先返回：本地照片插到最前，但不能清空列表把网络照片冲掉
                photoArray.removeAll(loadedPaths);
                photoArray.addAll(0, loadedPaths);
                photoAdapter.notifyDataSetChanged();
                markPhotoLoadTaskFinished();
            });
        });
    }

    /**
     * 查询服务器已上传照片（服务层任务，避免Activity内嵌Task）
     */
    private void requestAssetPhotos() {
        AsyncManager.getInstance().submitWithCallback(
                new RequestAssetPhotosTask(assets.getDOCID(), assets.getBARCODEID()),
                result -> mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!result.isSuccess()) {
                        String errorMessage = result.getError() == null ? "" : result.getError().getMessage();
                        showShortText("获取服务器照片出错" + errorMessage);
                        markPhotoLoadTaskFinished();
                        return;
                    }
                    List<String> results = result.getData();
                    if (results == null || results.isEmpty()) {
                        showShortText("暂无已上传的照片");
                        markPhotoLoadTaskFinished();
                        return;
                    }
                    refreshNetPhotos(results);
                    markPhotoLoadTaskFinished();
                }));
    }

    /**
     * 标记一个照片加载任务结束：本地记录与服务器照片都完成后关闭加载提示
     */
    private void markPhotoLoadTaskFinished() {
        if (pendingPhotoLoadTasks > 0) {
            pendingPhotoLoadTasks--;
        }
        if (pendingPhotoLoadTasks <= 0) {
            hideProgressDialog();
        }
    }

    private String subFileName(String filePath) {
        // 找到最后一个'/'的位置
        int lastSlashIndex = filePath.lastIndexOf('/');

        // 如果找到了'/', 从它的下一个位置开始截取字符串直到结束，得到文件名
        if (lastSlashIndex != -1) {
            return filePath.substring(lastSlashIndex + 1);
        } else {
            return filePath;
        }
    }

    /**
     * 扫描目录中“条码_三位序号.jpg”文件，返回最大序号
     * <p>
     * 序号取最大值而不是文件数量：删除中间照片后编号不回退复用，
     * 保证每次拍照生成的文件路径唯一，缩略图缓存不会串图。
     *
     * @param directory 照片目录
     * @param barcodeId 资产条码
     * @return 当前最大序号；没有匹配文件返回0
     */
    private int findMaxPhotoIndex(File directory, String barcodeId) {
        if (directory == null || !directory.exists() || !directory.isDirectory()
                || StringUtils.isNullOrEmpty(barcodeId)) {
            return 0;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        int maxIndex = 0;
        String prefix = barcodeId + "_";
        for (File file : files) {
            if (file == null || !file.isFile()) {
                continue;
            }
            String name = file.getName();
            if (!name.startsWith(prefix) || !name.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
                continue;
            }
            String indexText = name.substring(prefix.length(), name.length() - 4);
            if (indexText.length() == 0) {
                continue;
            }
            try {
                maxIndex = Math.max(maxIndex, Integer.parseInt(indexText));
            } catch (NumberFormatException ignored) {
                // 认领/迁移时追加 _1 等后缀的非纯数字命名不参与序号计算
            }
        }
        return maxIndex;
    }

    @NeedsPermission({Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE})
    public void takePhoto() {
        if (tmpPhotoFile == null) {
            showShortText("拍照文件创建失败，请重试");
            return;
        }
        // 部分相机要求 EXTRA_OUTPUT 对应的文件已存在，提前创建，避免只写入少量字节
        if (!tmpPhotoFile.exists()) {
            File parentDir = tmpPhotoFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                showShortText("创建照片目录失败，请重试");
                return;
            }
            try {
                if (!tmpPhotoFile.createNewFile()) {
                    showShortText("创建照片文件失败，请重试");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                showShortText("创建照片文件失败，请重试");
                return;
            }
        }
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // 高版本不允许直接共享，需要配置并且提供共享权限
            Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", tmpPhotoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            // 必须同时授予读写权限，否则部分相机无法写入照片文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // 部分华为机型需要显式声明 CATEGORY_DEFAULT 才能正常启动相机
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(intent, REQUEST_CODE_CAPTURE_CAMEIA);
        } catch (Exception e) {
            e.printStackTrace();
            // 相机没有启动成功时清理空文件，避免影响下一次拍照序号
            if (tmpPhotoFile.exists() && tmpPhotoFile.length() == 0) {
                tmpPhotoFile.delete();
            }
            showShortText("打开相机失败，请重试");
        }
    }


    public void refreshNetPhotos(List<String> urls) {
        if (urls == null || urls.size() == 0) return;
        for (String url : urls) {
            String fullUrl = StringUtils.rightTrim(SysDefine.SERVERHOST, '/') + url;
            // 服务器照片去重，避免重复显示同一张图
            if (!photoArray.contains(fullUrl)) {
                photoArray.add(fullUrl);
            }
        }
        photoAdapter.notifyDataSetChanged();
    }

    /**
     * 照片拍摄完成回调
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        返回数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE_CAPTURE_CAMEIA) {
            return;
        }
        // 相机取消：删除已创建但未写入内容的文件，避免空文件占用目录并影响照片序号
        if (resultCode != RESULT_OK) {
            if (!StringUtils.isNullOrEmpty(currentPhotoFilePath)) {
                File canceledFile = new File(currentPhotoFilePath);
                if (canceledFile.exists()) {
                    canceledFile.delete();
                }
            }
            currentPhotoFilePath = null;
            finishPhotoProcessing();
            return;
        }
        File photoFile = StringUtils.isNullOrEmpty(currentPhotoFilePath) ? tmpPhotoFile : new File(currentPhotoFilePath);
        if (photoFile == null || !photoFile.exists() || photoFile.length() == 0) {
            showShortText("拍照失败，请重试");
            if (photoFile != null && photoFile.exists()) {
                photoFile.delete();
            }
            currentPhotoFilePath = null;
            finishPhotoProcessing();
            return;
        }
        isProcessingPhoto = true;
        if (btnTakePhoto != null) {
            btnTakePhoto.setEnabled(false);
        }
        // 相机返回时文件可能还没完全落盘，延迟后校验并重试，避免读取到不完整图片
        mainHandler.postDelayed(() -> checkPhotoAndCompress(photoFile, 0), PHOTO_DECODE_RETRY_DELAY);
    }

    /**
     * 校验照片并压缩保存
     * <p>
     * 相机返回后文件可能仍在写入，解码失败时延迟重试；
     * 超过重试次数仍不可解码才提示重拍，避免把损坏文件交给压缩库导致闪退。
     *
     * @param photoFile  相机拍摄的文件
     * @param retryCount 当前重试次数
     */
    private void checkPhotoAndCompress(File photoFile, int retryCount) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (isValidImageFile(photoFile)) {
            compressAndSavePhoto(photoFile);
            return;
        }
        if (retryCount < MAX_PHOTO_DECODE_RETRY) {
            mainHandler.postDelayed(() -> checkPhotoAndCompress(photoFile, retryCount + 1), PHOTO_DECODE_RETRY_DELAY);
            return;
        }
        showShortText("拍照文件无效，请重新拍摄");
        if (photoFile.exists()) {
            photoFile.delete();
        }
        currentPhotoFilePath = null;
        finishPhotoProcessing();
    }

    /**
     * 压缩并保存照片
     * <p>
     * 压缩结果先写入临时文件，成功后再替换原图。
     * 不能直接输出到原文件：压缩库输出与输入同名时会截断原图，
     * 导致内部解码失败并抛出Bitmap空指针。
     * <p>
     * 拍照结果只把照片记录置为“待上传”状态，本页不联网上传；
     * 联网上传统一由首页“照片上传”页处理，上传代码保留不删。
     *
     * @param photoFile 已校验可解码的原图
     */
    private void compressAndSavePhoto(File photoFile) {
        File parentFile = photoFile.getParentFile();
        if (parentFile == null) {
            showShortText("照片目录异常，请重新拍摄");
            if (photoFile.exists()) {
                photoFile.delete();
            }
            currentPhotoFilePath = null;
            finishPhotoProcessing();
            return;
        }
        String destinationDir = StringUtils.isNullOrEmpty(currentPhotoDirPath)
                ? parentFile.getAbsolutePath() : currentPhotoDirPath;
        File tempCompressedFile = new File(destinationDir, "tmp_" + photoFile.getName());
        String finalPhotoPath = photoFile.getAbsolutePath();
        try {
            File compressedFile = new Compressor(this)
                    .setMaxWidth(800)
                    .setMaxHeight(600)
                    .setQuality(80)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setDestinationDirectoryPath(destinationDir)
                    .compressToFile(photoFile, tempCompressedFile.getName());
            if (compressedFile != null && compressedFile.exists() && compressedFile.length() > 0) {
                File targetFile = new File(destinationDir, photoFile.getName());
                // 删除原图失败时保留原图并放弃压缩结果，避免照片丢失
                if (!targetFile.exists() || targetFile.delete()) {
                    if (compressedFile.renameTo(targetFile)) {
                        finalPhotoPath = targetFile.getAbsolutePath();
                    } else {
                        finalPhotoPath = compressedFile.getAbsolutePath();
                    }
                } else {
                    compressedFile.delete();
                }
            }
        } catch (Exception e) {
            // Compressor内部解码失败会抛NPE，必须捕获Exception兜底
            e.printStackTrace();
            Log.e("PhotoActivity", "照片压缩失败：" + e.getMessage());
            if (tempCompressedFile.exists()) {
                tempCompressedFile.delete();
            }
            if (!isValidImageFile(photoFile)) {
                if (photoFile.exists()) {
                    photoFile.delete();
                }
                currentPhotoFilePath = null;
                showShortText("照片处理失败，请重新拍摄");
                finishPhotoProcessing();
                return;
            }
            // 压缩失败仍保留原图，保证目录文件与照片记录一致
        }

        // 只插入最前（最新拍摄优先），不能再追加一次，否则同一路径会在列表中出现两张
        photoArray.add(0, finalPhotoPath);
        photoAdapter.notifyDataSetChanged();

        try {
            File finalPhotoFile = new File(finalPhotoPath);
            AssetPhoto assetPhoto = new AssetPhoto();
            assetPhoto.setDoc(assets.getDOCID());
            // 拍照时记录账套编号，后续迁移/归位不再依赖资产表反查
            assetPhoto.setAcctsuiteId(assets.getACCTSUITEID());
            assetPhoto.setUser(application.getZcqcyCode());
            assetPhoto.setCode(assets.getBARCODEID());
            assetPhoto.setName(finalPhotoFile.getName());
            assetPhoto.setPath(finalPhotoPath);
            // 拍照只保存为待上传状态，等待用户在首页“照片上传”统一上传
            assetPhoto.setStatus(PhotoStorageHelper.STATUS_WAIT_UPLOAD);
            assetPhoto.save();
            photoRecords.add(assetPhoto);
            // 联网自动上传已停用：拍照后不再直接上传，改为到首页“照片上传”统一上传。
            // 上传实现保留（AssetsUtils.uploadAssetPhotos/uploadAssetPhoto），需要恢复自动上传时取消下一行注释即可。
            // AssetsUtils.uploadAssetPhotos(handler, mContext);
            // 弹框提醒用户：照片需要到首页“照片上传”上传
            showUploadPhotoTipDialog();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PhotoActivity", "保存照片记录失败：" + e.getMessage());
        } finally {
            currentPhotoFilePath = null;
            finishPhotoProcessing();
        }
    }

    /**
     * 结束本次拍照处理，恢复拍照按钮
     */
    private void finishPhotoProcessing() {
        isProcessingPhoto = false;
        if (btnTakePhoto != null) {
            btnTakePhoto.setEnabled(true);
        }
    }

    private void resetPhotoArray() {
        while (photoArray.contains("")) {
            photoArray.remove("");
        }
    }

    /**
     * 校验文件是否能完整解码为图片
     * <p>
     * 只读取图片头（inJustDecodeBounds）无法发现像素数据不完整的文件，
     * 这里用采样方式完整解码一次，确保压缩库不会再拿到无法解码的文件。
     *
     * @param imageFile 待校验文件
     * @return true 表示可以正常解码为图片
     */
    private boolean isValidImageFile(File imageFile) {
        if (imageFile == null || !imageFile.exists() || imageFile.length() == 0) {
            return false;
        }
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            // 采样解码，既能验证像素数据完整性，又避免大图占用过多内存
            options.inSampleSize = 4;
            bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            return bitmap != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    /**
     * 拍照保存成功后的上传提示框
     * <p>
     * 拍照后不再自动联网上传，只把照片置为“待上传”，
     * 本次登录只在第一次拍照成功时弹框提醒，后续拍照不再重复提示；
     * 重新登录后由InventoryApplication.setLoginAccount重置标志，再提示一次。
     */
    private void showUploadPhotoTipDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        // 本次登录已提示过则直接返回，避免每张照片都弹框
        if (application == null || !application.needShowPhotoUploadTip()) {
            return;
        }
        // 先标记再弹框：连续拍照时不会叠加弹出多个提示
        application.markPhotoUploadTipShown();
        new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle("照片上传提示")
                .setMessage("照片已保存为“待上传”状态，请到首页「照片上传」上传照片。")
                .addAction("知道了", (dialog, index) -> dialog.dismiss())
                .show();
    }
}
