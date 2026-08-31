package com.gsoft.inventory.view.pdf_view;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.BaseCompatActivity;
import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author 10904
 */
public class PdfViewerActivity extends BaseCompatActivity {
    private PDFView pdfView;
    private String pdfUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);

        // 获取传递过来的PDF网址
        pdfUrl = getIntent().getStringExtra("PDF_URL");

        if (pdfUrl != null && !pdfUrl.isEmpty()) {
            loadPdfFromUrl(pdfUrl);
        } else {
            Toast.makeText(this, "没有找到PDF文件地址", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void initializeView() {

    }

    /**
     * 从网络加载PDF文件
     */
    private void loadPdfFromUrl(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(PdfViewerActivity.this, "加载失败: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // 将PDF保存到本地临时文件
                    File pdfFile = savePdfToLocal(response.body().byteStream());

                    if (pdfFile != null) {
                        runOnUiThread(() -> loadPdfFromFile(pdfFile));
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(PdfViewerActivity.this, "无法保存PDF文件",
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    /**
     * 将PDF保存到本地存储
     */
    private File savePdfToLocal(InputStream inputStream) {
        try {
            // 高版本Android不允许在外置目录随意创建，只能在私有目录storage/emulated/0/Android/data/com.microhabit/files进行创建
            String path =  this.getExternalFilesDir(null) + "/pdf_temp";

            File directory = new File(path);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    showShortText("创建pdf_temp目录失败");
                    return null;
                }
            }

            File pdfFile = new File(directory, "temp_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdfFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return pdfFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从本地文件加载PDF
     */
    private void loadPdfFromFile(File file) {
        pdfView.fromFile(file)
                .defaultPage(0)
                // 允许滑动翻页
                .enableSwipe(true)
                // 垂直滑动
                .swipeHorizontal(false)
                .enableDoubletap(true)
                // 显示滚动条
                .scrollHandle(new DefaultScrollHandle(this))
                .enableAnnotationRendering(false)
                .password(null)
                .load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理临时文件
        deleteTempPdfFiles();
    }

    /**
     * 删除临时PDF文件
     */
    private void deleteTempPdfFiles() {
        String path =  this.getExternalFilesDir(null) + "/pdf_temp";
        File directory = new File(path);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("temp_") && file.getName().endsWith(".pdf")) {
                        file.delete();
                    }
                }
            }
        }
    }
}
