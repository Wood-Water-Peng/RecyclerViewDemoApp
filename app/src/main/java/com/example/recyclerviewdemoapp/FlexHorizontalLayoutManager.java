package com.example.recyclerviewdemoapp;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.MeasureSpec.AT_MOST;

/**
 * 横向布局
 */
public class FlexHorizontalLayoutManager extends FlexTabLayout.LayoutManager {
    public static final String TAG = "FlexHorizontalLayoutManager";

    @Override
    public FlexTabLayout.LayoutParams generateDefaultLayoutParams() {
        return new FlexTabLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public int getMeasureWidthFromChildren(FlexTabLayout.State state, int widthSpec, int heightSpec) {
        int width;
        if (View.MeasureSpec.getMode(widthSpec) == AT_MOST) {
            width = state.totalWidth;
        } else {
            width = View.MeasureSpec.getSize(widthSpec);
        }
        return width;
    }

    @Override
    public int getMeasureHeightFromChildren(FlexTabLayout.State state, int widthSpec, int heightSpec) {
        int height;
        if (View.MeasureSpec.getMode(heightSpec) == AT_MOST) {
            height = state.totalHeight;
        } else {
            height = View.MeasureSpec.getSize(heightSpec);
        }
        return height;
    }

    @Override
    public void onLayoutChildren(FlexTabLayout flexTabLayout, FlexTabLayout.State state) {
        //step1:获取一个child
        state.widthUsed = 0;
        state.totalHeight = 0;
        state.columnCount.clear();
        state.curChildIndex = 0;
        state.curRowIndex = 0;
        state.curRowMaxHeight = 0;
        mFlexTabLayout.detachAttachedViews();
        for (int i = 0; i < state.mItemCount; i++) {
            View view = mFlexTabLayout.getChildViewManager().getChildAt(i);
            fillChild(view, state);
        }
    }


    /**
     * @param child
     * @param state 1.测量child
     *              2.布局child
     */
    private void fillChild(@NonNull View child, FlexTabLayout.State state) {
        measureChildWithMargins(child);
        int left = 0, top = 0, right = 0, bottom = 0;
        int startRow = state.curRowIndex;
        int startHeight = getStartRowHeight(state, state.curRowIndex);
        FlexTabLayout.LayoutParams params = (FlexTabLayout.LayoutParams) child.getLayoutParams();
        int maxRowWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int childNeedWidth;
        if (state.curChildIndex == 0) {
            childNeedWidth = child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
        } else {
            childNeedWidth = child.getMeasuredWidth() + params.leftMargin + params.rightMargin + getHorizontalSpace();
        }
        if (childNeedWidth > (maxRowWidth - state.getWidthUsed())) {
            //换行
            childNeedWidth = child.getMeasuredWidth() + params.leftMargin + params.rightMargin;
            left = getPaddingLeft() + params.leftMargin;
            right = left + child.getMeasuredWidth();
            state.setWidthUsed(childNeedWidth);
            params.rowIndex = state.curRowIndex;
            state.columnCount.put(state.curRowIndex, 0);
            state.curRowIndex++;
        } else {
            Integer count = state.columnCount.get(state.curRowIndex, 0);
            state.columnCount.put(state.curRowIndex, count++);
            if (state.curChildIndex == 0) {
                left = state.widthUsed + getPaddingLeft() + params.leftMargin;
            } else {
                left = state.widthUsed + getPaddingLeft() + getHorizontalSpace() + params.leftMargin;
            }
            right = left + child.getMeasuredWidth();
            state.setWidthUsed(state.widthUsed + childNeedWidth);
            state.totalWidth = Math.min(View.MeasureSpec.getSize(getwSpec()), state.totalWidth + childNeedWidth);
        }

        int childNeedHeight;
        if (state.curRowIndex > 0) {
            childNeedHeight = child.getMeasuredHeight() + params.topMargin + params.bottomMargin + getVerticalSpace();
        } else {
            childNeedHeight = child.getMeasuredHeight() + params.topMargin + params.bottomMargin;
        }
        if (state.curChildIndex == 0) {
            state.totalHeight = childNeedHeight;
            state.lastChildMaxHeight = childNeedHeight;
        }
        state.curRowMaxHeight = state.lastChildMaxHeight;
        if (getHeightMode() == AT_MOST) {
            if (state.curRowIndex > startRow) {
                //在之前横向布局的过程中，执行了换行操作
                startHeight += state.curRowMaxHeight;
                startHeight += getVerticalSpace();
                state.lastChildMaxHeight = childNeedHeight;
                state.curRowMaxHeight = state.lastChildMaxHeight;
                state.totalHeight += state.curRowMaxHeight;
                state.rowMaxHeight.put(state.curRowIndex, startHeight);
            } else {
                if (childNeedHeight > state.lastChildMaxHeight) {
                    state.totalHeight += (childNeedHeight - state.lastChildMaxHeight);
                    state.lastChildMaxHeight = childNeedHeight;
                }
            }
        }
        top = startHeight;
        bottom = top + child.getMeasuredHeight();
        params.rowIndex = state.curRowIndex;
        params.columnIndex = state.columnCount.get(params.rowIndex, 0) + 1;
        state.curChildIndex++;
        child.layout(left, top, right, bottom);
        if (child.getParent() == null) {
            mFlexTabLayout.addView(child, params);
        }
        Log.i(TAG, "childNeedWidth:---" + childNeedWidth);
    }

    public void measureChildWithMargins(@NonNull View child) {
        final FlexTabLayout.LayoutParams lp = (FlexTabLayout.LayoutParams) child.getLayoutParams();
        int childWidthSpec = mFlexTabLayout.getChildMeasureSpec(getwSpec(), getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
        int childHeightSpec = mFlexTabLayout.getChildMeasureSpec(gethSpec(), getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin, lp.height);
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * 获取此时布局的起始行高
     * 考虑了parent的padding和divider
     */

    private int getStartRowHeight(FlexTabLayout.State state, int rowIndex) {
        if (state == null) return 0;
        int size = state.rowMaxHeight.size();
        if (size == 0) {
            return getPaddingTop();
        }
        int startHeight = state.rowMaxHeight.get(rowIndex, 0);
        if (state.curRowIndex == 0) {
            return startHeight + getPaddingTop();
        } else {
            return startHeight;
        }
    }
}
