package com.youngfeng.snake.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import me.foji.snake.util.Utils;

/**
 * SnakeHackLayout 用于滑动关闭视图处理
 *
 * @author Scott Smith 2017-12-13 14:24
 */
public class SnakeHackLayout extends FrameLayout {
    // 使用官方控件，简化拖拽处理
    private ViewDragHelper mViewDragHelper;
    private OnEdgeDragListener onEdgeDragListener;

    // 释放因子：决定页面滑动释放的力度（值为3，即页面滑动超过父控件宽度的1/3后页面可以滑动关闭）
    private final int DEFALT_RELEASE_FACTOR = 3;
    private int mReleaseFactor = DEFALT_RELEASE_FACTOR;
    private int mXRange;

    // 子视图原始坐标
    private PointF mOriginPoint = new PointF(0f, 0f);
    private OnReleaseStateListener onReleaseStateListener;
    private boolean mAllowDragChildView = true;

    // 阴影Drawable
    private GradientDrawable mShadowDrawable;

    // 阴影边缘默认起始颜色
    public static final int DEFAULT_SHADOW_START_COLOR = Color.parseColor("#00000000");
    // 阴影边缘默认结束颜色
    public static final int DEFAULT_SHADOW_END_COLOR = Color.parseColor("#50000000");

    private int mShadowStartColor = DEFAULT_SHADOW_START_COLOR;
    private int mShadowEndColor = DEFAULT_SHADOW_END_COLOR;
    private int mShadowWidth = (int) Utils.dp2px(getContext(),15f);
    private boolean isSettling = false;

    public SnakeHackLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SnakeHackLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(final Context context) {
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return true;
            }

            @Override
            public void onEdgeTouched(int edgeFlags, int pointerId) {
                if(null != onEdgeDragListener) {
                    onEdgeDragListener.onDragStart(SnakeHackLayout.this);
                }
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                return mXRange;
            }

            @Override
            public int getViewVerticalDragRange(View child) {
                return 0;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                if(left < mOriginPoint.x) left = (int) mOriginPoint.x;
                left = mViewDragHelper.isEdgeTouched(ViewDragHelper.EDGE_LEFT) ? left : (int) child.getX();
                left = mAllowDragChildView ? left : (int) mOriginPoint.x;

                return left;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return (int) mOriginPoint.y;
            }

            @Override
            public void onViewDragStateChanged(int state) {
                isSettling = ViewDragHelper.STATE_SETTLING == state;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    int shadowStartColor = Utils.changeAlpha(mShadowStartColor, (int) (Color.alpha(mShadowStartColor) * (1 - (float)left / (float) mXRange)));
                    int shadowEndColor = Utils.changeAlpha(mShadowEndColor, (int) (Color.alpha(mShadowEndColor) * (1 - (float)left / (float) mXRange)));

                    mShadowDrawable.mutate();
                    mShadowDrawable.setColors(new int[] {shadowStartColor , shadowEndColor});
                    invalidate();
                }

                if(null != onEdgeDragListener && needListenForDraging(mViewDragHelper, changedView)) {
                    onEdgeDragListener.onDrag(SnakeHackLayout.this, changedView, left);
                }

