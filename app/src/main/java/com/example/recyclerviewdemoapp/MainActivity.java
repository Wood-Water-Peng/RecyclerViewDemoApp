package com.example.recyclerviewdemoapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.recyclerviewdemoapp.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding viewBinding;
    private LinkedList<TestBean> testBeanList = new LinkedList<>();
    private static final int LEN = 2;
    private FlexTabLayout.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        for (int i = 0; i < LEN; i++) {
            testBeanList.add(new TestBean(false, "" + i));
        }

        adapter = new FlexTabLayout.Adapter() {

            @Override
            public void onItemClicked(View itemView, int position) {
                if (position == getItemCount() - 1) {

                } else {
                    if (itemView instanceof NormalItemView) {
                        NormalItemView normalItemView = (NormalItemView) itemView;
                        normalItemView.setSelected(!testBeanList.get(position).isSelected());
                        testBeanList.get(position).setSelected(normalItemView.isSelected());
                    }
                }
            }

            @NonNull
            @Override
            public FlexTabLayout.FlexItemHolder onCreateItemHolder(@NonNull ViewGroup parent, final int position) {
//                if (position == getItemCount() - 1) {
////                    final ItemHolder itemHolder = new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.normal_item_plus, parent, false));
////                    return itemHolder;
//                    return new ItemHolder(new PlusButtonItemView(parent.getContext()));
//                }
                final NormalItemView view = new NormalItemView(parent.getContext());
                TextView textView = view.findViewById(R.id.normal_item_tv);
                textView.setText(testBeanList.get(position).getText());
                view.setSelected(testBeanList.get(position).isSelected());
                Log.i(TAG, "onCreateItemHolder position:" + position);
//                if (position == 2) {
//                    view.setVisibility(View.GONE);
//                }
                return new ItemHolder(view);

            }

            @Override
            public int getItemCount() {
                return testBeanList.size();
            }
        };
        viewBinding.flexTabLayout.setAdapter(adapter);
        viewBinding.flexTabLayout.setLayoutManager(new FlexHorizontalLayoutManager());
        viewBinding.buttonJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RecyclerViewActivity.class));
            }
        });

        viewBinding.buttonAddOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testBeanList.add(new TestBean(false, "" + testBeanList.size()));
                adapter.notifyItemInserted(testBeanList.size());
            }
        });

        viewBinding.buttonAddTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 2; i++) {
                    testBeanList.add(new TestBean(false, "" + testBeanList.size()));
                    adapter.notifyItemInserted(testBeanList.size());
                }
            }
        });

        viewBinding.buttonRemoveOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int removedIndex = testBeanList.size() - 1;
                if (removedIndex < 0) return;
                testBeanList.remove(removedIndex);
                adapter.notifyItemRemoved(removedIndex);
            }
        });
        viewBinding.buttonRemoveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = testBeanList.size();
                testBeanList.clear();
                adapter.notifyItemRangeRemoved(0, size);
            }
        });
    }


    class ItemHolder extends FlexTabLayout.FlexItemHolder {
        private boolean selected;

        public ItemHolder(@NonNull View itemView, boolean selected) {
            super(itemView);
            this.selected = selected;
        }


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

    private class TestBean {
        private boolean selected = false;
        private String text;

        public TestBean(boolean selected, String text) {
            this.selected = selected;
            this.text = text;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
