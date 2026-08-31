package com.gsoft.inventory.view.scan;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.gsoft.inventory.R;
import com.gsoft.inventory.utils.CommonPermissionUtils;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import org.w3c.dom.Text;

public class ScanCodeResultActivity extends AppCompatActivity {


    private Button btnScanCode;
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner_result);




    }

}
