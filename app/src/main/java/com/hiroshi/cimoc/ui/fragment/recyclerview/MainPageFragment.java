package com.hiroshi.cimoc.ui.fragment.recyclerview;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hiroshi.cimoc.R;
import com.hiroshi.cimoc.fresco.ControllerBuilderProvider;
import com.hiroshi.cimoc.manager.SourceManager;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.presenter.BasePresenter;
import com.hiroshi.cimoc.presenter.ResultPresenter;
import com.hiroshi.cimoc.ui.activity.DetailActivity;
import com.hiroshi.cimoc.ui.activity.SearchActivity;
import com.hiroshi.cimoc.ui.adapter.BaseAdapter;
import com.hiroshi.cimoc.ui.adapter.ResultAdapter;
import com.hiroshi.cimoc.ui.view.ResultView;
import com.hiroshi.cimoc.utils.HintUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class MainPageFragment extends RecyclerViewFragment implements ResultView, BaseAdapter.OnItemClickListener {

    private ResultPresenter mPresenter;

    private ResultAdapter mResultAdapter;
    private LinearLayoutManager mLayoutManager;
    private ControllerBuilderProvider mProvider;

    @Override
    protected BasePresenter initPresenter() {

//        String keyword = getIntent().getStringExtra(Extra.EXTRA_KEYWORD);
//        int[] source = getIntent().getIntArrayExtra(Extra.EXTRA_SOURCE);
//        boolean strictSearch = getIntent().getBooleanExtra(Extra.EXTRA_STRICT, true);

        mPresenter = new ResultPresenter(null, "", false);
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initView() {
        setHasOptionsMenu(true);
        super.initView();
//        mResultAdapter.setOnItemClickListener(this);
        mProvider = new ControllerBuilderProvider(this.getActivity(), SourceManager.getInstance(this).new HeaderGetter(), true);
        mResultAdapter.setProvider(mProvider);
        mResultAdapter.setTitleGetter(SourceManager.getInstance(this).new TitleGetter());
        mRecyclerView.setHasFixedSize(true);
//        mRecyclerView.setLayoutManager(mLayoutManager);
//        mRecyclerView.addItemDecoration(mResultAdapter.getItemDecoration());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mLayoutManager.findLastVisibleItemPosition() >= mResultAdapter.getItemCount() - 4 && dy > 0) {
                    load();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        mProvider.pause();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        mProvider.resume();
                        break;
                }
            }
        });
//        mRecyclerView.setAdapter(mResultAdapter);

    }

    @Override
    protected BaseAdapter initAdapter() {

        mResultAdapter = new ResultAdapter(this.getActivity(), new LinkedList<Comic>());
        return mResultAdapter;
    }

    @Override
    protected RecyclerView.LayoutManager initLayoutManager() {
        mLayoutManager = new LinearLayoutManager(this.getActivity());
        return mLayoutManager;
    }

    @Override
    protected void initData() {

        load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mProvider != null) {
            mProvider.clear();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_source, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.comic_search:
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View view, int position) {
        Comic comic = mResultAdapter.getItem(position);
        Intent intent = DetailActivity.createIntent(this.getActivity(), null, comic.getSource(), comic.getCid());
        startActivity(intent);
    }


    @Override
    public void onSearchSuccess(Comic comic) {
        hideProgressBar();
        mResultAdapter.add(comic);
    }

    @Override
    public void onLoadSuccess(List<Comic> list) {
        hideProgressBar();
        mResultAdapter.addAll(list);
    }

    @Override
    public void onLoadFail() {
        hideProgressBar();
        HintUtils.showToast(getActivity(),R.string.common_parse_error);
    }

    @Override
    public void onSearchError() {
        hideProgressBar();
        HintUtils.showToast(getActivity(),R.string.result_empty);
    }


    private void load() {
        if (mPresenter != null){
            showProgressBar();
            mPresenter.loadSearch();
        }
    }
}
