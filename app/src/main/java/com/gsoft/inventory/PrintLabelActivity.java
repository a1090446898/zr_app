package com.gsoft.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.PrintCompatActivity;
import com.gsoft.inventory.utils.StringUtils;

import static com.gsoft.inventory.utils.SysDefine.REQESTCODE_WAITINGBLUETOOTH_BARCODE;
import static com.gsoft.inventory.utils.SysDefine.REQESTCODE_WAITINGBLUETOOTH_QRCODE;

public class PrintLabelActivity extends PrintCompatActivity {

    Button button;
    Button buttonBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_label);
        initializeView();
    }

    @Override
    public void initializeView() {
        buttonBar = (Button) findViewById(R.id.buttonBar);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isPrinterConnected()) {
                    Intent intentBluetooth = new Intent(mContext, BluetoothDeviceActivity.class);
                    startActivityForResult(intentBluetooth, REQESTCODE_WAITINGBLUETOOTH_QRCODE);
                    return;
                }
                printQRCode("1211103000308|管理部门警务保障部|资产名称：帐篷 使用人：蔡仕贵 使用人：蔡仕贵");

                /*if (getBSDPrinter().PTK_ConnectBluetooth(share.connectedDevice.address) == 1) {
                    showShortText("打印机连接失败！");
                    return;
                }

                getBSDPrinter().PTK_ClearBuffer();
                getBSDPrinter().PTK_SetPrintSpeed(4);
                getBSDPrinter().PTK_SetDarkness(10);
                getBSDPrinter().PTK_SetDirection("T");
                getBSDPrinter().PTK_SetLabelWidth(360);
                getBSDPrinter().PTK_SetLabelHeight(240, 16, 0, false);
                getBSDPrinter().PTK_DrawText(50, 20, 0, '6', 30, 30, 'N', "\"贵州兴财通科技有限公司\"");
                getBSDPrinter().PTK_DrawBarcode(80, 70, 0, "1", 2, 4, 80, 'N', "\"2018020000006\"");
                //getBSDPrinter().PTK_DrawBarcode(50, 80, 0, "1C", 2, 4, 80, 'N', "\"4101841982072\"");
                getBSDPrinter().PTK_DrawText(50, 180, 0, '6', 30, 30, 'N', "\"贵州兴财通科技有限公司\"");
                // getBSDPrinter().PTK_DrawText(5, 5, 0, '1', 20, 20, 'N', "aaaaaaa");
                //getBSDPrinter().PTK_PcxGraphicsDel("postek");
                getBSDPrinter().PTK_PrintLabel(1, 0);*/

            }
        });

        buttonBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPrinterConnected()) {
                    Intent intentBluetooth = new Intent(mContext, BluetoothDeviceActivity.class);
                    startActivityForResult(intentBluetooth, REQESTCODE_WAITINGBLUETOOTH_BARCODE);
                    return;
                }
                printBarCode("1211103000308");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (!isPrinterConnected()) {
                showShortText("打印机连接失败！");
                return;
            }
            if (requestCode == REQESTCODE_WAITINGBLUETOOTH_BARCODE) {
                printBarCode("1211103000308");
            } else if (requestCode == REQESTCODE_WAITINGBLUETOOTH_QRCODE) {
                printQRCode("资产名称：台式电脑\n管理部门警务保障部\n保管人：蔡仕贵\n使用人：蔡仕贵");
            }
        }
    }
}
