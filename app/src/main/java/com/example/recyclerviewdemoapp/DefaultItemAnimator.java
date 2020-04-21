package com.example.recyclerviewdemoapp;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * This implementation of {@link RecyclerView.ItemAnimator} provides basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a DefaultItemAnimator by default.
 *
 * @see RecyclerView#setItemAnimator(RecyclerView.ItemAnimator)
 */
public class DefaultItemAnimator extends SimpleItemAnimator {
    private static TimeInterpolator sDefaultInterpolator;
    private ArrayList<FlexTabLayout.FlexItemHolder> mPendingAdditions = new ArrayList<>();
    ArrayList<FlexTabLayout.FlexItemHolder> mAddAnimations = new ArrayList<>();

    @Override
    public boolean animateAdd(final FlexTabLayout.FlexItemHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0);
        mPendingAdditions.add(holder);
        return true;
    }

    private void resetAnimation(FlexTabLayout.FlexItemHolder holder) {
        if (sDefaultInterpolator == null) {
            sDefaultInterpolator = new ValueAnimator().getInterpolator();
        }
        holder.itemView.animate().setInterpolator(sDefaultInterpolator);
//        endAnimation(holder);
    }

    @Override
    public void runPendingAnimations() {

    }
}
