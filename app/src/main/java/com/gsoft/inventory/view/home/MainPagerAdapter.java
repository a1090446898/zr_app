package com.gsoft.inventory.view.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.gsoft.inventory.abs.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 10904
 */
public class MainPagerAdapter extends FragmentStateAdapter {


    private List<BaseFragment> fragmentList;


    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity,  List<BaseFragment> fragmentList) {
        super(fragmentActivity);
        this.fragmentList = fragmentList;



    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

}
