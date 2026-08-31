package com.gsoft.inventory.service.home;

import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.entities.DressManage;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.gsoft.inventory.const_data.Log_file_name.gson;

/**
 * @author 10904
 */
public class ExportTextFileTask implements Callable<List<DressManage>> {

    private String userId;

    public ExportTextFileTask(String userId){
        this.userId = userId;
    }

    @Override
    public List<DressManage> call() throws Exception {
        String urlParam = String.format(SysDefine.getFs(), userId);
        String result = OkHttpUtils.getInstance().get(urlParam);
        if (StringUtils.isNullOrEmpty(result) || result.startsWith("ERROR")) {
            throw new RuntimeException("同步出错，".concat(result));
        }
        List<String> results = gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
        if (results == null || results.isEmpty()) {
            throw new RuntimeException("数据为空");
        }

        List<DressManage> dataList = new ArrayList<>();
        for(String dataLine : results){
            if(dataLine.isEmpty() || "ERROR".equals(dataLine)){
                continue;
            }
            String[] rowStrArray = dataLine.split("\\|", -1);
            if(rowStrArray.length > 0){
                DressManage item = new DressManage();
                String docId =  rowStrArray[0].trim();
                String receiveDepart =  rowStrArray[1].trim();
                String receiveUser =  rowStrArray[2].trim();
                String receiveTime =  rowStrArray[3].trim();
                String receivePlace =  rowStrArray[4].trim();
                String giveUser =  rowStrArray[5].trim();
                String giveTime =  rowStrArray[5].trim();

                item.setDocId(docId);
                item.setReceiveDepart(receiveDepart);
                item.setReceiveUser(receiveUser);
                item.setReceiveTime(receiveTime);
                item.setReceivePlace(receivePlace);
                item.setGiveUser(giveUser);
                item.setGiveTime(giveTime);
                dataList.add(item);
            }
        }

        return dataList;
    }

    /*private boolean writeDataToFile() {
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File zrDir = new File(downloadDir, "zr");
            if (!zrDir.exists()) {
                zrDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "signature_export_" + sdf.format(new Date()) + ".txt";
            File file = new File(zrDir, fileName);

            FileWriter writer = new FileWriter(file, false);

            // &#x5199;&#x5165;&#x8868;&#x5934;
            writer.write("&#x90E8;&#x95E8;&#x540D;&#x79F0;\t&#x4EBA;&#x5458;&#x540D;&#x79F0;\t&#x53D1;&#x9001;&#x72B6;&#x6001;\t&#x53D1;&#x9001;&#x94FE;&#x63A5;\n");

            for (SignaturePerson person : signaturePeoples) {
                String department = person.getDepart() != null ? person.getDepart() : "";
                String name = person.getName() != null ? person.getName() : "";
                String status = person.getStatus() != null ? person.getStatus() : "";

                // &#x6784;&#x9020;&#x53D1;&#x9001;&#x94FE;&#x63A5;
                String smsLink = String.format("sms:%s?body=&#x8BF7;&#x5BF9;&#x8D44;&#x4EA7;&#x8FDB;&#x884C;&#x76D8;&#x70B9;&#x786E;&#x8BA4;",
                        person.getTelephone() != null ? person.getTelephone() : "");

                writer.write(String.format("%s\t%s\t%s\t%s\n",
                        department, name, status, smsLink));
            }

            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }*/
}
