package com.equationl.videoshotpro.utils;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.equationl.videoshotpro.R;

public class ChooseTagView extends android.support.v7.widget.AppCompatImageView {
    private Drawable deleteIcon;
    private int iconWidth;
    private int iconHeight;
    private boolean showIcon = true;
    private Rect mDelteRect;
    private Rect mAssumeDelteRect;

    public ChooseTagView(Context context) {
        super(context);
//        int id = context.getResources().getIdentifier("ic_delete", "drawable", context.getPackageName());
        deleteIcon = context.getResources().getDrawable(R.drawable.ic_delete);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mAssumeDelteRect == null) {
            setDeleteBounds();
        }
        if (showIcon) {
            deleteIcon.draw(canvas);
        }
    }

    private void setDeleteBounds() {
        iconWidth = deleteIcon.getIntrinsicWidth();
        iconHeight = deleteIcon.getIntrinsicHeight();
        int left = getWidth() - iconWidth;
        int top = 0;
        mDelteRect = new Rect(left, top, left + iconWidth, top + iconHeight);
        //padding扩大了icon的点击范围
        int padding = dip2px(getContext(), 10);
        mAssumeDelteRect = new Rect(mDelteRect.left, mDelteRect.top, mDelteRect.left + iconWidth + padding, mDelteRect.top + iconHeight + padding);
        deleteIcon.setBounds(mDelteRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (mAssumeDelteRect == null) {
            setDeleteBounds();
        }
        boolean contains = mAssumeDelteRect.contains(x, y);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (contains && showIcon) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (contains && showIcon) {
                    if (mListener != null) {
                        mListener.onDelete(this);
                    }
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void log(String msg) {
        Log.e(getClass().getCanonicalName(), msg);
    }

    public void showDeleteIcon(boolean show) {
        showIcon = show;
        invalidate();
    }

    private OnTagDeleteListener mListener;

    public void setOnTagDeleteListener(OnTagDeleteListener listener) {
        mListener = listener;
    }

    public interface OnTagDeleteListener {
        /**
         * Delete view.
         *
         * @param deleteView
         */
        void onDelete(View deleteView);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private static int dip2px(Context context, float dpValue) {
        final float scale = getScale(context);
        return (int) (dpValue * scale + 0.5f);
    }
    private static float getScale(Context context) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return findScale(fontScale);
    }
    private static float findScale(float scale){
        if(scale<=1){
            scale=1;
        }else if(scale<=1.5){
            scale=1.5f;
        }else if(scale<=2){
            scale=2f;
        }else if(scale<=3){
            scale=3f;
        }
        return scale;
    }

}
