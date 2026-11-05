package com.gsoft.inventory.service.import_net_assets;

import android.widget.CompoundButton;
import com.gsoft.inventory.utils.domain.Accounts;

import java.util.List;

/**
 * @author 10904
 */
public class OnDepartButtonCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
  /*  private String departCode = "";

    private final List<String> chooseDeparts;*/

    private Accounts account;

    private final List<Accounts> chooseDeparts;

    public OnDepartButtonCheckedChangeListener(Accounts account, List<Accounts> chooseDeparts) {
        this.account = account;
        this.chooseDeparts = chooseDeparts;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            chooseDeparts.add(account);
        } else {
            chooseDeparts.remove(account);
        }
    }
   /* public OnDepartButtonCheckedChangeListener(String title, List<String> chooseDeparts) {
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
    }*/
}
