package com.equationl.videoshotpro.cropBox;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by mac on 17/12/21.
 */

class UIUtil {
    static int dip2px(@NonNull Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
