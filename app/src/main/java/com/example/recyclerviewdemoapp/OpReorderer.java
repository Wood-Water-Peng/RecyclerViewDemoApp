package com.example.recyclerviewdemoapp;


public class OpReorderer {
    final Callback mCallback;

    OpReorderer(Callback callback) {
        mCallback = callback;
    }

    interface Callback {

        AdapterHelper.UpdateOp obtainUpdateOp(int cmd, int startPosition, int itemCount, Object payload);

    }
}
