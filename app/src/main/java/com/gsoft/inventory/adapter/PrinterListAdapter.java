package com.gsoft.inventory.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gsoft.inventory.R;
import com.gsoft.inventory.entities.JCPrinter;
import com.gsoft.inventory.utils.FileBean;

import java.util.List;

public class PrinterListAdapter extends BaseAdapter {
    private List<JCPrinter> printerList;

    private LayoutInflater mInflator;
    private Context mContext;

    public PrinterListAdapter(List<JCPrinter> fileList, Context context) {
        super();
        printerList = fileList;
        mContext = context;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return printerList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return printerList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewContainer viewContainer;
        // General ListView optimization code.
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.printer_item, null);
            viewContainer = new ViewContainer();
            viewContainer.tv_printer_name = view.findViewById(R.id.tv_printer_name);
            view.setTag(viewContainer);
        } else {
            viewContainer = (ViewContainer) view.getTag();
        }

        JCPrinter printer = printerList.get(position);
        viewContainer.tv_printer_name.setText(printer.printerno);
        //viewContainer.tvSize.setText(fileBean.getSize() + "");
        return view;
    }

    //cell
    public class ViewContainer {
        /*条码编号*/
        TextView tv_printer_name;
    }

}
