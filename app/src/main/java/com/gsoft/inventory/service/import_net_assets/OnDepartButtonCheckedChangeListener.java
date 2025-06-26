package com.gsoft.inventory.service.import_net_assets;

import android.widget.CompoundButton;

import java.util.List;

/**
 * @author 10904
 */
public class OnDepartButtonCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
    private String departCode = "";

    private final List<String> chooseDeparts;

    public OnDepartButtonCheckedChangeListener(String title, List<String> chooseDeparts) {
        this.departCode = title;
        this.chooseDeparts = chooseDeparts;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            chooseDeparts.add(departCode);
        } else {
            chooseDeparts.remove(departCode);
        }
    }
}
