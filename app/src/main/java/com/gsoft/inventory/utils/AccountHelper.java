package com.gsoft.inventory.utils;

import android.accounts.Account;

import java.util.ArrayList;
import java.util.List;

public class AccountHelper {

    /**
     * 根据账套编号获取账套信息
     *
     * @param ACCTSUITEID
     * @return
     */
    public static Accounts getACCTSUITEID(String ACCTSUITEID) {
        if (StringUtils.isNullOrEmpty(ACCTSUITEID)) return null;
        String[] whereArray = {ACCTSUITEID};
        List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH=?", whereArray, null, "Id ASC", "1");
        if (accountsList == null || accountsList.size() == 0) {
            return null;
        }
        return accountsList.get(0);
    }

    public static String getUserName(String userNo) {
        if (StringUtils.isNullOrEmpty(userNo)) return "";
        List<Accounts> accountsList = Accounts.find(Accounts.class, "YHBH = ?", userNo);
        if (accountsList == null || accountsList.size() == 0) {
            return "";
        }
        return accountsList.get(0).getYHXM();
    }

    public static List<String> getDepartList(String ACCTSUITEID) {
        List<String> departList = new ArrayList<>();
        if (StringUtils.isNullOrEmpty(ACCTSUITEID)) return departList;
        List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH = ?", ACCTSUITEID);
        for (Accounts accounts : accountsList) {
            if (!departList.contains(accounts.getBMMC()))
                departList.add(accounts.getBMMC());
        }
        return departList;
    }

    /**
     * 获取部门编码
     *
     * @param departName
     * @return
     */
    public static String getDepartCode(String accountid, String departName) {
        if (StringUtils.isNullOrEmpty(departName)) return "";
        List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH=? AND BMMC = ?", accountid, departName);
        return accountsList.size() == 0 ? "" : accountsList.get(0).getBMBH();
    }

    /**
     * 获取部门名称
     *
     * @param departCode
     * @return
     */
    public static String getDepartName(String departCode) {
        if (StringUtils.isNullOrEmpty(departCode)) return "";
        List<Accounts> accountsList = Accounts.find(Accounts.class, "BMBH = ?", departCode);
        return accountsList.size() == 0 ? "" : accountsList.get(0).getBMMC();
    }

    /**
     * 获取单位编码
     *
     * @param danweiName
     * @return
     */
    public static String getDanweiCode(String danweiName) {
        if (StringUtils.isNullOrEmpty(danweiName)) return "";
        List<Accounts> accountsList = Accounts.find(Accounts.class, "DWMC = ?", danweiName);
        return accountsList.size() == 0 ? "" : accountsList.get(0).getDWBH();
    }

    /**
     * 获取单位名称
     *
     * @param danweiCode
     * @return
     */
    public static String getDanweiName(String accountid, String danweiCode) {
        if (StringUtils.isNullOrEmpty(accountid) || StringUtils.isNullOrEmpty(danweiCode))
            return "";
        String[] whereArray = {accountid, danweiCode};
        List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH=? AND DWBH = ?", whereArray, null, "Id ASC", "1");
        return accountsList.size() == 0 ? "" : accountsList.get(0).getDWMC();
    }


    public static List<String> getDanweiList(String ACCTSUITEID) {
        List<String> departList = new ArrayList<>();
        if (StringUtils.isNullOrEmpty(ACCTSUITEID)) return departList;
        List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH = ?", ACCTSUITEID);
        for (Accounts accounts : accountsList) {
            if (!departList.contains(accounts.getDWMC()))
                departList.add(accounts.getDWMC());
        }
        return departList;
    }

    public static List<String> getUserList(String ACCTSUITEID) {
        if (SysConfig.userArray == null || SysConfig.userArray.size() == 0) {
            List<String> userList = new ArrayList<>();
            if (StringUtils.isNullOrEmpty(ACCTSUITEID)) return userList;
            List<Accounts> accountsList = Accounts.find(Accounts.class, "ZTBH = ?", ACCTSUITEID);
            for (Accounts accounts : accountsList) {
                userList.add(accounts.getYHXM());
            }
            SysConfig.userArray = userList;
        }
        return SysConfig.userArray;
    }
}
