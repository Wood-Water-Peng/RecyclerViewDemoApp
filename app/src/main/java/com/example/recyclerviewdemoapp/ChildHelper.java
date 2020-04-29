package com.example.recyclerviewdemoapp;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import static com.example.recyclerviewdemoapp.BuildConfig.DEBUG;

/**
 * Helper class to manage children.
 */
public class ChildHelper {
    private static final String TAG = "ChildrenHelper";

    final ChildHelper.Callback mCallback;
    final List<View> mHiddenViews;

    public ChildHelper(Callback mCallback) {
        this.mCallback = mCallback;
        this.mHiddenViews = new ArrayList<>();
    }

    /**
     * Marks a child view as hidden
     *
     * @param child View to hide.
     */
    private void hideViewInternal(View child) {
        mHiddenViews.add(child);
        mCallback.onEnteredHiddenState(child);
    }

    /**
     * Unmarks a child view as hidden.
     *
     * @param child View to hide.
     */
    private boolean unhideViewInternal(View child) {
        if (mHiddenViews.remove(child)) {
            mCallback.onLeftHiddenState(child);
            return true;
        } else {
            return false;
        }
    }

    View getChildAt(int index) {
        return mCallback.getChildAt(index);
    }

    /**
     * Returns the number of children that are not hidden.
     *
     * @return Number of children that are not hidden.
     * @see #getChildAt(int)
     */
    int getChildCount() {
        return mCallback.getChildCount() - mHiddenViews.size();
    }

    /**
     * Returns the total number of children.
     *
     * @return The total number of children including the hidden views.
     * @see #getUnfilteredChildAt(int)
     */
    int getUnfilteredChildCount() {
        return mCallback.getChildCount();
    }

    /**
     * Returns a child by ViewGroup offset. ChildHelper won't offset this index.
     *
     * @param index ViewGroup index of the child to return.
     * @return The view in the provided index.
     */
    View getUnfilteredChildAt(int index) {
        return mCallback.getChildAt(index);
    }

    public void detachViewFromParent(int index) {
        mCallback.detachViewFromParent(index);
    }

    public void addView(View child, int index, boolean hidden) {
        final int offset;
        if (index < 0) {
            offset = mCallback.getChildCount();
        } else {
            offset = 0;
        }
        mCallback.addView(child, offset);
    }

    boolean removeViewIfHidden(View view) {
        final int index = mCallback.indexOfChild(view);
        if (index == -1) {
//            if (unhideViewInternal(view) && DEBUG) {
//                throw new IllegalStateException("view is in hidden list but not in view group");
//            }
            return true;
        } else {
            mCallback.removeViewAt(index);
            return true;
        }
    }

    void addView(View child, boolean hidden) {
        addView(child, -1, hidden);
    }

    /**
     * Attaches the provided view to the underlying ViewGroup.
     *
     * @param child        Child to attach.
     * @param index        Index of the child to attach in regular perspective.
     * @param layoutParams LayoutParams for the child.
     * @param hidden       If set to true, this item will be invisible to the regular methods.
     */
    void attachViewToParent(View child, int index, ViewGroup.LayoutParams layoutParams,
                            boolean hidden) {
        final int offset;
        if (index < 0) {
            offset = mCallback.getChildCount();
        } else {
            offset = index;
        }
        if (hidden) {
            hideViewInternal(child);
        }
        mCallback.attachViewToParent(child, offset, layoutParams);
    }

    interface Callback {

        int getChildCount();

        void addView(View child, int index);

        int indexOfChild(View view);

        void removeViewAt(int index);

        View getChildAt(int index);

        void removeAllViews();

        void onEnteredHiddenState(View child);

        void onLeftHiddenState(View child);

        void detachViewFromParent(int index);

        void attachViewToParent(View child, int offset, ViewGroup.LayoutParams layoutParams);
    }
}


