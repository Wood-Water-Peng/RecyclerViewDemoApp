package com.example.recyclerviewdemoapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.core.view.ViewCompat;
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
    ArrayList<ArrayList<FlexTabLayout.FlexItemHolder>> mAdditionsList = new ArrayList<>();

    @Override
    public boolean animateAdd(final FlexTabLayout.FlexItemHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0);
        mPendingAdditions.add(holder);
        return true;
    }

    void animateAddImpl(final FlexTabLayout.FlexItemHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        mAddAnimations.add(holder);
        animation.alpha(1).setDuration(getAddDuration())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        view.setAlpha(1);
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        animation.setListener(null);
                        mAddAnimations.remove(holder);
                    }
                }).start();
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
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (additionsPending) {
            final ArrayList<FlexTabLayout.FlexItemHolder> additions = new ArrayList<>();
            additions.addAll(mPendingAdditions);
            mAdditionsList.add(additions);
            mPendingAdditions.clear();
            Runnable adder = new Runnable() {
                @Override
                public void run() {
                    for (FlexTabLayout.FlexItemHolder holder : additions) {
                        animateAddImpl(holder);
                    }
                    additions.clear();
                    mAdditionsList.remove(additions);
                }
            };

            adder.run();
        }
    }
}
