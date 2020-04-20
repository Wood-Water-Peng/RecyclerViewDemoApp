package com.example.recyclerviewdemoapp;

import android.content.Context;
import android.database.Observable;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

import static android.view.View.MeasureSpec.AT_MOST;

/**
 * 1.宽高均支持wrap_content
 * 2.当child的总宽高超过parent拿到的最大限定值时，使用该限定值(超出范围的child将不可见)
 * 3.不支持滑动
 * 4.兼容parent的padding和child的margin
 * <p>
 * 扩展计划：
 * <p>
 * 1.完善api接口
 * 2.增加insert的delete的动画效果
 * 3.优化逻辑，提高速度
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
    /**
     * Handles adapter updates
     */
    AdapterHelper mAdapterHelper;
    private boolean mItemsAddedOrRemoved;
    final State mState = new State();

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
        this(context, null);
    }

    public FlexTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlexTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAdapterManager();
        initChildrenHelper();
    }

    /**
     * Returns the position in the group of the specified child view.
     *
     * @return a positive integer representing the position of the view in the
     * group, or -1 if the view does not exist in the group
     */
    private void initChildrenHelper() {
        mChildHelper = new ChildHelper(new ChildHelper.Callback() {
            @Override
            public int getChildCount() {
                return FlexTabLayout.this.getChildCount();
            }

            @Override
            public void addView(View child, int index) {
                FlexTabLayout.this.addView(child, index);
            }

            @Override
            public int indexOfChild(View view) {
                return FlexTabLayout.this.indexOfChild(view);
            }

            @Override
            public void removeViewAt(int index) {
                //清除动画
                FlexTabLayout.this.removeViewAt(index);
            }

            @Override
            public View getChildAt(int index) {
                return FlexTabLayout.this.getChildAt(index);
            }

            @Override
            public void removeAllViews() {
                //清除动画
                FlexTabLayout.this.removeAllViews();
            }

            @Override
            public void onEnteredHiddenState(View child) {

            }

            @Override
            public void onLeftHiddenState(View child) {

            }
        });
    }

    static FlexTabLayout.FlexItemHolder getChildViewHolderInt(View child) {
        if (child == null) {
            return null;
        }
        return ((FlexTabLayout.LayoutParams) child.getLayoutParams()).mViewHolder;
    }

    ChildHelper mChildHelper;

    void offsetPositionRecordsForInsert(int positionStart, int itemCount) {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final FlexTabLayout.FlexItemHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null && holder.mPosition >= positionStart) {
                holder.offsetPosition(itemCount, false);
            }
        }

//        requestLayout();
    }

    private void initAdapterManager() {
        mAdapterHelper = new AdapterHelper(new AdapterHelper.Callback() {
            @Override
            public void offsetPositionsForAdd(int positionStart, int itemCount) {
                offsetPositionRecordsForInsert(positionStart, itemCount);
                mItemsAddedOrRemoved = true;
            }
        });
    }

    //设置自己的测量尺寸和模式
    void setMeasureSpecs(int wSpec, int hSpec) {
        mWidthSpec = wSpec;
        mHeightSpec = hSpec;

        mWidth = MeasureSpec.getSize(wSpec);
        mWidthMode = MeasureSpec.getMode(wSpec);

        mHeight = MeasureSpec.getSize(hSpec);
        mHeightMode = MeasureSpec.getMode(hSpec);

    }

    /**
     * An implementation of {@link View#onMeasure(int, int)} to fall back to in various scenarios
     * where this RecyclerView is otherwise lacking better information.
     */
    void defaultOnMeasure(int widthSpec, int heightSpec) {
        // calling LayoutManager here is not pretty but that API is already public and it is better
        // than creating another method since this is internal.
        final int width = RecyclerView.LayoutManager.chooseSize(widthSpec,
                getPaddingLeft() + getPaddingRight(),
                ViewCompat.getMinimumWidth(this));
        final int height = RecyclerView.LayoutManager.chooseSize(heightSpec,
                getPaddingTop() + getPaddingBottom(),
                ViewCompat.getMinimumHeight(this));

        setMeasuredDimension(width, height);
    }


    /**
     * @param widthMeasureSpec
     * @param heightMeasureSpec 1.高度如何支持wrap_content？
     *                          在测量阶段，我们需要确定出parent的高度，所以必须模拟布局操作，然后根据child的数量和布局结果确定出parent的高度
     *                          优化点：可以在测量阶段记录child的位置，然后在布局阶段直接使用这些位置，可以避免冗余的计算流程
     */


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        //先测量一下自己
        defaultOnMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasureSpecs(widthMeasureSpec, heightMeasureSpec);
        final boolean measureSpecModeIsExactly =
                mWidthMode == MeasureSpec.EXACTLY && mHeightMode == MeasureSpec.EXACTLY;
        if (measureSpecModeIsExactly || mAdapter == null) {
            return;
        }
        //测量并布局child
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width = widthSize;
        int height = heightSize;
        int itemCount = mAdapter == null ? 0 : mAdapter.getItemCount();
        int mTotalWidth = 0;
        int lastRowMaxHeight = 0; //当前行的最大高度
        int totalRowHeight = 0;//所有的行高度，每行最高的总和
        for (int i = 0; i < itemCount; i++) {
            View child = childViewManager.getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
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
        if (mWidthMode == AT_MOST) {
            //重新定义parent的宽度
            if (mTotalWidth < widthSize - getPaddingLeft() - getPaddingRight()) {
                //只有一行
                width = mTotalWidth + getPaddingRight() + getPaddingLeft();
                totalRowHeight = lastRowMaxHeight;
            } else {
                //多行，此时parent的宽度为其限制的最大宽度
            }

        }

        //此时parent的宽度已经确定，模拟布局
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

        //parent的高度为wrap_content,需要通过模拟layout来确定他的最大高度
        if (mHeightMode == AT_MOST) {
            //重新定义parent的高度
            //此时child需要占用的总宽度已经超过了父类最大的限制宽度，那么需要确定行数
            height = totalRowHeight + getPaddingTop() + getPaddingBottom();
        }


        //在此之前，已经完成了对child的测量和布局
        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
        }
        dispatchLayoutStep2();
        setMeasuredDimensionFromChildren(widthMeasureSpec, heightMeasureSpec);
