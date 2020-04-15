package com.example.recyclerviewdemoapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

abstract class CommonRvAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<T> dataSource = new ArrayList<>();

    public CommonRvAdapter(List<T> dataSource) {
        if (dataSource != null) {
            this.dataSource = dataSource;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterItemView<T> adapterItemView = createItem(parent.getContext(),viewType);
        return new CommonViewHolder(parent.getContext(), parent, adapterItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CommonViewHolder) {
            CommonViewHolder commonViewHolder = (CommonViewHolder) holder;
            commonViewHolder.adapterItemView.bindData(dataSource.get(position), position);
        }
    }

    abstract AdapterItemView<T> createItem(Context context,int viewType);


    abstract int getItemType(T data,int position);

    @Override
    public int getItemViewType(int position) {
        return getItemType(dataSource.get(position),position);
    }

    @Override
    public int getItemCount() {
        return dataSource.size();
    }

    private class CommonViewHolder<T> extends RecyclerView.ViewHolder {
        AdapterItemView<T> adapterItemView;

        public CommonViewHolder(Context context, ViewGroup parent, @NonNull AdapterItemView<T> adapterItemView) {
            super((adapterItemView instanceof View) ? (View) adapterItemView : LayoutInflater.from(context).inflate(adapterItemView.getLayoutResId(), parent, false));
            this.adapterItemView = adapterItemView;
            adapterItemView.initView(itemView);
        }

    }
}

interface AdapterItemView<T> {
    @LayoutRes
    int getLayoutResId();

    void initView(View view);

    void bindData(T data, int position);
}