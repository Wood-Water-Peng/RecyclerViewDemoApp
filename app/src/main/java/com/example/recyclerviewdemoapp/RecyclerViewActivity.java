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

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewActivity extends AppCompatActivity {
    private ActivityRecyclerViewBinding viewBinding;
    private List<String> testBeanList = new ArrayList<>();
    private static final int LEN = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityRecyclerViewBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        for (int i = 0; i < LEN; i++) {
            testBeanList.add(i + "");
        }
        final RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
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
                return testBeanList.size();
            }
        };
        viewBinding.recycler.setLayoutManager(new LinearLayoutManager(RecyclerViewActivity.this));
        viewBinding.recycler.setAdapter(adapter);
        viewBinding.buttonAddOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testBeanList.add(testBeanList.size() + "");
                adapter.notifyItemInserted(testBeanList.size());
            }
        });
        viewBinding.buttonAddTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 2; i++) {
                    testBeanList.add(testBeanList.size() + "");
                    adapter.notifyItemInserted(testBeanList.size());
                }
            }
        });
        viewBinding.buttonRemoveOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = testBeanList.size() - 1;
                testBeanList.remove(index);
                adapter.notifyItemRemoved(index);
            }
        });
        viewBinding.buttonRemoveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i<testBeanList.size(); i++) {
                    testBeanList.remove(i);
                    adapter.notifyItemRemoved(i);
                }
            }
        });
        viewBinding.recycler.getItemAnimator().setRemoveDuration(3000);
    }

    class ItemHolder extends RecyclerView.ViewHolder {

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
