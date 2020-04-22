package com.example.recyclerviewdemoapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import androidx.collection.SimpleArrayMap;

/**
 * This class abstracts all tracking for Views to run animations.
 */
public class ViewInfoStore {
    /**
     * View data records for pre-layout
     */
    @VisibleForTesting
    final SimpleArrayMap<FlexTabLayout.FlexItemHolder, ViewInfoStore.InfoRecord> mLayoutHolderMap =
            new SimpleArrayMap<>();

    @VisibleForTesting
    final LongSparseArray<FlexTabLayout.FlexItemHolder> mOldChangedHolders = new LongSparseArray<>();

    void process(ViewInfoStore.ProcessCallback callback) {
        for (int index = mLayoutHolderMap.size() - 1; index >= 0; index--) {
            final FlexTabLayout.FlexItemHolder viewHolder = mLayoutHolderMap.keyAt(index);
            final ViewInfoStore.InfoRecord record = mLayoutHolderMap.removeAt(index);
            if ((record.flags & InfoRecord.FLAG_APPEAR_PRE_AND_POST) == InfoRecord.FLAG_APPEAR_PRE_AND_POST) {
                // Appeared in the layout but not in the adapter (e.g. entered the viewport)
                callback.processAppeared(viewHolder, record.preInfo, record.postInfo);
            }
        }
    }

    /**
     * Adds the given ViewHolder to the oldChangeHolders list
     *
     * @param key    The key to identify the ViewHolder.
     * @param holder The ViewHolder to store
     */
    void addToOldChangeHolders(long key, FlexTabLayout.FlexItemHolder holder) {
        mOldChangedHolders.put(key, holder);
    }

    /**
     * Queries the oldChangeHolder list for the given key. If they are not tracked, simply returns
     * null.
     *
     * @param key The key to be used to find the ViewHolder.
     * @return A ViewHolder if exists or null if it does not exist.
     */
    FlexTabLayout.FlexItemHolder getFromOldChangeHolders(long key) {
        return mOldChangedHolders.get(key);
    }

    /**
     * Adds the item information to the post layout list
     *
     * @param holder The ViewHolder whose information is being saved
     * @param info   The information to save
     */
    void addToPostLayout(FlexTabLayout.FlexItemHolder holder, FlexTabLayout.ItemAnimator.ItemHolderInfo info) {
        ViewInfoStore.InfoRecord record = mLayoutHolderMap.get(holder);
        if (record == null) {
            record = ViewInfoStore.InfoRecord.obtain();
            mLayoutHolderMap.put(holder, record);
        }
        record.postInfo = info;
        record.flags |= InfoRecord.FLAG_POST;
    }

    static class InfoRecord {
        // disappearing list
        static final int FLAG_DISAPPEARED = 1;
        // appear in pre layout list
        static final int FLAG_APPEAR = 1 << 1;

        // pre layout, this is necessary to distinguish null item info
        static final int FLAG_PRE = 1 << 2;
        // post layout, this is necessary to distinguish null item info
        static final int FLAG_POST = 1 << 3;

        static final int FLAG_APPEAR_AND_DISAPPEAR = FLAG_APPEAR | FLAG_DISAPPEARED;
        static final int FLAG_PRE_AND_POST = FLAG_PRE | FLAG_POST;
        static final int FLAG_APPEAR_PRE_AND_POST = FLAG_APPEAR | FLAG_PRE | FLAG_POST;
        int flags;

        @Nullable
        FlexTabLayout.ItemAnimator.ItemHolderInfo preInfo;
        @Nullable
        FlexTabLayout.ItemAnimator.ItemHolderInfo postInfo;

        static InfoRecord obtain() {

            return new InfoRecord();
        }
    }

    interface ProcessCallback {
        void processDisappeared(FlexTabLayout.FlexItemHolder viewHolder, @NonNull FlexTabLayout.ItemAnimator.ItemHolderInfo preInfo,
                                @Nullable FlexTabLayout.ItemAnimator.ItemHolderInfo postInfo);

        void processAppeared(FlexTabLayout.FlexItemHolder viewHolder, @Nullable FlexTabLayout.ItemAnimator.ItemHolderInfo preInfo,
                             FlexTabLayout.ItemAnimator.ItemHolderInfo postInfo);
    }
}
