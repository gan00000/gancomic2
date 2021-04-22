package com.hiroshi.cimoc.ui.fragment.recyclerview.grid;

import android.os.Bundle;

import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.presenter.BasePresenter;
import com.hiroshi.cimoc.presenter.ResultPresenter;
import com.hiroshi.cimoc.ui.view.ResultView;

import java.util.List;

import static com.hiroshi.cimoc.ui.activity.ResultActivity.LAUNCH_MODE_CATEGORY;
import static com.hiroshi.cimoc.ui.activity.ResultActivity.LAUNCH_MODE_SEARCH;

/**
 * Created by Hiroshi on 2016/7/3.
 */
public class HotPageFragment extends GridFragment implements ResultView {

    private static final int DIALOG_REQUEST_CLEAR = 1;
    private static final int DIALOG_REQUEST_INFO = 2;
    private static final int DIALOG_REQUEST_DELETE = 3;

    private static final int OPERATION_INFO = 0;
    private static final int OPERATION_DELETE = 1;

    private ResultPresenter mPresenter;
    private int type = LAUNCH_MODE_SEARCH;

    @Override
    protected BasePresenter initPresenter() {

        mPresenter = new ResultPresenter(new int[0], "", false);
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initData() {
        load();
    }

    @Override
    protected void performActionButtonClick() {

    }

    @Override
    protected int getActionButtonRes() {
        return 0;
    }

    @Override
    protected String[] getOperationItems() {
        return new String[0];
    }


    private void load() {
        switch (type) {
            case LAUNCH_MODE_SEARCH:
                mPresenter.loadSearch();
                break;
            case LAUNCH_MODE_CATEGORY:
                mPresenter.loadCategory();
                break;
        }
    }


    //ResultView implements

    @Override
    public void onSearchError() {

    }

    @Override
    public void onSearchSuccess(Comic comic) {
//        mGridAdapter.add(comic);
    }

    @Override
    public void onLoadSuccess(List<Comic> list) {
//        mGridAdapter.addAll(list);
    }

    @Override
    public void onLoadFail() {

    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {

    }
}
