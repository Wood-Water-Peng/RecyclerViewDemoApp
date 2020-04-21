package com.example.recyclerviewdemoapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public abstract class SimpleItemAnimator extends FlexTabLayout.ItemAnimator {
    @Override
    public boolean animateDisappearance(@NonNull FlexTabLayout.FlexItemHolder viewHolder, @NonNull ItemHolderInfo preLayoutInfo, @Nullable ItemHolderInfo postLayoutInfo) {
        return false;
    }

    @Override
    public boolean animateAppearance(@NonNull FlexTabLayout.FlexItemHolder viewHolder, @Nullable ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
        if (preLayoutInfo != null && (preLayoutInfo.left != postLayoutInfo.left
                || preLayoutInfo.top != postLayoutInfo.top)) {
            //preLayoutInfo存在，则视为位移
            return false;
        } else {
            return animateAdd(viewHolder);
        }
    }

    /**
     * @param holder The item that is being added.
     * @return true if a later call to {@link #runPendingAnimations()} is requested,
     * false otherwise.
     */
    public abstract boolean animateAdd(FlexTabLayout.FlexItemHolder holder);
}
