package com.gsoft.inventory;

import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gsoft.inventory.adapter.WaitingSignaturePersonListAdapter;
import com.gsoft.inventory.entities.SignaturePerson;
import com.gsoft.inventory.utils.BaseCompatActivity;
import com.gsoft.inventory.utils.DataTransmitHelper;
import com.gsoft.inventory.utils.OkHttpUtils;
import com.gsoft.inventory.utils.StringCallback;
import com.gsoft.inventory.utils.StringUtils;
import com.gsoft.inventory.utils.SysDefine;
import com.qmuiteam.qmui.widget.QMUITopBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SignatureActivity extends BaseCompatActivity {

    WaitingSignaturePersonListAdapter signaturePersonListAdapter;
    List<SignaturePerson> signaturePeoples;
    ListView listView;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        initializeView();
    }

    @Override
    public void initializeView() {
        QMUITopBar topBar = findViewById(R.id.topbar);
        topBar.setTitle("资产盘点确认");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        listView = findViewById(R.id.listPersons);
        signaturePeoples = new ArrayList<>();

        /*List<Assets> assets = Assets.listAll(Assets.class, "CUSTODIAN ASC");

        for (Assets asset : assets) {
            SignaturePerson person = findPerson(asset.getCUSTODIAN());
            if (person == null) {
                person = new SignaturePerson(asset.getCUSTODIAN());
                signaturePeoples.add(person);
            }
            person.setAmount(person.getAmount() + 1);
        }*/

        signaturePersonListAdapter = new WaitingSignaturePersonListAdapter(signaturePeoples, mContext);
        signaturePersonListAdapter.setSendMessageCallBack(new StringCallback() {
            @Override
            public void onResponseSuccess(String result) {
                runOnUiThread(() -> showShortText("发送成功！"));
            }

            @Override
            public void onResponseFailure(String errMessage) {
                runOnUiThread(() -> showShortText("发送失败，请稍后重试"));
            }
        });
        listView.setAdapter(signaturePersonListAdapter);

        new SearchAssetPersonTask().execute();
    }

    public void refreshPersons(List<SignaturePerson> peoples) {
        signaturePeoples.addAll(peoples);


        Collections.sort(signaturePeoples, new Comparator<SignaturePerson>() {
            @Override
            public int compare(SignaturePerson o1, SignaturePerson o2) {
                return o1.getDepart().compareTo(o2.getDepart());
            }
        });

        // signaturePeoples.sort();
        signaturePersonListAdapter.notifyDataSetChanged();
    }

    public SignaturePerson findPerson(String name) {
        if (signaturePeoples == null || signaturePeoples.size() == 0) return null;
        for (SignaturePerson person : signaturePeoples) {
            if (person.getName().equals(name)) return person;
        }
        return null;
    }

    private class SearchAssetPersonTask extends AsyncTask<Void, Void, List<SignaturePerson>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //assetsList.clear();
            showProgressDialog("", "正在检索");
        }

        @Override
        protected List<SignaturePerson> doInBackground(Void... params) {
            String responseString = OkHttpUtils.getInstance().get(String.format(SysDefine.SERVER_GET_ASSETS_PEOPLES(), application.getLoginAccount().getZTBH()));
            Log.e("fetch users", responseString);
            if (StringUtils.isNullOrEmpty(responseString) || responseString.startsWith("ERROR")) {
                return null;
            }

            List<SignaturePerson> personList = new ArrayList<>();
            try {
                List<String> results = gson.fromJson(responseString, new TypeToken<List<String>>() {
                }.getType());
                if (results == null || results.size() == 0) {
                    return null;
                }
                for (String dataLine : results) {
                    SignaturePerson person = DataTransmitHelper.transferSignaturePerson(dataLine);
                    if (person != null) personList.add(person);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return personList;
        }

        @Override
        protected void onPostExecute(List<SignaturePerson> s) {
            super.onPostExecute(s);
            hideProgressDialog();
            refreshPersons(s);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            hideProgressDialog();
        }

        @Override
        protected void onCancelled(List<SignaturePerson> s) {
            super.onCancelled(s);
            hideProgressDialog();
        }
    }
}