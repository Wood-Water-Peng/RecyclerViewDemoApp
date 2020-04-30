package com.example.recyclerviewdemoapp;

import android.content.Context;
import android.database.Observable;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    FlexTabLayout.Adapter mAdapter;
    private final FlexTabViewDataObserver mObserver = new FlexTabViewDataObserver();
    //横向间距，不包括头尾
    private static int HORIZONTAL_SPACE = 0;
    //纵向间距，不包括头尾
    private static int VERTICAL_SPACE = 0;

    private int mInterceptRequestLayoutDepth;
    private boolean mLayoutWasDefered = false;
    private int mLayoutOrScrollCounter = 0;
    private ItemAnimator.ItemAnimatorListener mItemAnimatorListener =
            new ItemAnimatorRestoreListener();
    LayoutManager mLayout;
    final FlexHolderRecycler mRecycler = new FlexHolderRecycler();

    /**
     * Handles adapter updates
     */
    AdapterHelper mAdapterHelper;
    private boolean mItemsAddedOrRemoved;
    final State mState = new State();

    public Adapter getAdapter() {
        return mAdapter;
    }


    ItemAnimator mItemAnimator = new DefaultItemAnimator();

    private Runnable mItemAnimatorRunner = new Runnable() {
        @Override
        public void run() {
            if (mItemAnimator != null) {
                mItemAnimator.runPendingAnimations();
            }
            mPostedAnimatorRunner = false;
        }
    };

    public void setAdapter(Adapter adapter) {
        if (adapter != null) {
            this.mAdapter = adapter;
            mAdapter.registerAdapterDataObserver(mObserver);
            requestLayout();
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
        mItemAnimator.setListener(mItemAnimatorListener);
    }

    boolean mPostedAnimatorRunner = false;

    private class ItemAnimatorRestoreListener implements ItemAnimator.ItemAnimatorListener {

        ItemAnimatorRestoreListener() {
        }

        @Override
        public void onAnimationFinished(FlexItemHolder item) {
            Log.i(TAG, "onAnimationFinished index:" + item.mPosition + "---childCount:" + mChildHelper.getChildCount());
//            removeAnimatingView(item.itemView);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mItemAnimatorRunner);
    }

    void postAnimationRunner() {
        if (!mPostedAnimatorRunner && mIsAttached) {
            ViewCompat.postOnAnimation(this, mItemAnimatorRunner);
            mPostedAnimatorRunner = true;
        }
    }

    void animateAppearance(@NonNull FlexItemHolder itemHolder,
                           @Nullable ItemAnimator.ItemHolderInfo preLayoutInfo, @NonNull ItemAnimator.ItemHolderInfo postLayoutInfo) {
        itemHolder.setIsRecyclable(false);
        if (mItemAnimator.animateAppearance(itemHolder, preLayoutInfo, postLayoutInfo)) {
            postAnimationRunner();
        }
    }

    void animateDisappearance(@NonNull FlexItemHolder itemHolder,
                              @Nullable ItemAnimator.ItemHolderInfo preLayoutInfo, @NonNull ItemAnimator.ItemHolderInfo postLayoutInfo) {
        Log.i(TAG, "animateDisappearance index:" + itemHolder.mPosition);
//        addAnimatingView(itemHolder);
        if (mItemAnimator.animateDisappearance(itemHolder, preLayoutInfo, postLayoutInfo)) {
            postAnimationRunner();
        }
    }

    @Override
    public void requestLayout() {
        if (mInterceptRequestLayoutDepth == 0) {
            super.requestLayout();
        } else {
            mLayoutWasDefered = true;
        }
    }

    void startInterceptRequestLayout() {
        mInterceptRequestLayoutDepth++;
        mLayoutWasDefered = true;
    }

    void stopInterceptRequestLayout() {
        if (mInterceptRequestLayoutDepth > 0) {
            mInterceptRequestLayoutDepth--;
        }
        if (mInterceptRequestLayoutDepth == 0) {
            mLayoutWasDefered = false;
        }
    }

    public void setLayoutManager(@Nullable FlexTabLayout.LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }
        mLayout = layout;
        if (layout != null) {
            mLayout.setFlexTabLayout(this);
        }
        requestLayout();
    }

    /**
     * The callback to convert view info diffs into animations.
     */
    private final ViewInfoStore.ProcessCallback mViewInfoProcessCallback =
            new ViewInfoStore.ProcessCallback() {

                @Override
                public void processDisappeared(FlexItemHolder viewHolder, @NonNull ItemAnimator.ItemHolderInfo preInfo, @Nullable ItemAnimator.ItemHolderInfo postInfo) {
                    animateDisappearance(viewHolder, preInfo, postInfo);
                }

                @Override
                public void processAppeared(FlexItemHolder viewHolder, @Nullable ItemAnimator.ItemHolderInfo preInfo, ItemAnimator.ItemHolderInfo postInfo) {
                    animateAppearance(viewHolder, preInfo, postInfo);
                }

                @Override
                public void processPersistent(FlexItemHolder viewHolder, ItemAnimator.ItemHolderInfo preInfo, ItemAnimator.ItemHolderInfo postInfo) {
                    if (mItemAnimator.animatePersistence(viewHolder, preInfo, postInfo)) {
                        postAnimationRunner();
                    }
                }
            };

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
                if (child.getParent() == null) {
                    FlexTabLayout.this.addView(child, index);

                }
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
                View view = FlexTabLayout.this.getChildAt(index);
                LayoutParams params = (LayoutParams) view.getLayoutParams();
                Log.i(TAG, "getChildAt index:" + params.mViewHolder.mPosition);
                return view;
            }

            @Override
            public void removeAllViews() {
                //清除动画
//                FlexTabLayout.this.removeAllViews();
            }

            @Override
            public void onEnteredHiddenState(View child) {

            }

            @Override
            public void onLeftHiddenState(View child) {

            }

            @Override
            public void detachViewFromParent(int index) {
                Log.i(TAG, "detachViewFromParent index:" + index);
                final View view = getChildAt(index);
                if (view != null) {
                    final FlexTabLayout.FlexItemHolder vh = getChildViewHolderInt(view);
                    if (vh != null) {
                        if (vh.isTmpDetached()) {
                            throw new IllegalArgumentException("called detach on an already"
                                    + " detached child ");
                        }
                        vh.addFlags(FlexTabLayout.FlexItemHolder.FLAG_TMP_DETACHED);
                    }
                }
                FlexTabLayout.this.detachViewFromParent(index);
            }

            @Override
            public void attachViewToParent(View child, int index, ViewGroup.LayoutParams layoutParams) {
                Log.i(TAG, "attachViewToParent index:" + index);
                final FlexItemHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    if (!vh.isTmpDetached()) {
                        throw new IllegalArgumentException("Called attach on a child which is not"
                                + " detached: ");
                    }
                    vh.clearTmpDetachFlag();
                }
                FlexTabLayout.this.attachViewToParent(child, index, layoutParams);
            }
        });
    }

    static FlexTabLayout.FlexItemHolder getChildViewHolderInt(View child) {
        if (child == null) {
            return null;
        }
        Log.d(TAG, "getChildViewHolderInt-------");
        FlexItemHolder mViewHolder = ((LayoutParams) child.getLayoutParams()).mViewHolder;
        return mViewHolder;
    }

    ChildHelper mChildHelper;

    /**
     * @param positionStart
     * @param itemCount     当有新数据插入后，插入位置之后的position位置都要+1
     */
    void offsetPositionRecordsForInsert(int positionStart, int itemCount) {
        final int childCount = mChildHelper.getUnfilteredChildCount();
        for (int i = 0; i < childCount; i++) {
            final FlexTabLayout.FlexItemHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            Log.i(TAG, "offsetPositionRecordsForInsert startPosition:" + holder.mPosition);
            if (holder != null && holder.mPosition >= positionStart) {
                holder.offsetPosition(itemCount, false);
            }
            Log.i(TAG, "offsetPositionRecordsForInsert endPosition:" + holder.mPosition);
        }

        requestLayout();
    }

    void offsetPositionRecordsForRemove(int positionStart, int itemCount, boolean applyToPreLayout) {
        final int positionEnd = positionStart + itemCount;
        final int childCount = mChildHelper.getUnfilteredChildCount();
        Log.i(TAG, "offsetPositionRecordsForRemove childCount:" + childCount + "---positionStart：" + positionStart);
        for (int i = 0; i < childCount; i++) {
            final FlexItemHolder holder = getChildViewHolderInt(mChildHelper.getUnfilteredChildAt(i));
            if (holder != null) {
                if (holder.mPosition >= positionEnd) {
//                    holder.offsetPosition(-itemCount, applyToPreLayout);
//                    mState.mStructureChanged = true;
                } else if (holder.mPosition >= positionStart) {
                    holder.flagRemovedAndOffsetPosition(positionStart - 1, -itemCount,
                            applyToPreLayout);
                    mState.mStructureChanged = true;
                }
            }
        }
        requestLayout();

    }


    private void addAnimatingView(FlexItemHolder viewHolder) {
        Log.i(TAG, "addAnimatingView index:" + viewHolder.mPosition);
        final View view = viewHolder.itemView;
        final boolean alreadyParented = view.getParent() == this;
        if (viewHolder.isTmpDetached()) {
            // re-attach
            mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);
        } else if (!alreadyParented) {
            mChildHelper.addView(view, true);
        }
    }

    boolean removeAnimatingView(View view) {
        Log.i(TAG, "removeAnimatingView index:");
        startInterceptRequestLayout();
        final boolean removed = mChildHelper.removeViewIfHidden(view);
        if (removed) {
            return true;
        }
        // only clear request eaten flag if we removed the view.
        stopInterceptRequestLayout();
        return false;
    }

    private void initAdapterManager() {
        mAdapterHelper = new AdapterHelper(new AdapterHelper.Callback() {
            @Override
            public void offsetPositionsForAdd(int positionStart, int itemCount) {
                offsetPositionRecordsForInsert(positionStart, itemCount);
                mItemsAddedOrRemoved = true;
            }

            @Override
            public void offsetPositionsForRemovingLaidOutOrNewView(int positionStart, int itemCount) {
                Log.i(TAG, "offsetPositionsForRemovingLaidOutOrNewView positionStart:" + positionStart);
                offsetPositionRecordsForRemove(positionStart, itemCount, false);
                // should we create mItemsMoved ?
                mItemsAddedOrRemoved = true;
            }
        });
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
        Log.i(TAG, "onMeasure-----");
        if (mAdapter == null || mLayout == null) {
            //先测量一下自己
            defaultOnMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        defaultOnMeasure(widthMeasureSpec, heightMeasureSpec);
        final boolean measureSpecModeIsExactly =
                widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY;
        if (measureSpecModeIsExactly || mAdapter == null) {
            //此时flexTabLayout的宽高都是固定值，与child无关，可以直接退出测量过程
            mLayout.setMeasureSpecs(widthMeasureSpec, heightMeasureSpec);
//            defaultOnMeasure(widthMeasureSpec, heightMeasureSpec);
//            return;
        }
        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
        }
        mLayout.setMeasureSpecs(widthMeasureSpec, heightMeasureSpec);
        mState.mIsMeasuring = true;
        //对child执行测量
        dispatchLayoutStep2();
        mLayout.setMeasuredDimensionFromChildren();
    }


    static void getDecoratedBoundsWithMarginsInt(View view, Rect outBounds) {
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        outBounds.set(view.getLeft() - lp.leftMargin,
                view.getTop() - lp.topMargin,
                view.getRight() + lp.rightMargin,
                view.getBottom() + lp.bottomMargin);
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
                                           int parentHeightMeasureSpec, int totalHeight) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingLeft() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingBottom() + getPaddingTop() + lp.topMargin + lp.bottomMargin
                        + totalHeight, lp.height);

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
        dispatchLayout();
        mFirstLayoutComplete = true;
        Log.i(TAG, "onLayout-------------");
    }

    private void dispatchLayout() {
        if (mAdapter == null) {
            Log.e(TAG, "No adapter attached; skipping layout");
            // leave the state in START
            return;
        }
        if (mLayout == null) {
            Log.e(TAG, "No layout manager attached; skipping layout");
            // leave the state in START
            return;
        }
        mState.mIsMeasuring = false;
        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
            dispatchLayoutStep2();
        }
        dispatchLayoutStep3();
    }

    /**
     * True after an event occurs that signals that the entire data set has changed. In that case,
     * we cannot run any animations since we don't know what happened until layout.
     * <p>
     * Attached items are invalid until next layout, at which point layout will animate/replace
     * items as necessary, building up content from the (effectively) new adapter from scratch.
     * <p>
     * Cached items must be discarded when setting this to true, so that the cache may be freely
     * used by prefetching until the next layout occurs.
     */
    boolean mDataSetHasChangedAfterLayout = false;

    /**
     * Consumes adapter updates and calculates which type of animations we want to run.
     * Called in onMeasure and dispatchLayout.
     * <p>
     * This method may process only the pre-layout state of updates or all of them.
     */
    private void processAdapterUpdatesAndSetAnimationFlags() {

        if (mDataSetHasChangedAfterLayout) {
            mAdapterHelper.reset();
        }
        consumePendingUpdateOperations();
        mState.mRunSimpleAnimations = mFirstLayoutComplete
                && mItemAnimator != null
                && !mDataSetHasChangedAfterLayout;

        mState.mRunPredictiveAnimations = mState.mRunSimpleAnimations;
    }

    /**
     * The first step of a layout where we;
     * - process adapter updates
     */
    private void dispatchLayoutStep1() {
        mState.assertLayoutStep(State.STEP_START);
        mState.mIsMeasuring = false;
        startInterceptRequestLayout();
        mViewInfoStore.clear();
        onEnterLayoutOrScroll();
        processAdapterUpdatesAndSetAnimationFlags();
        mState.mItemCount = mAdapter.getItemCount();
        mState.mInPreLayout = mState.mRunPredictiveAnimations;
        mState.mLayoutStep = State.STEP_LAYOUT;
        if (mState.mRunSimpleAnimations) {
            // Step 0: Find out where all non-removed items are, pre-layout
            int count = mChildHelper.getChildCount();
            for (int i = 0; i < count; ++i) {
                final FlexItemHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
                Log.d(TAG, "dispatchLayoutStep1-----addToPreLayout index:" + holder.mPosition);
                final ItemAnimator.ItemHolderInfo animationInfo = mItemAnimator
                        .recordPreLayoutInformation(mState, holder,
                                0, new Object());
                mViewInfoStore.addToPreLayout(holder, animationInfo);
            }
        }

        if (mState.mRunPredictiveAnimations) {
            mLayout.onLayoutChildren(this, mState);
            clearOldPositions();
        }
        onExitLayoutOrScroll();
        stopInterceptRequestLayout();
    }

    private void clearOldPositions() {
        mRecycler.clearOldPositions();
    }

    /**
     * The second layout step where we do the actual layout of the views for the final state.
     * This step might be run multiple times if necessary (e.g. measure).
     * <p>
     * 真正对child进行测量和布局
     */
    private void dispatchLayoutStep2() {
        Log.d(TAG, "dispatchLayoutStep2-----");
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();
        mState.assertLayoutStep(State.STEP_LAYOUT | State.STEP_ANIMATIONS);
        mState.mItemCount = mAdapter.getItemCount();
        // Step 2: Run layout
        mState.mInPreLayout = false;
        mLayout.onLayoutChildren(this, mState);
        mState.mRunSimpleAnimations = mState.mRunSimpleAnimations && mItemAnimator != null;
        mState.mLayoutStep = State.STEP_ANIMATIONS;
        onExitLayoutOrScroll();
        stopInterceptRequestLayout();
    }

    /**
     * Keeps data about views to be used for animations
     */
    final ViewInfoStore mViewInfoStore = new ViewInfoStore();

    /**
     * The final step of the layout where we save the information about views for animations,
     * trigger animations and do any necessary cleanup.
     */
    private void dispatchLayoutStep3() {
        mState.assertLayoutStep(State.STEP_ANIMATIONS);
        startInterceptRequestLayout();
        onEnterLayoutOrScroll();
        mState.mLayoutStep = State.STEP_START;
        if (mState.mRunSimpleAnimations) {
            int childCount = mChildHelper.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                FlexItemHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
                final ItemAnimator.ItemHolderInfo animationInfo = mItemAnimator
                        .recordPostLayoutInformation(mState, holder);
                long key = getChangedHolderKey(holder);
                FlexItemHolder oldChangeViewHolder = mViewInfoStore.getFromOldChangeHolders(key);
                if (oldChangeViewHolder != null) {
                    //做位移动画
                } else {
                    mViewInfoStore.addToPostLayout(holder, animationInfo);
                }
            }
            // Step 4: Process view info lists and trigger animations
            mViewInfoStore.process(mViewInfoProcessCallback);
        }
        mState.mPreviousLayoutItemCount = mState.mItemCount;
        onExitLayoutOrScroll();
        stopInterceptRequestLayout();
    }

    /**
     * Returns a unique key to be used while handling change animations.
     * It might be child's position or stable id depending on the adapter type.
     */
    long getChangedHolderKey(FlexItemHolder holder) {
        return holder.mPosition;
    }


    /**
     * childCount 为FlexTabLayout的child数量
     */
    public void detachAttachedViews() {
        int childCount = getChildCount();
        Log.i(TAG, "detachAttachedViews childCount：-------------" + childCount);
        for (int i = childCount - 1; i >= 0; i--) {
            final View v = getChildAt(i);
            if (v == null) continue;
            detachViewAt(i);
        }
    }

    private void detachViewAt(int index) {
        mChildHelper.detachViewFromParent(index);
    }


    public void layoutChildWithMargins(@NonNull View child, int left, int top, int right,
                                       int bottom) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
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


    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        FlexTabLayout.FlexItemHolder mViewHolder;
        boolean mInsetsDirty = true;
        int rowIndex;
        int columnIndex;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
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

        public final void notifyItemRemoved(int position) {
            mObservable.notifyItemRangeRemoved(position, 1);
        }

        public final void notifyItemRangeRemoved(int positionStart, int itemCount) {
            mObservable.notifyItemRangeRemoved(positionStart, itemCount);
        }

        public abstract void onItemClicked(View itemView, int position);

        @NonNull
        public abstract FH onCreateItemHolder(@NonNull ViewGroup parent, int position);

        @NonNull
        public abstract void onBindItemHolder(@NonNull FH holder, int position);

        public abstract int getItemCount();

        public void registerAdapterDataObserver(FlexTabViewDataObserver observer) {
            mObservable.registerObserver(observer);
        }
    }

    public abstract static class FlexItemHolder {

        /**
         * This ViewHolder has been bound to a position; mPosition, mItemId and mItemViewType
         * are all valid.
         */
        static final int FLAG_BOUND = 1 << 0;

        /**
         * The data this ViewHolder's view reflects is stale and needs to be rebound
         * by the adapter. mPosition and mItemId are consistent.
         */
        static final int FLAG_UPDATE = 1 << 1;
        /**
         * This ViewHolder points at data that represents an item previously removed from the
         * data set. Its view may still be used for things like outgoing animations.
         */
        static final int FLAG_REMOVED = 1 << 3;

        /**
         * This ViewHolder should not be recycled. This flag is set via setIsRecyclable()
         * and is intended to keep views around during animations.
         */
        static final int FLAG_NOT_RECYCLABLE = 1 << 4;

        static final int FLAG_TMP_DETACHED = 1 << 8;

        /**
         * Used by ItemAnimator when a ViewHolder appears in pre-layout
         */
        static final int FLAG_APPEARED_IN_PRE_LAYOUT = 1 << 12;

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

        boolean isTmpDetached() {
            return (mFlags & FLAG_TMP_DETACHED) != 0;
        }

        /**
         * @param offset
         * @param applyToPreLayout 此次offset的变动是否影响pre-layout阶段holder的参数
         */
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

        FlexHolderRecycler mScrapContainer = null;

        boolean isScrap() {
            return mScrapContainer != null;
        }

        void unScrap() {
            mScrapContainer.unscrapView(this);
        }

        void setScrapContainer(FlexHolderRecycler recycler) {
            mScrapContainer = recycler;
        }

        void flagRemovedAndOffsetPosition(int mNewPosition, int offset, boolean applyToPreLayout) {
            addFlags(FlexItemHolder.FLAG_REMOVED);
//            offsetPosition(offset, applyToPreLayout);
//            mPosition = mNewPosition;
        }

        void addFlags(int flags) {
            mFlags |= flags;
        }

        public final void setIsRecyclable(boolean recyclable) {
            mFlags |= FLAG_NOT_RECYCLABLE;
        }

        public void clearTmpDetachFlag() {
            mFlags = mFlags & ~FLAG_TMP_DETACHED;
        }

        public int getLayoutPosition() {
            return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        }

        public void clearOldPosition() {
            mOldPosition = NO_POSITION;
            mPreLayoutPosition = NO_POSITION;
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

        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            // since onItemRangeRemoved() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeRemoved(positionStart, itemCount);
            }
        }

    }


    public abstract static class AdapterDataObserver {
        public void onChanged() {
            // Do nothing
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            // do nothing
        }
    }

    private class FlexTabViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
