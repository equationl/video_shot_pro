package com.equationl.videoshotpro.Image;


import android.graphics.Bitmap;
import android.util.Log;

public class CheckPictureText {
    /**
     * 二值化阀值
    * */
    private final static int BinaryzationT = 150;

    /**
    * 对比两张截图是否在同一台词时的容差
    * */
    private final static int isSameSubtitleTolerance = 20;

    /**
    * 字幕高度
    * */
    private int subtitleHeight = 0;



    /**
    * 对比两张截图是否拥有同一对白
    * */
    public boolean contrastPicture(Bitmap Bitmap, Bitmap nextBitmap) {
        //TODO
        return true;
    }


    /**
    * 获取bitmap中指定颜色的比率
    * */
    public float getColorRatio(Bitmap bitmap, int color) {
        //TODO
        return 0;
    }

    /**
     * 裁切bitmap中有字幕部分
    * */
    public Bitmap getTextPicture(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        //if (subtitleHeight == 0) {  //字幕高度尚未初始化   //FIXME DEBUG用，记得把注释删掉
            subtitleHeight = (int)(height * 0.75);
        //}
        Log.i("el", "height="+height+" subtitleheight="+subtitleHeight);
        Bitmap textPicture = Bitmap.createBitmap(bitmap, 0, subtitleHeight, width, height-subtitleHeight);
        textPicture = getBinaryzationPicture(textPicture);

        //TODO 检测图片颜色值，随时调整字幕高度

        return textPicture;
    }

    /**
    * 二值化bitmap（固定阀值法）
     * source: https://blog.csdn.net/huangxin388/article/details/80229103
    * */
    public Bitmap getBinaryzationPicture(Bitmap bitmap) {
        //得到图形的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        //创建二值化图像
        Bitmap binarymap = null;
        binarymap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        //依次循环，对图像的像素进行处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //得到当前像素的值
                int col = binarymap.getPixel(i, j);
                //得到alpha通道的值
                int alpha = col & 0xFF000000;
                //得到图像的像素RGB的值
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);
                // 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                //对图像进行二值化处理
                if (gray <= BinaryzationT) {
                    gray = 0;
                } else {
                    gray = 255;
                }
                // 新的ARGB
                int newColor = alpha | (gray << 16) | (gray << 8) | gray;
                //设置新图像的当前像素值
                binarymap.setPixel(i, j, newColor);
            }
        }
        return binarymap;
    }

}
