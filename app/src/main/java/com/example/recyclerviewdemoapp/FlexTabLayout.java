package com.example.recyclerviewdemoapp;

import android.content.Context;
import android.database.Observable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.MeasureSpec.AT_MOST;

/**
 * 1.宽高均支持wrap_content
 * 2.当child的总宽高超过parent拿到的最大限定值时，使用该限定值(超出范围的child将不可见)
 * 3.不支持滑动
 * 4.兼容parent的padding和child的margin
 */

public class FlexTabLayout extends ViewGroup {
    public static final String TAG = "FlexTabLayout";
    public static final int NO_POSITION = -1;
    public static final long NO_ID = -1;
    final ChildViewManager childViewManager = new ChildViewManager();
    FlexTabLayout.Adapter mAdapter;
    private final FlexTabViewDataObserver mObserver = new FlexTabViewDataObserver();
    //横向间距，不包括头尾
    private static int HORIZONTAL_SPACE = 100;
    //纵向间距，不包括头尾
    private static int VERTICAL_SPACE = 1;

    public Adapter getAdapter() {
        return mAdapter;
    }

    public ChildViewManager getChildViewManager() {
        return childViewManager;
    }

    public void setAdapter(Adapter adapter) {
        if (adapter != null) {
            this.mAdapter = adapter;
            mAdapter.registerAdapterDataObserver(mObserver);
        }
    }

    public FlexTabLayout(Context context) {
        super(context);
    }

    public FlexTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlexTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * @param widthMeasureSpec
     * @param heightMeasureSpec 1.高度如何支持wrap_content？
     *                          在测量阶段，我们需要确定出parent的高度，所以必须模拟布局操作，然后根据child的数量和布局结果确定出parent的高度
     *                          优化点：可以在测量阶段记录child的位置，然后在布局阶段直接使用这些位置，可以避免冗余的计算流程
     */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width = widthSize;
        int height = heightSize;
        int itemCount = mAdapter == null ? 0 : mAdapter.getItemCount();
        int largestChildHeight = Integer.MIN_VALUE;
        int mTotalWidth = 0;
        int mTotalHeight = 0;
        int lastRowMaxHeight = 0; //当前行的最大高度
        int totalRowHeight = 0;//所有的行高度，每行最高的总和
        for (int i = 0; i < itemCount; i++) {
            View child = childViewManager.getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            measureChildWithMargins(child, widthMeasureSpec, 0,
                    heightMeasureSpec, 0);
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();
            lastRowMaxHeight = Math.max(childHeight + params.topMargin + params.bottomMargin, lastRowMaxHeight);
            if (i == 0) {
                //每一行的第一个child忽略HORIZONTAL_SPACE
                mTotalWidth += childWidth + params.leftMargin + params.rightMargin;
            } else {
                mTotalWidth += childWidth + params.leftMargin + params.rightMargin + HORIZONTAL_SPACE;
            }

        }
        //1.测量宽度
        //2.确定高度
        if (widthMode == AT_MOST) {
            //重新定义parent的宽度
            if (mTotalWidth < widthSize - getPaddingLeft() - getPaddingRight()) {
                //只有一行
                width = mTotalWidth + getPaddingRight() + getPaddingLeft();
                totalRowHeight = lastRowMaxHeight;
            }

        }

        if (heightMode == AT_MOST) {
            //重新定义parent的高度
            //此时child需要占用的总宽度已经超过了父类最大的限制宽度，那么需要确定行数
            int rowMaxWidth = widthSize - getPaddingLeft() - getPaddingRight();
            int availableWidth = rowMaxWidth;
            for (int i = 0; i < itemCount; i++) {
                View child = childViewManager.getChildAt(i);
                int childHeight = child.getMeasuredHeight();
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                int childNeedWidth = child.getMeasuredWidth() + params.leftMargin + params.rightMargin + (i == 0 ? 0 : HORIZONTAL_SPACE);
                //计算出行高
                lastRowMaxHeight = Math.max(childHeight + params.topMargin + params.bottomMargin, lastRowMaxHeight);

                if (childNeedWidth > availableWidth) {
                    //换行    第一行忽略VERTICAL_SPACE
                    totalRowHeight += (lastRowMaxHeight + VERTICAL_SPACE);
                    availableWidth = rowMaxWidth;
                    childNeedWidth = child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
                }
                availableWidth -= childNeedWidth;
            }
            totalRowHeight += lastRowMaxHeight;
            height = totalRowHeight + getPaddingTop() + getPaddingBottom();
        }


