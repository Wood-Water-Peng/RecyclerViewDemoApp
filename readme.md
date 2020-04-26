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