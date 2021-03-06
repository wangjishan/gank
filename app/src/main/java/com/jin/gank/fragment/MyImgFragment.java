package com.jin.gank.fragment;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jin.gank.GankActivity;
import com.jin.gank.PictureActivity;
import com.jin.gank.R;
import com.jin.gank.data.Constant;
import com.jin.gank.data.GankCategory;
import com.jin.gank.databinding.ItemImgBinding;
import com.jin.gank.network.RetrofitHelp;
import com.malinskiy.superrecyclerview.OnMoreListener;
import com.malinskiy.superrecyclerview.SuperRecyclerView;
import com.vlonjatg.progressactivity.ProgressActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.OvershootInLeftAnimator;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Administrator on 2015/8/25.
 */
public class MyImgFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, OnMoreListener {
    private static final String TAG = "MyImgFragment";
    @Bind(R.id.progressActivity)
    ProgressActivity mProgressActivity;
    @Bind(R.id.superRecyclerView_img)
    SuperRecyclerView mSuperRecyclerViewImg;


    private List<GankCategory.ResultsEntity> mGirls;
    private String[] mCategoryArray;
    private LayoutInflater mInflater;
    private MyAdapter mAdapter;
    private boolean isLoadMore = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCategoryArray = getResources().getStringArray(R.array.category_list);
        mInflater = LayoutInflater.from(getActivity());

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container = (ViewGroup) inflater.inflate(R.layout.fragment_img, null, false);
        ButterKnife.bind(this, container);
        return container;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSuperRecyclerViewImg.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mSuperRecyclerViewImg.getRecyclerView().setItemAnimator(new OvershootInLeftAnimator());
        mSuperRecyclerViewImg.setRefreshListener(this);
        mSuperRecyclerViewImg.setupMoreListener(this, 10);
        mSuperRecyclerViewImg.setRefreshingColorResources(android.R.color.holo_orange_light, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_red_light);
        if (mGirls == null || mGirls.size() == 0) {
            loadGirlData(mCategoryArray[0], Constant.API_COUNT, 1, true);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }


    //加载数据
    private void loadGirlData(String category, int count, int page, boolean firstLoad) {
        if (firstLoad) {
            mProgressActivity.showLoading();
            if (mGirls != null)
                mGirls.clear();
        }
        Observable.zip(RetrofitHelp.getApi().listGankCategory(category, count, page)
                , RetrofitHelp.getApi().listGankCategory(mCategoryArray[5], count, page), (Girls, Videos) ->
                        createGirlDataWithgetFreeVideoDesc(Girls, Videos)
        ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(Girls -> {
                    //判断是否已经加载到最后一页
                    if (Girls.getResults().size() < 10)
                        isLoadMore = false;

                    return Observable.just(Girls.getResults());
                })
                .subscribe(Girls -> {
                            mProgressActivity.showContent();

                            if (mAdapter == null) {
                                mGirls = Girls;
                                mAdapter = new MyAdapter(mGirls);
                                mSuperRecyclerViewImg.setAdapter(mAdapter);
                            } else {
                                if (!firstLoad)
                                    mGirls.addAll(mGirls.size(), Girls);

                                mAdapter.setGirls(mGirls);
                                mAdapter.notifyDataSetChanged();
                            }

                            mSuperRecyclerViewImg.hideMoreProgress();
                            mSuperRecyclerViewImg.getSwipeToRefresh().setRefreshing(false);
                        },
                        err -> mProgressActivity.showError(null, "错误", err.toString(), "重试", v -> loadGirlData(category, Constant.API_COUNT, 1, true)));
    }

    private GankCategory createGirlDataWithgetFreeVideoDesc(GankCategory girls, GankCategory loves) {
        for (int i = 0; i < girls.getResults().size(); i++) {
            GankCategory.ResultsEntity girl = girls.getResults().get(i);
            if (loves.getResults() != null && loves.getResults().size() > i)
                girl.setDesc(girl.getDesc() + "  " + loves.getResults().get(i).getDesc());
        }
        return girls;
    }


    @Override
    public void onMoreAsked(int numberOfItems, int numberBeforeMore, int currentItemPos) {
        if (isLoadMore) {
            loadGirlData(mCategoryArray[0], Constant.API_COUNT, numberOfItems / Constant.API_COUNT + 1, false);
        } else {
            mSuperRecyclerViewImg.hideMoreProgress();
        }
    }

    @Override
    public void onRefresh() {
        isLoadMore = true;
        loadGirlData(mCategoryArray[0], Constant.API_COUNT, 1, true);
    }


    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> implements View.OnClickListener {

        public void setGirls(List<GankCategory.ResultsEntity> girls) {
            this.girls = girls;
        }

        private List<GankCategory.ResultsEntity> girls;

        public MyAdapter(List<GankCategory.ResultsEntity> girls) {
            super();
            this.girls = girls;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View contentView = mInflater.inflate(R.layout.item_img, parent, false);
            MyViewHolder holder = new MyViewHolder(contentView);
            return holder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            GankCategory.ResultsEntity gril = girls.get(position);
            holder.bind(gril);
            holder.mImgBinding.welfareImg.setOriginalSize(50, 50);
            holder.mImgBinding.welfareImg.setTag(R.id.image_tag, gril);
            holder.mImgBinding.welfareImg.setOnClickListener(this);
            holder.mImgBinding.welfareDesc.setTag(R.id.image_tag, gril);
            holder.mImgBinding.welfareDesc.setOnClickListener(this);

        }

        @Override
        public int getItemCount() {
            return girls.size();
        }

        @Override
        public void onClick(View v) {
            startPictureActivity((GankCategory.ResultsEntity) v.getTag(R.id.image_tag), v);
        }


        class MyViewHolder extends RecyclerView.ViewHolder {
            private ItemImgBinding mImgBinding;

            public MyViewHolder(View convertView) {
                super(convertView);
                mImgBinding = DataBindingUtil.bind(convertView);

            }

            public void bind(GankCategory.ResultsEntity Gril) {
                mImgBinding.setGirl(Gril);
            }
        }

        private void startPictureActivity(GankCategory.ResultsEntity girl, View sharedView) {
            if (sharedView.getId() == R.id.welfare_img) {
                Intent intent = new Intent(getActivity(), PictureActivity.class);
                intent.putExtra(PictureActivity.EXTRA_IMAGE_URL, girl.getUrl());
                intent.putExtra(PictureActivity.EXTRA_IMAGE_TITLE, girl.getDesc());
                //打开activity 动画
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), sharedView,
                        PictureActivity.TRANSIT_PIC);
                ActivityCompat.startActivity(getActivity(), intent, optionsCompat.toBundle());
            } else {
                Intent intent = new Intent(getActivity(), GankActivity.class);
                int position = mGirls.indexOf(girl);
                List<String> dates = new ArrayList<>();
                //判断是否超出最大list大小
                if (position + 5 > mGirls.size()) {
                    for (int i = position; i < mGirls.size(); i++) {
                        String PublishedAt = mGirls.get(i).getPublishedAt().split("T")[0];
                        dates.add(PublishedAt);
                    }
                } else {
                    for (int i = position; i < position + 5; i++) {
                        String PublishedAt = mGirls.get(i).getPublishedAt().split("T")[0];
                        dates.add(PublishedAt);
                    }
                }
                // String PublishedAt = girl.getPublishedAt().split("T")[0];
                intent.putStringArrayListExtra(GankActivity.EXTRA_GANK_DATA, (ArrayList<String>) dates);
                startActivity(intent);
            }

        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

}
