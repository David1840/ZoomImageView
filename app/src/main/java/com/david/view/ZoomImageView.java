package com.david.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by David on 17/3/19.
 */

public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private boolean mOnce = false;

    //初始化时缩放的值
    private float mInitScale;
    //双击放大时到达的值
    private float mMidScale;
    //放大的最大值
    private float mMaxScale;

    private Matrix mMatrix;

    //捕获用户多点触控时缩小或放大的比例
    private ScaleGestureDetector mScaleGestureDetector;

    //-------自由移动
    private int mLastPointerCount;

    private float mLastX;
    private float mLastY;

    private int mTouchSlop;
    private boolean isCanDrag;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //-----双击放大缩小
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //设置图片大小

        mMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);

        mScaleGestureDetector = new ScaleGestureDetector(context, this);

        setOnTouchListener(this);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (isAutoScale) {
                            return true;
                        }
                        float x = e.getX();
                        float y = e.getY();
                        if (getScale() < mMidScale) {
//                            mMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
//                            setImageMatrix(mMatrix);
                            postDelayed(new AutoScaleRunnable(mMidScale, x, y), 20);
                            isAutoScale = true;
                        } else {
//                            mMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
//                            setImageMatrix(mMatrix);
                            postDelayed(new AutoScaleRunnable(mInitScale, x, y), 20);
                            isAutoScale = true;
                        }


                        return true;
                    }
                });
    }

    /**
     * 自动放大与缩小
     */
    private class AutoScaleRunnable implements Runnable {

        private float mTargetScale;
        //中心点
        private float x;
        private float y;

        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        private float tmpScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if (getScale() < mTargetScale) {
                tmpScale = BIGGER;
            }
            if (getScale() > mTargetScale) {
                tmpScale = SMALL;
            }
        }

        @Override
        public void run() {
            mMatrix.postScale(tmpScale, tmpScale, x, y);
            checkBorderAndCenter();
            setImageMatrix(mMatrix);
            if ((tmpScale > 1.0f && getScale() < mTargetScale) || (tmpScale < 1.0f && getScale() > mTargetScale)) {
                postDelayed(this, 20);
            } else {
                float scale = mTargetScale / getScale();
                mMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenter();
                setImageMatrix(mMatrix);
                isAutoScale = false;
            }
        }
    }


    /**
     * 获取ImageView加载完成的图片
     */
    @Override
    public void onGlobalLayout() {
        if (!mOnce) {
            //得到控件的宽和高
            int width = getWidth();
            int height = getHeight();

            //得到图片的宽和高
            Drawable d = getDrawable();
            if (d == null) {
                return;
            }
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();

            //图片大小和控件宽高比较
            //默认缩放值为1.0
            float scale = 1.0f;

            //图片宽度大于控件宽度，高度低于图片高度
            if (dw > width && dh < height) {
                scale = width * 1.0f / dw;
            }

            if (dh > height && dw < width) {
                scale = height * 1.0f / dh;
            }

            if ((dh > height && dw > width) || (dw < width && dh < height)) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }
            mInitScale = scale;
            mMaxScale = mInitScale * 4;
            mMidScale = mInitScale * 2;

            /**
             * 将图片移动到当前控件的中心
             */
            //要移动的距离
            int dx = width / 2 - dw / 2;
            int dy = height / 2 - dh / 2;

            mMatrix.postTranslate(dx, dy);
            mMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
            setImageMatrix(mMatrix);

            mOnce = true;
        }

    }

    //当View显示在Window上时执行
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    //当View从Window上移除时执行
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    public float getScale() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();

        if (getDrawable() == null) {
            return true;
        }

        if ((scale < mMaxScale && scaleFactor > 1.0f)
                || (scale > mInitScale && scaleFactor < 1.0f)) {
            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale;
            }
            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale;
            }
            mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

            checkBorderAndCenter();
            setImageMatrix(mMatrix);
        }

        return true;
    }


    private RectF getMatrixRectF() {
        Matrix matrix = mMatrix;
        RectF rectf = new RectF();
        Drawable d = getDrawable();

        if (d != null) {
            rectf.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rectf);
        }
        return rectf;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }


    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;

        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        x /= pointerCount;
        y /= pointerCount;

        if (mLastPointerCount != pointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;
        RectF rectf = getMatrixRectF();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rectf.width() > getWidth() || rectf.height() > getHeight()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (rectf.width() > getWidth() + 0.01 || rectf.height() > getHeight() + 0.01) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag) {
//                    RectF rectf = getMatrixRectF();
                    if (getDrawable() != null) {
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        if (rectf.width() < getWidth()) {
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        if (rectf.height() < getHeight()) {
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }

                        mMatrix.postTranslate(dx, dy);
                        checkBorderWhenTraslate();
                        setImageMatrix(mMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                mLastPointerCount = 0;
                break;
        }


        return true;
    }


    /**
     * 在移动时进行边界检查
     */
    private void checkBorderWhenTraslate() {
        RectF rectf = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectf.top > 0 && isCheckTopAndBottom) {
            deltaY = -rectf.top;
        }
        if (rectf.bottom < height && isCheckTopAndBottom) {
            deltaY = height - rectf.bottom;
        }
        if (rectf.left > 0 && isCheckLeftAndRight) {
            deltaX = -rectf.left;
        }

        if (rectf.right < width && isCheckLeftAndRight) {
            deltaX = width - rectf.right;
        }
        mMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 在放大缩小的同时进行边界控制
     */
    private void checkBorderAndCenter() {
        RectF rectf = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectf.width() >= width) {
            if (rectf.left > 0) {
                deltaX = -rectf.left;
            }

            if (rectf.right < width) {
                deltaX = width - rectf.right;
            }
        }

        if (rectf.height() >= height) {
            if (rectf.top > 0) {
                deltaY = -rectf.top;
            }
            if (rectf.bottom < height) {
                deltaY = height - rectf.bottom;
            }
        }

        if (rectf.width() < width) {
            deltaX = width / 2f - rectf.right + rectf.width() / 2f;

        }
        if (rectf.height() < height) {
            deltaY = height / 2f - rectf.bottom + rectf.height() / 2f;
        }

        mMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {

        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }
}
