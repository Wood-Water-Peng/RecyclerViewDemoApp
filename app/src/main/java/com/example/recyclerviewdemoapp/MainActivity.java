package com.example.recyclerviewdemoapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.recyclerviewdemoapp.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
//        viewBinding.activityMainRecycler.setAdapter(new ItemAdapter(initData()));
//        viewBinding.activityMainRecycler.setLayoutManager(new LinearLayoutManager(this));
        viewBinding.flexTabLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "position:", Toast.LENGTH_SHORT).show();
            }
        });
        viewBinding.flexTabLayout.setAdapter(new FlexTabLayout.Adapter() {
            @Override
            public void onItemClicked(View itemView, int position) {
                Toast.makeText(MainActivity.this, "position:" + position, Toast.LENGTH_SHORT).show();
            }

            @NonNull
            @Override
            public FlexTabLayout.FlexItemHolder onCreateItemHolder(@NonNull ViewGroup parent, final int position) {
                final View itemView;
                if (position == getItemCount() - 1) {
                    itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.normal_item_plus, parent, false);
                    return new ItemHolder(itemView);
                }
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.normal_item, parent, false);
//                itemView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        itemView.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                onItemClicked(v, position);
//                            }
//                        });
//                    }
//                });
                return new ItemHolder(itemView);

            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });

    }

    class ItemHolder extends FlexTabLayout.FlexItemHolder {

        protected ItemHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private List<String> initData() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            data.add("测试");
        }
        return data;
    }

    private class ItemAdapter extends CommonRvAdapter<String> {

        public ItemAdapter(List<String> dataSource) {
            super(dataSource);
        }

        @Override
        AdapterItemView<String> createItem(Context context, int viewType) {
            if (viewType == 1) {
                SimpleTextWithColorItem simpleTextWithColorItem = new SimpleTextWithColorItem(context);
                simpleTextWithColorItem.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return simpleTextWithColorItem;
            }
            return new SimpleTextItem();
        }

        @Override
        int getItemType(String data, int position) {
            if (position == 10) {
                return 1;
            }
            return 0;
        }
    }

    private class SimpleTextItem implements AdapterItemView<String> {
        TextView tv;

        @Override
        public int getLayoutResId() {
            return R.layout.normal_item;
        }

        @Override
        public void initView(View view) {
            tv = view.findViewById(R.id.normal_item_tv);
        }

        @Override
        public void bindData(String data, int position) {
            tv.setText(data + "_" + position);
        }
    }

    private class SimpleTextWithColorItem extends FrameLayout implements AdapterItemView<String> {
        TextView tv;

        public SimpleTextWithColorItem(Context context) {
            this(context, null);
        }

        public SimpleTextWithColorItem(Context context, @Nullable AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public SimpleTextWithColorItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            LayoutInflater.from(context).inflate(getLayoutResId(), this, true);
        }

        @Override
        public int getLayoutResId() {
            return R.layout.normal_item_with_color;
        }

        @Override
        public void initView(View view) {
            tv = view.findViewById(R.id.normal_item_with_color_tv);
        }

        @Override
        public void bindData(String data, int position) {
            tv.setText(data + "_" + position);
        }
    }
}
