package com.example.recyclerviewdemoapp;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public abstract class SimpleItemAnimator extends FlexTabLayout.ItemAnimator {
    @Override
    public boolean animateDisappearance(@NonNull FlexTabLayout.FlexItemHolder viewHolder, @NonNull ItemHolderInfo preLayoutInfo, @Nullable ItemHolderInfo postLayoutInfo) {
        return animateRemove(viewHolder);
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

    @Override
    public boolean animatePersistence(@NonNull FlexTabLayout.FlexItemHolder viewHolder, @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {
        if (preInfo.left != postInfo.left || preInfo.top != postInfo.top) {
            return animateMove(viewHolder,
                    preInfo.left, preInfo.top, postInfo.left, postInfo.top);
        }
        return false;
    }

    /**
     * @param holder The item that is being added.
     * @return true if a later call to {@link #runPendingAnimations()} is requested,
     * false otherwise.
     */
    public abstract boolean animateAdd(FlexTabLayout.FlexItemHolder holder);

    public abstract boolean animateRemove(FlexTabLayout.FlexItemHolder holder);

    public abstract boolean animateMove(FlexTabLayout.FlexItemHolder holder, int fromX, int fromY,
                                        int toX, int toY);

    public final void dispatchRemoveFinished(FlexTabLayout.FlexItemHolder item) {
        onRemoveFinished(item);
        dispatchAnimationFinished(item);
    }

    public void onRemoveFinished(FlexTabLayout.FlexItemHolder item) {
    }


}
