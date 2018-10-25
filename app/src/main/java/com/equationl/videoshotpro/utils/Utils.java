package com.equationl.videoshotpro.utils;


import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;


public class Utils {
    public static void fitsSystemWindows(boolean isTranslucentStatus, View view) {
        if (isTranslucentStatus) {
            view.getLayoutParams().height = calcStatusBarHeight(view.getContext());
        }
    }

    public static int calcStatusBarHeight(Context context) {
        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    public void finishActivity(Activity context) {
        try {
            context.finish();
        } catch (NullPointerException e) {
            Log.e("el, in finishActivity", e.toString());
        }
    }
}