        setMeasuredDimension(width, height);

    }

    public final class ChildViewManager {
        private Map<Integer, View> childList = new HashMap<>();

        public View getChildAt(final int position) {
            if (mAdapter == null) {
                throw new IllegalStateException("Adapter can not be null");
            }
            View child = childList.get(position);
            if (child == null) {
                child = createNewView(position);
                childList.put(position, child);
            }
            child.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mAdapter.onItemClicked(childList.get(position), position);
                }
            });
            return child;
        }

        private View createNewView(final int position) {
            final View child = mAdapter.onCreateItemHolder(FlexTabLayout.this, position).itemView;
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp == null) {
                LayoutParams layoutParams = (LayoutParams) generateDefaultLayoutParams();
                child.setLayoutParams(layoutParams);
            } else {
                child.setLayoutParams(generateLayoutParams(lp));
            }

            return child;
        }

        public void clear() {
            childList.clear();
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    protected void measureChildWithMargins(View child,
                                           int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingLeft() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingBottom() + getPaddingTop() + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = mAdapter == null ? 0 : mAdapter.getItemCount();
        int rowIndex = 0;  //摆放的行数
        Map<Integer, Integer> maxHeightInRow = new HashMap<>(); //一行中最大的高度
        int nextRowStart = 0;//下一行的摆放起点
        int width = getMeasuredWidth();
        int usedHeight = 0; //布局child已经使用的高度
        //行最大可用宽度
        int rowMaxAvailableSpace = width - getPaddingLeft() - getPaddingRight();
        int availableSpace = rowMaxAvailableSpace;
        int childLeft = getPaddingLeft();  //下一个child的左起点
        int childTop = getPaddingTop();  //下一个child的top
        for (int i = 0; i < childCount; i++) {
            final View child = childViewManager.getChildAt(i);
            int childHeight = child.getMeasuredHeight();
            int childWidth = child.getMeasuredWidth();
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            //第一行的第一个child不需要考虑HORIZONTAL_SPACE
            if (availableSpace == rowMaxAvailableSpace) {
                childLeft += lp.leftMargin;
            } else {
                childLeft += lp.leftMargin + HORIZONTAL_SPACE;
            }
            //计算出每一行的最大高度
            Integer maxHeight = maxHeightInRow.get(rowIndex);
            if (maxHeight == null) {
                maxHeightInRow.put(rowIndex, childHeight);
            } else {
                maxHeightInRow.put(rowIndex, Math.max(childHeight, maxHeight));
            }
            //布局该child需要的宽度
            int childNeedWidth;
            if (availableSpace == rowMaxAvailableSpace) {
                childNeedWidth = childWidth + lp.leftMargin + lp.rightMargin;
            } else {
                childNeedWidth = childWidth + lp.leftMargin + lp.rightMargin + HORIZONTAL_SPACE;
            }
            //当前行剩余的空间是否放的下这个child,否则执行换行操作
            if (childNeedWidth > availableSpace) {
                childTop += maxHeightInRow.get(rowIndex) + VERTICAL_SPACE;
                rowIndex++;
                childNeedWidth = childWidth + lp.leftMargin + lp.rightMargin;
                availableSpace = rowMaxAvailableSpace;
                childLeft = getPaddingLeft();
                usedHeight += childTop;
                Log.i(TAG, "onLayout left:" + childLeft + "---childTop:" + childTop + "---position:" + i);
            }
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            if (!child.isAttachedToWindow()) {
                addView(child, i);
            }
            childLeft += lp.rightMargin + childWidth;
            availableSpace -= childNeedWidth;
        }

    }


    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public abstract static class Adapter<FH extends FlexTabLayout.FlexItemHolder> {
        private final FlexTabLayout.AdapterDataObservable mObservable = new FlexTabLayout.AdapterDataObservable();

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public abstract void onItemClicked(View itemView, int position);

        @NonNull
        public abstract FH onCreateItemHolder(@NonNull ViewGroup parent, int position);

        public abstract int getItemCount();

        public void registerAdapterDataObserver(FlexTabViewDataObserver observer) {
            mObservable.registerObserver(observer);
        }
    }

    public abstract static class FlexItemHolder {
        @NonNull
        public final View itemView;
        int mPosition = NO_POSITION;
        int mOldPosition = NO_POSITION;
        long mItemId = NO_ID;

        protected FlexItemHolder(@NonNull View itemView) {
            this.itemView = itemView;
        }
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        public void notifyChanged() {
            // since onChanged() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }
    }


    public abstract static class AdapterDataObserver {
        public void onChanged() {
            // Do nothing
        }
    }

    private class FlexTabViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }
    }
}