//            requestLayout();
//            mState.mStructureChanged = true;
//            mDataSetHasChangedAfterLayout = true;
//            if (!mAdapterHelper.hasPendingUpdates()) {
//                requestLayout();
//            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mAdapterHelper.onItemRangeChanged(positionStart, itemCount, new Object())) {
                triggerUpdateProcessor();
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (mAdapterHelper.onItemRangeRemoved(positionStart, itemCount)) {
                triggerUpdateProcessor();
            }
        }
    }

    private void triggerUpdateProcessor() {
//        if (mIsAttached) {
//            ViewCompat.postOnAnimation(FlexTabLayout.this, mUpdateChildViewsRunnable);
//        }
        requestLayout();
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
        startInterceptRequestLayout();
        mAdapterHelper.preProcess();
        stopInterceptRequestLayout();
    }

    void onEnterLayoutOrScroll() {
        mLayoutOrScrollCounter++;
    }

    void onExitLayoutOrScroll() {
        mLayoutOrScrollCounter--;
        if (mLayoutOrScrollCounter < 1) {
            if (mLayoutOrScrollCounter < 0) {
                throw new IllegalStateException("layout or scroll counter cannot go below zero."
                        + "Some calls are not matching");
            }
            mLayoutOrScrollCounter = 0;

        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLayoutOrScrollCounter = 0;
        mIsAttached = true;
        mFirstLayoutComplete = mFirstLayoutComplete && !isLayoutRequested();

    }

    public static class State {
        static final int STEP_START = 1;
        static final int STEP_LAYOUT = 1 << 1;
        static final int STEP_ANIMATIONS = 1 << 2;
        public boolean mStructureChanged;
        public boolean mInPreLayout;
        int mPreviousLayoutItemCount;

        boolean mIsMeasuring = false;

        int widthUsed;
        int totalHeight;
        int totalWidth;

        //当前行数
        int curRowIndex = 0;

        //当前行的最大高度
        int curRowMaxHeight = 0;

        //当前布局的child  index
        int curChildIndex = 0;


        // 上一个最大的child高度
        int lastChildMaxHeight = 0;
        //每一列的数量
        final SparseArray<Integer> columnCount = new SparseArray<>();
        //每一行的最大高度
        final SparseArray<Integer> rowMaxHeight = new SparseArray<>();

        public int getWidthUsed() {
            return widthUsed;
        }

        public void setWidthUsed(int widthUsed) {
            this.widthUsed = widthUsed;
        }

        public int getTotalHeight() {
            return totalHeight;
        }

        public void setTotalHeight(int totalHeight) {
            this.totalHeight = totalHeight;
        }

        boolean mRunSimpleAnimations = true;

        boolean mRunPredictiveAnimations = false;

        boolean hasMore(RecyclerView.State state, int position) {
            return position >= 0 && position < state.getItemCount();
        }

        public int getItemCount() {
            return mInPreLayout
                    ? (mPreviousLayoutItemCount)
                    : mItemCount;
        }

        /**
         * Number of items adapter has.
         */
        int mItemCount = 0;

        @IntDef(flag = true, value = {
                STEP_START, STEP_LAYOUT, STEP_ANIMATIONS
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

    public abstract static class ItemAnimator {
        /**
         * The Item represented by this ViewHolder is removed from the adapter.
         * <p>
         */
        public static final int FLAG_REMOVED = FlexItemHolder.FLAG_REMOVED;
        private ItemAnimatorListener mListener = null;
        /**
         * This ViewHolder was not laid out but has been added to the layout in pre-layout state
         * by the {@link RecyclerView.LayoutManager}. This means that the item was already in the Adapter but
         * invisible and it may become visible in the post layout phase. LayoutManagers may prefer
         * to add new items in pre-layout to specify their virtual location when they are invisible
         * (e.g. to specify the item should <i>animate in</i> from below the visible area).
         * <p>
         */
        public static final int FLAG_APPEARED_IN_PRE_LAYOUT =
                FlexItemHolder.FLAG_APPEARED_IN_PRE_LAYOUT;

        interface ItemAnimatorListener {
            void onAnimationFinished(@NonNull FlexItemHolder item);
        }

        public interface ItemAnimatorFinishedListener {
            /**
             * Notifies when all pending or running animations in an ItemAnimator are finished.
             */
            void onAnimationsFinished();
        }

        private ArrayList<ItemAnimator.ItemAnimatorFinishedListener> mFinishedListeners =
                new ArrayList<ItemAnimator.ItemAnimatorFinishedListener>();
        private long mAddDuration = 1200;
        private long mRemoveDuration = 1200;
        private long mMoveDuration = 1200;

        void setListener(ItemAnimatorListener listener) {
            mListener = listener;
        }

        /**
         * This method should be called by ItemAnimator implementations to notify
         * any listeners that all pending and active item animations are finished.
         */
        public final void dispatchAnimationsFinished() {
            final int count = mFinishedListeners.size();
            for (int i = 0; i < count; ++i) {
                mFinishedListeners.get(i).onAnimationsFinished();
            }
            mFinishedListeners.clear();
        }

        public abstract boolean animateDisappearance(@NonNull FlexItemHolder viewHolder,
                                                     @NonNull ItemHolderInfo preLayoutInfo, @Nullable ItemHolderInfo postLayoutInfo);

        public abstract boolean animateAppearance(@NonNull FlexItemHolder viewHolder,
                                                  @Nullable ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo);

        public abstract boolean animatePersistence(@NonNull FlexItemHolder viewHolder,
                                                   @NonNull ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo);


        /**
         * Gets the current duration for which all remove animations will run.
         *
         * @return The current remove duration
         */
        public long getRemoveDuration() {
            return mRemoveDuration;
        }

        /**
         * Gets the current duration for which all add animations will run.
         *
         * @return The current add duration
         */
        public long getAddDuration() {
            return mAddDuration;
        }

        public long getMoveDuration() {
            return mMoveDuration;
        }

        public abstract void runPendingAnimations();

        public abstract boolean isRunning();

        public final void dispatchAnimationFinished(FlexTabLayout.FlexItemHolder viewHolder) {
            onAnimationFinished(viewHolder);
            if (mListener != null) {
                mListener.onAnimationFinished(viewHolder);
            }
        }

        public void onAnimationFinished(FlexTabLayout.FlexItemHolder viewHolder) {

        }

        public @NonNull
        ItemAnimator.ItemHolderInfo recordPostLayoutInformation(@NonNull State state,
                                                                @NonNull FlexItemHolder viewHolder) {
            return obtainHolderInfo().setFrom(viewHolder);
        }

        @NonNull
        public ItemAnimator.ItemHolderInfo obtainHolderInfo() {
            return new ItemAnimator.ItemHolderInfo();
        }

        public ItemHolderInfo recordPreLayoutInformation(State mState, FlexItemHolder holder, int i, Object o) {
            return obtainHolderInfo().setFrom(holder);
        }

        public static class ItemHolderInfo {
            /**
             * The left edge of the View (excluding decorations)
             */
            public int left;

            /**
             * The top edge of the View (excluding decorations)
             */
            public int top;

            /**
             * The right edge of the View (excluding decorations)
             */
            public int right;

            /**
             * The bottom edge of the View (excluding decorations)
             */
            public int bottom;

            /**
             * The change flags that were passed to
             */
            @IntDef(flag = true, value = {
                    FLAG_REMOVED,
                    FLAG_APPEARED_IN_PRE_LAYOUT
            })
            @Retention(RetentionPolicy.SOURCE)
            public @interface AdapterChanges {
            }

            public int changeFlags;

            public ItemHolderInfo() {
            }

            @NonNull
            public ItemAnimator.ItemHolderInfo setFrom(@NonNull FlexItemHolder holder) {
                return setFrom(holder, 0);
            }

            @NonNull
            public ItemAnimator.ItemHolderInfo setFrom(@NonNull FlexItemHolder holder,
                                                       @AdapterChanges int flags) {
                final View view = holder.itemView;
                this.left = view.getLeft();
                this.top = view.getTop();
                this.right = view.getRight();
                this.bottom = view.getBottom();
                return this;
            }
        }
    }

    public abstract static class LayoutManager {
        ChildHelper mChildHelper;
        FlexTabLayout mFlexTabLayout;
        boolean mIsAttachedToWindow = false;
        boolean mAutoMeasure = false;
        private int mWidthMode, mHeightMode;
        private int mWidth, mHeight;
        private int wSpec;
        private int hSpec;

        public int getwSpec() {
            return wSpec;
        }

        public int gethSpec() {
            return hSpec;
        }

        /**
         * On M+, an unspecified measure spec may include a hint which we can use. On older platforms,
         * this value might be garbage. To save LayoutManagers from it, RecyclerView sets the size to
         * 0 when mode is unspecified.
         */
        static final boolean ALLOW_SIZE_IN_UNSPECIFIED_SPEC = Build.VERSION.SDK_INT >= 23;

        void setFlexTabLayout(FlexTabLayout flexTabLayout) {
            if (flexTabLayout == null) {
                mFlexTabLayout = null;
                mChildHelper = null;
                mWidth = 0;
                mHeight = 0;
            } else {
                mFlexTabLayout = flexTabLayout;
                mChildHelper = flexTabLayout.mChildHelper;
                mWidth = flexTabLayout.getWidth();
                mHeight = flexTabLayout.getHeight();
            }
            mWidthMode = MeasureSpec.EXACTLY;
            mHeightMode = MeasureSpec.EXACTLY;
        }

        void setMeasureSpecs(int wSpec, int hSpec) {
            mWidth = MeasureSpec.getSize(wSpec);
            mWidthMode = MeasureSpec.getMode(wSpec);
            this.wSpec = wSpec;
            this.hSpec = hSpec;
            if (mWidthMode == MeasureSpec.UNSPECIFIED && !ALLOW_SIZE_IN_UNSPECIFIED_SPEC) {
                mWidth = 0;
            }

            mHeight = MeasureSpec.getSize(hSpec);
            mHeightMode = MeasureSpec.getMode(hSpec);
            if (mHeightMode == MeasureSpec.UNSPECIFIED && !ALLOW_SIZE_IN_UNSPECIFIED_SPEC) {
                mHeight = 0;
            }
        }


        void setMeasuredDimensionFromChildren() {
            int width = getMeasureWidthFromChildren(mFlexTabLayout.mState, wSpec, hSpec);
            int height = getMeasureHeightFromChildren(mFlexTabLayout.mState, wSpec, hSpec);
            Log.i(TAG, "setMeasuredDimensionFromChildren width:" + width + "---height:" + height);
            mFlexTabLayout.setMeasuredDimension(width, height);
        }

        public void requestLayout() {
            if (mFlexTabLayout != null) {
                mFlexTabLayout.requestLayout();
            }
        }

        void dispatchAttachedToWindow(RecyclerView view) {
            mIsAttachedToWindow = true;
        }

        void dispatchDetachedFromWindow(FlexTabLayout view) {
            mIsAttachedToWindow = false;
        }

        public boolean isAttachedToWindow() {
            return mIsAttachedToWindow;
        }

        public void postOnAnimation(Runnable action) {
            if (mFlexTabLayout != null) {
                ViewCompat.postOnAnimation(mFlexTabLayout, action);
            }
        }

        public boolean removeCallbacks(Runnable action) {
            if (mFlexTabLayout != null) {
                return mFlexTabLayout.removeCallbacks(action);
            }
            return false;
        }

        public void onLayoutChildren(FlexTabLayout flexTabLayout, FlexTabLayout.State state) {
            Log.e(TAG, "You must override onLayoutChildren(Recycler recycler, State state) ");
        }

        public abstract FlexTabLayout.LayoutParams generateDefaultLayoutParams();

        public boolean checkLayoutParams(FlexTabLayout.LayoutParams lp) {
            return lp != null;
        }

        public FlexTabLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
            if (lp instanceof FlexTabLayout.LayoutParams) {
                return new FlexTabLayout.LayoutParams((FlexTabLayout.LayoutParams) lp);
            } else if (lp instanceof MarginLayoutParams) {
                return new FlexTabLayout.LayoutParams((MarginLayoutParams) lp);
            } else {
                return new FlexTabLayout.LayoutParams(lp);
            }
        }

        public FlexTabLayout.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
            return new FlexTabLayout.LayoutParams(c, attrs);
        }

        public void addView(View child) {
            addView(child, -1);
        }

        public void addView(View child, int index) {
            addViewInt(child, index, false);
        }

        private void addViewInt(View child, int index, boolean disappearing) {
            if (child == null) return;
            FlexItemHolder holder = getChildViewHolderInt(child);
            if (holder == null) return;
            if (holder.isTmpDetached()) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                mChildHelper.attachViewToParent(child, index, lp, false);
            } else {
                mChildHelper.addView(child, index, false);
            }
        }

        public int getWidthMode() {
            return mWidthMode;
        }

        public int getHeightMode() {
            return mHeightMode;
        }

        @Px
        public int getWidth() {
            return mWidth;
        }

        @Px
        public int getHeight() {
            return mHeight;
        }

        /**
         * Return the left padding of the parent RecyclerView
         *
         * @return Padding in pixels
         */
        @Px
        public int getPaddingLeft() {
            return mFlexTabLayout != null ? mFlexTabLayout.getPaddingLeft() : 0;
        }

        /**
         * Return the top padding of the parent RecyclerView
         *
         * @return Padding in pixels
         */
        @Px
        public int getPaddingTop() {
            return mFlexTabLayout != null ? mFlexTabLayout.getPaddingTop() : 0;
        }

        /**
         * Return the right padding of the parent RecyclerView
         *
         * @return Padding in pixels
         */
        @Px
        public int getPaddingRight() {
            return mFlexTabLayout != null ? mFlexTabLayout.getPaddingRight() : 0;
        }

        /**
         * Return the bottom padding of the parent RecyclerView
         *
         * @return Padding in pixels
         */
        @Px
        public int getPaddingBottom() {
            return mFlexTabLayout != null ? mFlexTabLayout.getPaddingBottom() : 0;
        }

        /**
         * Return the start padding of the parent RecyclerView
         *
         * @return Padding in pixels
         */
        @Px
        public int getPaddingStart() {
            return mFlexTabLayout != null ? ViewCompat.getPaddingStart(mFlexTabLayout) : 0;
        }

        /**
         * Return the end padding of the parent RecyclerView
         *
         * @return Padding in pixels
         */
        @Px
        public int getPaddingEnd() {
            return mFlexTabLayout != null ? ViewCompat.getPaddingEnd(mFlexTabLayout) : 0;
        }

        public int getHorizontalSpace() {
            return FlexTabLayout.HORIZONTAL_SPACE;
        }

        public int getVerticalSpace() {
            return FlexTabLayout.VERTICAL_SPACE;
        }

        public abstract int getMeasureWidthFromChildren(FlexTabLayout.State state, int widthSpec, int heightSpec);

        public abstract int getMeasureHeightFromChildren(FlexTabLayout.State state, int widthSpec, int heightSpec);
    }


    public final class FlexHolderRecycler {
        final ArrayList<FlexItemHolder> mAttachedScrap = new ArrayList<>();

        @Nullable
        FlexItemHolder tryGetViewHolderForPosition(final int position) {
            FlexItemHolder holder = getHolderFromScrap(position);
            if (holder == null) {
                holder = createNewHolder(position);
                scrapView(holder);
                holder.setScrapContainer(mRecycler);
            } else {
//                holder.unScrap();
//                mRecycler.unscrapView(holder);
            }
            if (!mState.mInPreLayout) {
                mAdapter.onBindItemHolder(((LayoutParams) holder.itemView.getLayoutParams()).mViewHolder, position);
            }
            final FlexItemHolder finalHolder = holder;
            holder.itemView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mAdapter.onItemClicked(finalHolder.itemView, position);
                }
            });
            return holder;
        }

        void scrapView(FlexItemHolder holder) {
            mAttachedScrap.add(holder);
        }

        void unscrapView(FlexItemHolder holder) {
            mAttachedScrap.remove(holder);
        }

        private FlexItemHolder getHolderFromScrap(int position) {
            boolean mInPreLayout = mState.mInPreLayout;
            final int scrapCount = mAttachedScrap.size();
            for (int i = 0; i < scrapCount; i++) {
                final FlexItemHolder holder = mAttachedScrap.get(i);
                int layoutPosition = holder.getLayoutPosition();
                if (layoutPosition == position) {
                    Log.i(TAG, "getHolderFromScrap  position:" + position);
                    return holder;
                }
            }
            return null;
        }

        @NonNull
        private FlexItemHolder createNewHolder(int position) {
            FlexItemHolder holder = mAdapter.onCreateItemHolder(FlexTabLayout.this, position);
            holder.mPosition = position;
            final View child = holder.itemView;
            final ViewGroup.LayoutParams lp = child.getLayoutParams();
            final LayoutParams layoutParams;
            if (lp == null) {
                layoutParams = (LayoutParams) generateDefaultLayoutParams();
                child.setLayoutParams(layoutParams);
            } else {
                layoutParams = (LayoutParams) generateLayoutParams(lp);
                child.setLayoutParams(layoutParams);
            }
            layoutParams.mViewHolder = holder;
            Log.i(TAG, "createNewHolder  position:" + position);
            return holder;
        }

        public void clearOldPositions() {
            for (int i = 0; i < mAttachedScrap.size(); i++) {
                mAttachedScrap.get(i).clearOldPosition();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        boolean needsInvalidate = false;
        if (mItemAnimator != null && mItemAnimator.isRunning()) {
            needsInvalidate = true;
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
            Log.i(TAG, "draw needsInvalidate:" + needsInvalidate);
        }
    }
}
