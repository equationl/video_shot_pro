

package com.equationl.videoshotpro.cropBox;

import android.graphics.PointF;
import android.support.annotation.NonNull;


/***
 * 捕获手指在裁剪框的哪一条边
 */
class CatchEdgeUtil {


    /**
     * 判断手指是否的位置是否在有效的缩放区域：缩放区域的半径为targetRadius
     * 缩放区域使指：裁剪框的四个角度或者四条边，当手指位置处在某个角
     * 或者某条边的时候，则随着手指的移动对裁剪框进行缩放操作。
     * 如果手指位于裁剪框的内部，则裁剪框随着手指的移动而只进行移动操作。
     * 否则可以判定手指距离裁剪框较远而什么都不做
     */
    static CropWindowEdgeSelector getPressedHandle(float x,
                                                   float y,
                                                   float left,
                                                   float top,
                                                   float right,
                                                   float bottom,
                                                   float targetRadius) {

        //////////判断手指是否在图二种的C位置：四个边的某条边/////////////////
        if (CatchEdgeUtil.isInHorizontalTargetZone(x, y, left, right, top, targetRadius)) {
            return CropWindowEdgeSelector.TOP;//说明手指在裁剪框top区域
        } else if (CatchEdgeUtil.isInHorizontalTargetZone(x, y, left, right, bottom, targetRadius)) {
            return CropWindowEdgeSelector.BOTTOM;//说明手指在裁剪框bottom区域
        }

        //////////判断手指是否在图二种的B位置：裁剪框的中间/////////////////
        if (isWithinBounds(x, y, left, top, right, bottom)) {
            return CropWindowEdgeSelector.CENTER;
        }

        ////////手指位于裁剪框的D位置，此时移动手指什么都不做/////////////
        return null;
    }

    static void getOffset(@NonNull CropWindowEdgeSelector cropWindowEdgeSelector,
                          float x,
                          float y,
                          float left,
                          float top,
                          float right,
                          float bottom,
                          @NonNull PointF touchOffsetOutput) {

        float touchOffsetX = 0;
        float touchOffsetY = 0;

        switch (cropWindowEdgeSelector) {

            case TOP_LEFT:
                touchOffsetX = left - x;
                touchOffsetY = top - y;
                break;
            case TOP_RIGHT:
                touchOffsetX = right - x;
                touchOffsetY = top - y;
                break;
            case BOTTOM_LEFT:
                touchOffsetX = left - x;
                touchOffsetY = bottom - y;
                break;
            case BOTTOM_RIGHT:
                touchOffsetX = right - x;
                touchOffsetY = bottom - y;
                break;
            case LEFT:
                touchOffsetX = left - x;
                touchOffsetY = 0;
                break;
            case TOP:
                touchOffsetX = 0;
                touchOffsetY = top - y;
                break;
            case RIGHT:
                touchOffsetX = right - x;
                touchOffsetY = 0;
                break;
            case BOTTOM:
                touchOffsetX = 0;
                touchOffsetY = bottom - y;
                break;
            case CENTER:
                final float centerX = (right + left) / 2;
                final float centerY = (top + bottom) / 2;
                touchOffsetX = centerX - x;
                touchOffsetY = centerY - y;
                break;
        }

        touchOffsetOutput.x = touchOffsetX;
        touchOffsetOutput.y = touchOffsetY;
    }


    private static boolean isInHorizontalTargetZone(float x,
                                                    float y,
                                                    float handleXStart,
                                                    float handleXEnd,
                                                    float handleY,
                                                    float targetRadius) {

        return (x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius);
    }


    private static boolean isWithinBounds(float x, float y, float left, float top, float right, float bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

}
