package com.example.recyclerviewdemoapp;


import android.view.View;

import java.util.ArrayList;
import java.util.List;

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

    interface Callback {

        int getChildCount();

        void addView(View child, int index);

        int indexOfChild(View view);

        void removeViewAt(int index);

        View getChildAt(int index);

        void removeAllViews();

        void onEnteredHiddenState(View child);

        void onLeftHiddenState(View child);
    }
}


