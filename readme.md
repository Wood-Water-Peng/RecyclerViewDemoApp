# 说明

[项目地址](https://github.com/Wood-Water-Peng/RecyclerViewDemoApp)

仿照RecyclerView  

1. 采用适配器模式更新数据
2. 使用LayoutManager自己定义布局
3. 数据插入、移除和变化时的动画效果(实现中)


思路来自RecyclerView，除了没用到他的Recycler复用这个核心思想，其他的都考虑在内。 


待解决问题：

1. onLayout调用多次，attachViewToParent没有执行，第一次布局阶段不应该detachView

2.dispatchLayoutStep3的时候，正确的应该是childCount=0，断点addView

#### 两种布局的状态：

**pre-layout**   

在这个状态中，items会展现出执行predictive动画前的布局状态。

相关变量，mRunPredictiveAnimations，表示当前阶段是否运行前置动画

在整个流程的状态改变：

1 onMeasure阶段

由于LinearLayoutManager默认会mAutoMeasure=true,所以在此阶段不会考虑pre-layout。

2 dispatchLayoutStep1阶段

`mState.mInPreLayout = mState.mRunPredictiveAnimations;`
 
mState.mRunPredictiveAnimations取决于mRunSimpleAnimations，mRunSimpleAnimations取决于第一次布局是否完成。假如我们在已经完成布局的基础上进行增删操作，那么mState.mRunPredictiveAnimations=true,即mState.mInPreLayout =true。

场景1：

在已经布局好的recyclerView中remove一个item，那么这个item的holder在dispatchLayoutStep1会被修改为FLAG_PRE状态。`addToPreLayout`

3 dispatchLayoutStep2阶段

`mState.mInPreLayout = false;`  
接着执行onLayoutChildren。

```
 public void onLayoutChildren(){
    detachAndScrapAttachedViews(recycler);  //recycelrView的mChildCount=0;
    
    fill();  //填充适配中的数据源，假如我们之前的操作是移除了适配器中的所有数据,那么这里不会填充新的item
    
 }，
 
```

4 dispatchLayoutStep3阶段

`mChildHelper.getChildCount()==0`

由于之前detach了，而适配器中的数据源为0，导致fill()没有执行，所以recyclerView此时还是没有children的。导致没有执行`addToPostLayout()`，而直接执行了`mViewInfoStore.process(mViewInfoProcessCallback);`，开启最后的动画操作。

``` 
 processDisappeared();
 animateDisappearance();
 addAnimatingView(holder);   //当动画结束之后，会有一个对应的removeAnimatingView()
```
注意：经过了dispatchLayoutStep2的布局，此时recycelrView中的mChildCount=0,而现在要做消失动画
这里通过addAnimatingView()方法来达到效果。

```
if (viewHolder.isTmpDetached()) {  
            // re-attach  
            mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);  
}
```
当动画结束之后，通过removeAnimatingView(）来移除这个做动画的child,此时recycelrView的mChildCount=0;

**post-layout**






#### 几个重要的概念

```
public boolean hasPendingAdapterUpdates() {
        return !mFirstLayoutComplete || mDataSetHasChangedAfterLayout
                || mAdapterHelper.hasPendingUpdates();
    }
```

该方法表示适配器数据的更新，是否反应到了布局上。

true  表示用户当前看到的，并不是适配器的最新内容

1. 如果第一次布局没有完成，返回false
2. 适配器的数据发生了改变（适配器被替换或者onChanged()方法被调用，返回false
3. 当前的适配器中还有未执行的操作，返回false


**AdapterHelper类**

Adapter的帮助类，针对外界对适配器的操作，可以将这些操作加入到队列，或者执行这些操作。




Bug分析：

1.adapter中的数据为0，但是UI上却依然显示

2.快速新增和移除，UI显示紊乱

3.FlexTabLayout的children数量为0，但是AdapterHelper中mPendingUpdates的数量却>1，导致后续的insert操作无法调用`triggerUpdateProcessor()`方法。



**RecyclerView中的一些重要思路：**

> 布局position:

```
public final int getLayoutPosition() {
return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
}
```
基于对性能和动画效果的考虑，RecyclerView对适配器中的所有更新，打包到下一次layout时去实现。所以可能发生item在适配器中的position和该item在上一次布局中的position不相符的场景。当LinearLayout做计算时需要item的position时，应该调用上面的方法。RecyclerView.LayoutManager，RecyclerView.State和RecyclerView.Recycler中需要position的方法，都应该是item的layout position。


> 如果child已经被移除了，那么怎么保证移除动画的进行呢？

按照常规的思路，child被remove之后，是不会被绘制出来的，那么一个5s消失的动画是怎么完成的呢？

1.保证child仍然在RecyclerView中，但是对他们做一个标识。

```
private void addAnimatingView(ViewHolder viewHolder) {
	   if (viewHolder.isTmpDetached()) {
            // re-attach
            mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);
        }
}
```
RecyclerView在attach的时候，标记了hidden，然后这个child会被加入到ChildHelper的mHiddenViews中。

ChildHelper的getChildCount()方法会忽略hidden的view

```
int getChildCount() {
        return mCallback.getChildCount() - mHiddenViews.size();
    }
```
当动画执行完动画之后，会调用`removeAnimatingView()`

> RecyclerView如何保证addView()却又不requestLayout()呢?

ViewGroup的`attachViewToParent()`，官方的解释是轻量级的方法， 将child添加到viewgroup中，但是不会主动去重绘或者布局。当所有的attach和detach完成之后，可以post一个requestLayout方法，在下一帧执行布局操作。











