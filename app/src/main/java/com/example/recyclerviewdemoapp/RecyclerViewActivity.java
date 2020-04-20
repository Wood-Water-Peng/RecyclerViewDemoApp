package com.example.recyclerviewdemoapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.recyclerviewdemoapp.databinding.ActivityMainBinding;
import com.example.recyclerviewdemoapp.databinding.ActivityRecyclerViewBinding;

public class RecyclerViewActivity extends AppCompatActivity {
    private ActivityRecyclerViewBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityRecyclerViewBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewBinding.recycler.setLayoutManager(new LinearLayoutManager(RecyclerViewActivity.this));
                viewBinding.recycler.setAdapter(new RecyclerView.Adapter() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        return new ItemHolder(new NormalItemView(parent.getContext()));
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        TextView textView = holder.itemView.findViewById(R.id.normal_item_tv);
                        textView.setText("position:" + position);
                    }

                    @Override
                    public int getItemCount() {
                        return 3;
                    }
                });
            }
        }, 1000);
    }

    class ItemHolder extends RecyclerView.ViewHolder {

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