                if(left <= 0 || left >= mXRange) {
                    if (null != onReleaseStateListener && isSettling) {
                        onReleaseStateListener.onReleaseCompleted(SnakeHackLayout.this, changedView);
                    }
                }
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                if(null != onEdgeDragListener && mViewDragHelper.isEdgeTouched(ViewDragHelper.EDGE_LEFT)) {
                    boolean shouldClose = xvel > mViewDragHelper.getMinVelocity();

                    if(!shouldClose) {
                        shouldClose = releasedChild.getLeft() > mXRange / mReleaseFactor;
                    }

                    onEdgeDragListener.onRelease(SnakeHackLayout.this, releasedChild, releasedChild.getLeft(), shouldClose);
                }
            }
        });
        mViewDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);

        mShadowDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[] {mShadowStartColor, mShadowEndColor});
    }

    // Only listen for left edge was touched and the child view has left edge when finger leave
    private boolean needListenForDraging(ViewDragHelper viewDragHelper, View childView) {
        return viewDragHelper.isEdgeTouched(ViewDragHelper.EDGE_LEFT)
                || (viewDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING
                    && childView.getLeft() > 0);
    }

    public static SnakeHackLayout getLayout(Context context, View contentView, boolean allowDragChildView) {
        SnakeHackLayout snakeHackLayout = new SnakeHackLayout(context);
        snakeHackLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        snakeHackLayout.setAllowDragChildView(allowDragChildView);
        snakeHackLayout.addView(contentView);

        return snakeHackLayout;
    }

    private void printLog(String text) {
        Log.e("SnakeHackLayout", text);
    }

    @Override
    public void addView(View child) {
        if(getChildCount() > 0) {
            throw new IllegalStateException("SnakeHackLayout can host only one direct child. ");
        }
        super.addView(child);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mXRange = right - left;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        if(mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * 设置是否允许拖拽子视图（默认允许）
     *
     * @param allowDragChildView true 允许 false 禁止拖拽
     */
    public void setAllowDragChildView(boolean allowDragChildView) {
        mAllowDragChildView = allowDragChildView;
    }

    public void setOnEdgeDragListener(OnEdgeDragListener onEdgeDragListener) {
        this.onEdgeDragListener = onEdgeDragListener;
    }

    /**
     * 平滑移动View到指定位置
     *
     * @param view 移动的目标子视图
     * @param x 水平坐标
     * @param y 垂直坐标
     */
    public void smoothScrollTo(View view, int x, int y, final OnReleaseStateListener onReleaseStateListener) {
        if(null != view) {
            mViewDragHelper.smoothSlideViewTo(view, x, y);
            invalidate();

            this.onReleaseStateListener = onReleaseStateListener;
        }
    }

    /**
     * 平滑移动View到起始位置
     *
     * @param view 移动的目标子视图
     */
    public void smoothScrollToStart(View view, OnReleaseStateListener onReleaseStateListener) {
        smoothScrollTo(view, (int) mOriginPoint.x, (int) mOriginPoint.y, onReleaseStateListener);
    }

    /**
     * 平滑移动View到最后位置
     *
     * @param view 移动的目标子视图
     */
    public void smoothScrollToLeave(View view, OnReleaseStateListener onReleaseStateListener) {
        smoothScrollTo(view, mXRange, (int) mOriginPoint.y, onReleaseStateListener);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if(getChildCount() <= 0) return;

        canvas.save();
        int left = getChildAt(0).getLeft() - mShadowWidth;
        int top = 0;
        int right = left + mShadowWidth;
        int bottom = getHeight();

        mShadowDrawable.setBounds(left,top,right,bottom);
        mShadowDrawable.draw(canvas);

        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if(getChildCount() > 0) {
            View childView = getChildAt(0);
            FrameLayout.LayoutParams lp = (LayoutParams) childView.getLayoutParams();

            float x = childView.getX() + lp.leftMargin;
            float y = childView.getY() + lp.topMargin;

            mOriginPoint = new PointF(x, y);
        }
    }

    public interface OnEdgeDragListener {
        /**
         * 控件拖拽开始
         *
         * @param parent 父布局
         */
        void onDragStart(SnakeHackLayout parent);

        /**
         * 控件正在拖拽中
         *
         * @param parent 父布局
         * @param view 当前拖拽的View
         * @param left 距离容器左侧的距离（单位：像素）
         */
        void onDrag(SnakeHackLayout parent, View view, int left);

        /**
         * 拖拽过程被释放
         *
         * @param parent 父布局
         * @param view 当前释放的View
         * @param left 距离容器左侧的距离（单位：像素）
         * @param shouldClose true 需要关闭当前页面 false 需要还原当前页面
         */
        void onRelease(SnakeHackLayout parent, View view, int left, boolean shouldClose);
    }

    public interface OnReleaseStateListener {
        /**
         * 释放完成（页面还原或关闭完成）
         *
         * @param parent 父布局
         * @param view 当前释放的View
         */
        void onReleaseCompleted(SnakeHackLayout parent, View view);
    }
}