//        setMeasuredDimension(width, height);
        if (shouldMeasureTwice()) {

        }

    }

    final Rect mTempRect = new Rect();

    /**
     * @param widthSpec  parent
     * @param heightSpec parent
     *
     * parent的测量宽度不能超过widthSpec的最大值
     */
    void setMeasuredDimensionFromChildren(int widthSpec, int heightSpec) {
        final int count = getChildCount();
        if (count == 0) {
            defaultOnMeasure(widthSpec, heightSpec);
            return;
        }


        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            final Rect bounds = mTempRect;
            getDecoratedBoundsWithMarginsInt(child, bounds);
        }
    }

    static void getDecoratedBoundsWithMarginsInt(View view, Rect outBounds) {
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        outBounds.set(view.getLeft() - lp.leftMargin,
                view.getTop() - lp.topMargin,
                view.getRight() + lp.rightMargin,
                view.getBottom() + lp.bottomMargin);
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

    boolean shouldMeasureTwice() {
        return getHeightMode() != View.MeasureSpec.EXACTLY
                && getWidthMode() != View.MeasureSpec.EXACTLY
                && hasFlexibleChild();
    }

    boolean hasFlexibleChild() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp.width < 0 && lp.height < 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = mAdapter == null ? 0 : mAdapter.getItemCount();
        int rowIndex = 0;  //摆放的行数
        Map<Integer, Integer> maxHeightInRow = new HashMap<>(); //一行中最大的高度
        int width = getMeasuredWidth();
        //行最大可用宽度
        int rowMaxAvailableSpace = width - getPaddingLeft() - getPaddingRight();
        int availableSpace = rowMaxAvailableSpace;
        int childLeft = getPaddingLeft();  //下一个child的左起点
        int childTop = getPaddingTop();  //下一个child的top
        for (int i = 0; i < childCount; i++) {
            final View child = childViewManager.getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                if (!child.isAttachedToWindow()) {
                    addView(child, i);
                }
                continue;
            }
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
            }
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            if (!child.isAttachedToWindow()) {
                addView(child, i);
            }
            childLeft += lp.rightMargin + childWidth;
            availableSpace -= childNeedWidth;
        }
        mFirstLayoutComplete = true;
    }


    /**
     * The first step of a layout where we;
     * - process adapter updates
     */
    private void dispatchLayoutStep1() {
        mState.assertLayoutStep(State.STEP_START);
        mState.mIsMeasuring = false;
        mState.mItemCount = mAdapter.getItemCount();
        mState.mLayoutStep = State.STEP_LAYOUT;
    }

    /**
     * The second layout step where we do the actual layout of the views for the final state.
     * This step might be run multiple times if necessary (e.g. measure).
     * <p>
     * 真正对child进行测量和布局
     */
    private void dispatchLayoutStep2() {
        mState.assertLayoutStep(State.STEP_LAYOUT);
        mState.mItemCount = mAdapter.getItemCount();
        onLayoutChildren();
    }

    /**
     * 1.获取一个child---暂时不用缓存
     * <p>
     * 此时child的尺寸已经测量出来，而parent的布局不一定确定，则根据parent的measureSpec的值进行布局
     */
    private void onLayoutChildren() {
        //step1:获取一个child
        for (int i = 0; i < mState.mItemCount; i++) {
            View view = getChildViewManager().getChildAt(i);
            int usedWidth;
            int usedHeight;
            int rowMaxHeight;
            int left, top, right, bottom;
            measureChildWithMargins(view);
            Log.i(TAG, "onLayoutChildren_" + i + "---width:" + view.getMeasuredWidth() + "_height:" + view.getMeasuredHeight());


//            layoutChildWithMargins(view, left, top, right, bottom);
        }
    }

    public void layoutChildWithMargins(@NonNull View child, int left, int top, int right,
                                       int bottom) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final Rect insets = lp.mDividerInsets;
        child.layout(left + insets.left + lp.leftMargin, top + insets.top + lp.topMargin,
                right - insets.right - lp.rightMargin,
                bottom - insets.bottom - lp.bottomMargin);
    }

    private int mWidthMode, mHeightMode;
    private int mWidth, mHeight;
    private int mWidthSpec;
    private int mHeightSpec;

    public int getWidthMode() {
        return mWidthMode;
    }

    public int getHeightMode() {
        return mHeightMode;
    }


    public void measureChildWithMargins(@NonNull View child) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int childWidthSpec = getChildMeasureSpec(mWidthSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
        int childHeightSpec = getChildMeasureSpec(mHeightSpec, getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin, lp.height);
        child.measure(childWidthSpec, childHeightSpec);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        FlexTabLayout.FlexItemHolder mViewHolder;
        boolean mInsetsDirty = true;
        final Rect mDividerInsets = new Rect();

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

        /**
         * Returns true if the view this LayoutParams is attached to needs to have its content
         * updated from the corresponding adapter.
         *
         * @return true if the view should have its content updated
         */
        public boolean viewNeedsUpdate() {
            return mViewHolder.needsUpdate();
        }
    }

    public abstract static class Adapter<FH extends FlexTabLayout.FlexItemHolder> {
        private final FlexTabLayout.AdapterDataObservable mObservable = new FlexTabLayout.AdapterDataObservable();

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public final void notifyItemInserted(int position) {
            mObservable.notifyItemRangeInserted(position, 1);
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

        /**
         * The data this ViewHolder's view reflects is stale and needs to be rebound
         * by the adapter. mPosition and mItemId are consistent.
         */
        static final int FLAG_UPDATE = 1 << 1;

        int mFlags;
        @NonNull
        public final View itemView;
        int mPosition = NO_POSITION;
        int mOldPosition = NO_POSITION;
        int mPreLayoutPosition = NO_POSITION;
        long mItemId = NO_ID;

        protected FlexItemHolder(@NonNull View itemView) {
            this.itemView = itemView;
        }

        boolean needsUpdate() {
            return (mFlags & FLAG_UPDATE) != 0;
        }

        void offsetPosition(int offset, boolean applyToPreLayout) {
            if (mOldPosition == NO_POSITION) {
                mOldPosition = mPosition;
            }
            if (mPreLayoutPosition == NO_POSITION) {
                mPreLayoutPosition = mPosition;
            }
            if (applyToPreLayout) {
                mPreLayoutPosition += offset;
            }
            mPosition += offset;
            if (itemView.getLayoutParams() != null) {
                ((FlexTabLayout.LayoutParams) itemView.getLayoutParams()).mInsetsDirty = true;
            }
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

        public void notifyItemRangeInserted(int positionStart, int itemCount) {
            // since onItemRangeInserted() is implemented by the app, it could do anything,
            // including removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeInserted(positionStart, itemCount);
            }
        }

    }


    public abstract static class AdapterDataObserver {
        public void onChanged() {
            // Do nothing
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
        }
    }

    private class FlexTabViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mAdapterHelper.onItemRangeChanged(positionStart, itemCount, new Object())) {
                triggerUpdateProcessor();
            }
        }
    }

    private void triggerUpdateProcessor() {
        if (mIsAttached) {
            ViewCompat.postOnAnimation(FlexTabLayout.this, mUpdateChildViewsRunnable);
        }
    }

    boolean mFirstLayoutComplete;
    boolean mIsAttached;
    /**
     * Note: this Runnable is only ever posted if:
     * 1) We've been through first layout
     * 2) We know we have a fixed size (mHasFixedSize)
     * 3) We're attached
     */
    final Runnable mUpdateChildViewsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mFirstLayoutComplete || isLayoutRequested()) {
                // a layout request will happen, we should not do layout here.
                return;
            }
            if (!mIsAttached) {
                requestLayout();
                // if we are not attached yet, mark us as requiring layout and skip
                return;
            }
            consumePendingUpdateOperations();
        }
    };

    private void consumePendingUpdateOperations() {
        if (!mAdapterHelper.hasPendingUpdates()) {
            return;
        }
        mAdapterHelper.preProcess();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        mFirstLayoutComplete = mFirstLayoutComplete && !isLayoutRequested();
    }

    public static class State {
        static final int STEP_START = 1;
        static final int STEP_LAYOUT = 1 << 1;

        boolean mIsMeasuring = false;

        /**
         * Number of items adapter has.
         */
        int mItemCount = 0;

        @IntDef(flag = true, value = {
                STEP_START, STEP_LAYOUT
        })
        @interface LayoutState {
        }

        @LayoutState
        int mLayoutStep = STEP_START;

        void assertLayoutStep(int accepted) {
            if ((accepted & mLayoutStep) == 0) {
                throw new IllegalStateException("Layout state should be one of "
                        + Integer.toBinaryString(accepted) + " but it is "
                        + Integer.toBinaryString(mLayoutStep));
            }
        }
    }
}
