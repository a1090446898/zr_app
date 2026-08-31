package com.gsoft.inventory.ui;

import android.content.Context;
import android.view.ViewGroup;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogMenuItemView;

import java.util.ArrayList;

public class MyQMUIDialog extends QMUIDialog {

    public MyQMUIDialog(Context context) {
        super(context);
    }

    public MyQMUIDialog(Context context, int styleRes) {
        super(context, styleRes);
    }

    /**
     * 多选类型的对话框 Builder
     */
    public static class MultiCheckableDialogBuilder extends MenuBaseDialogBuilder<MyQMUIDialog.MultiCheckableDialogBuilder> {

        /**
         * 该 int 的每一位标识菜单的每一项是否被选中 (1为选中,0位不选中)
         */
        private int mCheckedItems;

        public MultiCheckableDialogBuilder(Context context) {
            super(context);
        }

        /**
         * 设置被选中的菜单项的下标
         *
         * @param checkedItems <b>注意: 该 int 参数的每一位标识菜单项的每一项是否被选中</b>
         *                     <p>如 20 表示选中下标为 1、3 的菜单项, 因为 (2<<1) + (2<<3) = 20</p>
         */
        public MyQMUIDialog.MultiCheckableDialogBuilder setCheckedItems(int checkedItems) {
            mCheckedItems = checkedItems;
            return this;
        }

        /**
         * 设置被选中的菜单项的下标
         *
         * @param checkedIndexes 被选中的菜单项的下标组成的数组,如 [1,3] 表示选中下标为 1、3 的菜单项
         */
        public MyQMUIDialog.MultiCheckableDialogBuilder setCheckedItems(int[] checkedIndexes) {
            int checkedItemRecord = 0;
            if (checkedIndexes != null) {
                for (int checkedIndexe : checkedIndexes) {
                    checkedItemRecord += 2 << (checkedIndexe);
                }
            }
            return setCheckedItems(checkedItemRecord);
        }

        /**
         * 添加菜单项
         *
         * @param items    所有菜单项的文字
         * @param listener 菜单项的点击事件,可以在点击事件里调用 {@link #setCheckedItems(int[])}} 来设置选中某些菜单项
         */
        public MyQMUIDialog.MultiCheckableDialogBuilder addItems(CharSequence[] items, OnClickListener listener) {
            for (final CharSequence item : items) {
                addItem(new ItemViewFactory() {
                    @Override
                    public QMUIDialogMenuItemView createItemView(Context context) {
                        return new QMUIDialogMenuItemView.CheckItemView(context, true, item);
                    }
                }, listener);
            }
            return this;
        }

        @Override
        public MyQMUIDialog.MultiCheckableDialogBuilder addItem(QMUIDialogMenuItemView itemView, OnClickListener listener) {
//            if(mMenuItemViewsFactoryList.size() >= 32){
//                throw new RuntimeException("there are more than 32 items, please use LiseView to improve performance!!");
//            }
            return super.addItem(itemView, listener);
        }

        @Override
        public MyQMUIDialog.MultiCheckableDialogBuilder addItem(ItemViewFactory itemViewFactory, OnClickListener listener) {
//            if(mMenuItemViewsFactoryList.size() >= 32){
//                throw new RuntimeException("there are more than 32 items, please use LiseView to improve performance!!");
//            }
            return super.addItem(itemViewFactory, listener);
        }

        @Override
        protected void onCreateContent(QMUIDialog dialog, ViewGroup parent, Context context) {
            super.onCreateContent(dialog, parent, context);
            for (int i = 0; i < mMenuItemViews.size(); i++) {
                QMUIDialogMenuItemView itemView = mMenuItemViews.get(i);
                int v = 2 << i;
                itemView.setChecked((v & mCheckedItems) == v);
            }
        }

        @Override
        protected void onItemClick(int index) {
            QMUIDialogMenuItemView itemView = mMenuItemViews.get(index);
            itemView.setChecked(!itemView.isChecked());
        }

        /**
         * @return 被选中的菜单项的下标 <b>注意: 如果选中的是1，3项(以0开始)，因为 (2<<1) + (2<<3) = 20</b>
         */
        public int getCheckedItemRecord() {
            int output = 0;
            int length = mMenuItemViews.size();

            for (int i = 0; i < length; i++) {
                QMUIDialogMenuItemView itemView = mMenuItemViews.get(i);
                if (itemView.isChecked()) {
                    output += 2 << itemView.getMenuIndex();
                }
            }
            mCheckedItems = output;
            return output;
        }

        /**
         * @return 被选中的菜单项的下标数组。如果选中的是1，3项(以0开始)，则返回[1,3]
         */
        public int[] getCheckedItemIndexes() {
            ArrayList<Integer> array = new ArrayList<>();
            int length = mMenuItemViews.size();

            for (int i = 0; i < length; i++) {
                QMUIDialogMenuItemView itemView = mMenuItemViews.get(i);
                if (itemView.isChecked()) {
                    array.add(itemView.getMenuIndex());
                }
            }
            int[] output = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                output[i] = array.get(i);
            }
            return output;
        }

        protected boolean existCheckedItem() {
            return getCheckedItemRecord() <= 0;
        }
    }

}
