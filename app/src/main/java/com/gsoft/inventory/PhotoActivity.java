package com.gsoft.inventory;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.adapter.PhotoGridAdapter;
import com.gsoft.inventory.entities.AssetPhoto;
import com.gsoft.inventory.utils.Assets;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.IGridItemDeleteListener;
import com.gsoft.inventory.utils.MenuGridView;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysConfig;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
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

    Assets assets = null;
    File tmpPhotoFile;
    Uri uri;

    private List<AssetPhoto> photoRecords;
    private static final int REQUEST_WRITE_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        ButterKnife.bind(this);
        initializeView();
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
        topBar.addRightImageButton(android.R.drawable.ic_menu_camera, R.id.topbar_right_change_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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
                        int imgCount = countFilesInDirectory(docDir);
                        String barcodeId = assets.getBARCODEID();
                        // 图片命名规范为docId+编号，编号规则为三位数，左边不足补零
                        String imgName = String.format(Locale.CHINESE, "%1$s_%2$03d.jpg", barcodeId, imgCount + 1);
//                        String fileName = String.format(Locale.CHINESE, "%1$s_%2$d.jpg", assets.getBARCODEID(), System.currentTimeMillis());
                        String filePathStr = docDirPath + File.separator + imgName;

                        tmpPhotoFile = new File(filePathStr);
                        PhotoActivityPermissionsDispatcher.takePhotoWithPermissionCheck(PhotoActivity.this);

                    }
                });

        photoArray = new ArrayList<String>();

        photoRecords = AssetPhoto.find(AssetPhoto.class, "CODE=?", assets.getBARCODEID());
        for (AssetPhoto photo : photoRecords) {
            photoArray.add(photo.getPath());
        }
        //FileSearchHelper.searchFiles(SysConfig.PHOTO_TEMP_FILE_PATH, "jpg", assets.getBARCODEID(), false, photoArray);//assets.getSERIALCODE()
        /*for (String file : photoList) {
            photoArray.add(file);
        }*/
        //resetPhotoArray();
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
                                new RequestDeleteAssetPhotoTask(PhotoActivity.this).execute(imgPath);
                            } else {

                                try {
                                    String imgName = subFileName(imgPath);
                                    // 通过filePath查询对应数据
                                    for (AssetPhoto item : photoRecords) {
                                        String itemName = subFileName(item.getPath());
                                        if (imgName.equals(itemName)) {
                                            AssetPhoto param = new AssetPhoto();
                                            Long id = item.getId();
                                            param.setId(id);
                                            AssetPhoto.delete(param);
                                        }
                                    }
                                    // 删除文件
                                    File delFile = new File(imgPath);
                                    if (delFile.exists()) {
                                        delFile.delete();
                                    }
                                    photoArray.remove(imgPath);
                                    resetPhotoArray();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    photoAdapter.notifyDataSetChanged();
                                }
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
                startActivity(intentView);
            }
        });

        new RequestAssetPhotosTask(PhotoActivity.this).execute();
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

    private int countFilesInDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return 0;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (file.isFile()) {
                count++;
            }
        }
        return count;
    }


    @NeedsPermission({Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE})
    public void takePhoto() {
        try{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // 高版本不允许直接共享，需要配置并且提供共享权限
            Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", tmpPhotoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_CAPTURE_CAMEIA);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void refreshNetPhotos(List<String> urls) {
        if (urls == null || urls.size() == 0) return;
        for (String url : urls) {
            photoArray.add(StringUtils.rightTrim(SysDefine.SERVERHOST, '/') + url);
        }
        photoAdapter.notifyDataSetChanged();
    }

    /**
     * 照片拍摄完成回调
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA && resultCode == RESULT_OK) {
            String photoPath = "";
            photoPath = tmpPhotoFile.getAbsolutePath();
            photoArray.add(photoPath);
            photoAdapter.notifyDataSetChanged();

            try {
                File compressedFile = new Compressor(this)
                        .setMaxWidth(800)
                        .setMaxHeight(600)
                        .setQuality(80)
                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                        .compressToFile(tmpPhotoFile);

                AssetPhoto assetPhoto = new AssetPhoto();
                assetPhoto.setDoc(assets.getDOCID());
                assetPhoto.setUser(application.getZcqcyCode());
                assetPhoto.setCode(assets.getBARCODEID());
                assetPhoto.setName(compressedFile.getName());
                assetPhoto.setPath(compressedFile.getAbsolutePath());
                assetPhoto.setStatus("待上传");
                assetPhoto.save();

                photoRecords.add(assetPhoto);
                // 保存完后上传，暂时去掉
//                ThreadManager.get().start(() -> {
//                    AssetsUtils.uploadAssetPhoto(assets.getDOCID(), assets.getBARCODEID(), application.getZcqcyCode(), compressedFile, assetPhoto.getId());
//                });

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("报错:", e.getMessage());
            }
        }
    }

    private void resetPhotoArray() {
        while (photoArray.contains("")) {
            photoArray.remove("");
        }
        /*for (int i = 0; i < photoArray.size() % 3; i++) {
            photoArray.add("");
        }*/
    }

    static class RequestAssetPhotosTask extends AsyncTask<Void, Void, String> {

        private WeakReference<PhotoActivity> reference;
        private final Gson gson = new Gson();

        public RequestAssetPhotosTask(PhotoActivity activity) {
            this.reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String resultString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_PHOTO_LIST(), reference.get().assets.getDOCID(), reference.get().assets.getBARCODEID()));
            if (StringUtils.isNullOrEmpty(resultString) || resultString.contains("ERROR")) {
                return "ERROR:暂无已上传的照片！";
            }
            return resultString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (StringUtils.isNullOrEmpty(s)) reference.get().showShortText("暂无已上传的照片");
            if (s.startsWith("ERROR")) reference.get().showShortText("暂无已上传的照片");

            try {
                List<String> results = gson.fromJson(s, new TypeToken<List<String>>() {}.getType());
                if (results == null || results.isEmpty()) {
                    reference.get().showShortText("暂无已上传的照片");
                    return;
                }
                reference.get().refreshNetPhotos(results);
            } catch (Exception e) {
                e.printStackTrace();
                reference.get().showShortText("获取服务器照片出错" + e.getMessage());
            }
        }
    }

    static class RequestDeleteAssetPhotoTask extends AsyncTask<String, Void, String> {

        private WeakReference<PhotoActivity> reference;
        private final Gson gson = new Gson();
        private String deletingImage;

        public RequestDeleteAssetPhotoTask(PhotoActivity activity) {
            this.reference = new WeakReference<>(activity);
        }

        @Override
        protected String doInBackground(String... voids) {
            deletingImage = voids[0];

            String imageId = Uri.parse(deletingImage).getQueryParameter("IMGID");
            String resultString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_ASSET_PHOTO_DELETE(), imageId));
            if (StringUtils.isNullOrEmpty(resultString) || resultString.contains("ERROR")) {
                return "ERROR:提交删除请求失败！";
            }
            return resultString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (StringUtils.isNullOrEmpty(s)) reference.get().showShortText("删除失败，请联系技术人员");
            if ("true".equals(s)) {
                reference.get().showShortText("照片已删除");
                if (!StringUtils.isNullOrEmpty(deletingImage)) {
                    reference.get().photoArray.remove(deletingImage);
                    reference.get().resetPhotoArray();
                    reference.get().photoAdapter.notifyDataSetChanged();
                }
            } else {
                reference.get().showShortText("删除失败，请联系技术人员");
            }
        }
    }
}
