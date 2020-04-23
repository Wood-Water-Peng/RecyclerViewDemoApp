package com.example.recyclerviewdemoapp;

import android.util.Log;

import java.util.ArrayList;

public class AdapterHelper implements OpReorderer.Callback {
    final AdapterHelper.Callback mCallback;
    final ArrayList<AdapterHelper.UpdateOp> mPendingUpdates = new ArrayList<AdapterHelper.UpdateOp>();
    Runnable mOnItemProcessedCallback;
    private int mExistingUpdateTypes = 0;

    public AdapterHelper(AdapterHelper.Callback mCallback) {
        this.mCallback = mCallback;
        mOpReorderer = new OpReorderer(this);
    }

    /**
     * @return True if updates should be processed.
     */
    boolean onItemRangeChanged(int positionStart, int itemCount, Object payload) {
        if (itemCount < 1) {
            return false;
        }
        mPendingUpdates.add(obtainUpdateOp(AdapterHelper.UpdateOp.ADD, positionStart, itemCount, payload));
        return mPendingUpdates.size() == 1;
    }

    /**
     * @return True if updates should be processed.
     */
    boolean onItemRangeRemoved(int positionStart, int itemCount) {
        if (itemCount < 1) {
            return false;
        }
        mPendingUpdates.add(obtainUpdateOp(AdapterHelper.UpdateOp.REMOVE, positionStart, itemCount, null));
        mExistingUpdateTypes |= AdapterHelper.UpdateOp.REMOVE;
        return mPendingUpdates.size() == 1;
    }

    @Override
    public UpdateOp obtainUpdateOp(int cmd, int positionStart, int itemCount, Object payload) {
        AdapterHelper.UpdateOp op = new AdapterHelper.UpdateOp(cmd, positionStart, itemCount, payload);
        op.cmd = cmd;
        op.positionStart = positionStart;
        op.itemCount = itemCount;
        op.payload = payload;
        return op;
    }

    final OpReorderer mOpReorderer;

    void preProcess() {
        final int count = mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            AdapterHelper.UpdateOp op = mPendingUpdates.get(i);
            switch (op.cmd) {
                case AdapterHelper.UpdateOp.ADD:
                    applyAdd(op);
                    break;
                case AdapterHelper.UpdateOp.REMOVE:
                    applyRemove(op);
                    break;
            }
            if (mOnItemProcessedCallback != null) {
                mOnItemProcessedCallback.run();
            }
        }
        mPendingUpdates.clear();
    }

    private void applyRemove(UpdateOp op) {
        postponeAndUpdateViewHolders(op);
    }

    final ArrayList<AdapterHelper.UpdateOp> mPostponedList = new ArrayList<AdapterHelper.UpdateOp>();

    private void postponeAndUpdateViewHolders(AdapterHelper.UpdateOp op) {

        mPostponedList.add(op);
        switch (op.cmd) {
            case AdapterHelper.UpdateOp.ADD:
                mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
                break;
            case AdapterHelper.UpdateOp.REMOVE:
                mCallback.offsetPositionsForRemovingLaidOutOrNewView(op.positionStart,
                        op.itemCount);
                break;
            default:
                throw new IllegalArgumentException("Unknown update op type for " + op);
        }
    }

    private void applyAdd(UpdateOp op) {
        postponeAndUpdateViewHolders(op);
    }

    public boolean hasPendingUpdates() {
        return mPendingUpdates.size() > 0;
    }

    static final class UpdateOp {

        static final int ADD = 1;

        static final int REMOVE = 1 << 1;

        static final int UPDATE = 1 << 2;

        static final int MOVE = 1 << 3;

        static final int POOL_SIZE = 30;

        int cmd;

        int positionStart;

        Object payload;

        // holds the target position if this is a MOVE
        int itemCount;

        UpdateOp(int cmd, int positionStart, int itemCount, Object payload) {
            this.cmd = cmd;
            this.positionStart = positionStart;
            this.itemCount = itemCount;
            this.payload = payload;
        }

        String cmdToString() {
            switch (cmd) {
                case ADD:
                    return "add";
                case REMOVE:
                    return "rm";
                case UPDATE:
                    return "up";
                case MOVE:
                    return "mv";
            }
            return "??";
        }

        @Override
        public String toString() {
            return Integer.toHexString(System.identityHashCode(this))
                    + "[" + cmdToString() + ",s:" + positionStart + "c:" + itemCount
                    + ",p:" + payload + "]";
        }


        @Override
        public int hashCode() {
            int result = cmd;
            result = 31 * result + positionStart;
            result = 31 * result + itemCount;
            return result;
        }
    }


    /**
     * Contract between AdapterHelper and RecyclerView.
     */
    interface Callback {

        void offsetPositionsForAdd(int positionStart, int itemCount);

        void offsetPositionsForRemovingLaidOutOrNewView(int positionStart, int itemCount);
    }

}
