package com.equationl.videoshotpro.Image;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;

public class CheckPictureText {
    public final int StateCutPicture = 1000;
    public final int StateAllPicture = 1001;
    public final int StateDelPicture = 1002;


    /**
     * 二值化阀值
    * */
    private final static int BinaryzationT = 200;

    /**
    * 对比两张截图是否在同一台词时的容差
    * */
    private final static float isSameSubtitleTolerance = 0.6f;

    /**
    * 根据图片中包含的白色像素点确定是否是文字的概率
    * */
    private final static float colorLikeTextProbability= 0.05f;

    /**
    * 字幕高度
    * */
    private int subtitleHeight = 0;

    private String lastText = null;

    private int fullHeight = 0;

    private TessBaseAPI tessBaseAPI;

    private final static  String TAG = "el, in CPT";

    public CheckPictureText(Context context) {
        initTess(context);
    }

    /**
    * 获取最终的字幕高度
    * */
    public int getSubtitleHeight() {
        return subtitleHeight;
    }

    /**
     * 是否是非重复的有字幕图片
     * @param bitmap 欲检测的图片(原图)
     * */
    public int isSingleSubtitlePicture(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        fullHeight = height;
        if (subtitleHeight == 0) {
            subtitleHeight = (int)(bitmap.getHeight() * 0.75);
        }

        Log.i(TAG, "字幕高度为"+subtitleHeight);

        //预裁剪，减少遍历数
        //bitmap = Bitmap.createBitmap(bitmap, (int)(width*0.35), subtitleHeight, width-(int)(width*0.35)*2, height-subtitleHeight);
        bitmap = Bitmap.createBitmap(bitmap, 0, subtitleHeight, width, height-subtitleHeight);
        bitmap = getBinaryzationPicture(bitmap);

        String text = getOCRString(bitmap);
        Log.i(TAG, "识别到文字："+text);
        int charNum = 0;
        if (text.contains("_")) {
            charNum = text.length() - text.replaceAll("_", "").length();
        }
        if (text.equals("") || charNum > 2) {
            Log.i(TAG, "不是字幕因为没有文字");
            lastText = text;
            return StateDelPicture;
        }
        else if (lastText != null) {
            float similarity = levenshtein(text, lastText);
            Log.i(TAG, "similarity="+similarity);
            if (similarity > isSameSubtitleTolerance) {
                Log.i(TAG, "不是字幕因为两张图片一样！");
                lastText = text;
                return StateDelPicture;
            }
        }


        /*if (!isSubtitlePicture(bitmap)) {
            Log.i("el", "不是字幕因为没有文字");
            lastBitmap = bitmap;
            return StateDelPicture;
        }
        else if (lastBitmap != null) {
            if (contrastPicture(lastBitmap, bitmap)) {
                Log.i("el", "不是字幕因为两张图片一样！");
                lastBitmap = bitmap;
                return StateDelPicture;
            }
        }  */

        Log.i(TAG, "是非重复的字幕");
        lastText = text;
        return StateCutPicture;
    }

    private String getOCRString(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String text = tessBaseAPI.getUTF8Text();
        /*ArrayList<Rect> wordRects = tessBaseAPI.getRegions().getBoxRects();
        for (Rect rect : wordRects) {
            Log.i(TAG, "word rect="+rect.toString());
        }   */
        text = text.replaceAll("[^a-zA-Z_\u4e00-\u9fa5]", "");
        return text;
    }

    private void initTess(Context context) {
        String DATAPATH = context.getExternalFilesDir("tessdata").toString();
        String PATH = context.getExternalFilesDir(null).toString();
        Log.i(TAG, "PATH="+PATH);
        String DEFAULT_LANGUAGE = "chi_sim";
        tessBaseAPI = new TessBaseAPI();
        if (!tessBaseAPI.init(PATH+ File.separator, DEFAULT_LANGUAGE)) {
            //TODO
            Log.i(TAG, "init tess fail");
        }
    }

    /**
     * 检测该图片是否是有字幕图片
     * @param bitmap 欲检测的图片（二值化处理后）
    * */
    private boolean isSubtitlePicture(Bitmap bitmap) {
        if (Math.random() < 1) {    //FIXME 一半的概率检测字幕高度
            //Log.i("EL", "检测高度");
            subtitleHeight = getTextHeight(bitmap);
        }

        //Log.i("el", "图片有文字的概率====="+getColorRatio(bitmap, Color.WHITE));

        if (getColorRatio(bitmap, Color.WHITE) > colorLikeTextProbability) {
            return true;
        }
        return false;
    }


    /**
    * 对比两张截图是否拥有同一对白
    * */
    private boolean contrastPicture(Bitmap bitmap, Bitmap nextBitmap) {
        float bitmapRatio1 = getColorRatio(bitmap, Color.WHITE);
        float bitmapRatio2 = getColorRatio(nextBitmap, Color.WHITE);

        //Log.i("el", "图片相似度===="+Math.abs(bitmapRatio1-bitmapRatio2));
        if (Math.abs(bitmapRatio1-bitmapRatio2) > isSameSubtitleTolerance) {
            return true;
        }
        return false;
    }


    /**
    * 获取bitmap中指定颜色的比率
    * */
    private float getColorRatio(Bitmap bitmap, int color) {
        int x = bitmap.getWidth();
        int y = bitmap.getHeight();
        int colorNums = 0;

        for (int i=0;i<x;i++) {
            for (int j=0;j<y;j++) {
                if (bitmap.getPixel(i , j) == color) {
                    colorNums++;
                }
            }
        }
        return (float)colorNums/(x*y);
    }


    /**
    * 获取字幕高度
     * @param bitmap 处理后的bitmap（预剪切，二值化）
    * */
    private int getTextHeight(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int linneNotText = 0;
        int temp = 0;

        for (int i=height-1;i>0;i--) {
            if (!checkLineIsText(bitmap, i)) {
                linneNotText++;
            }
            else {
                linneNotText = 0;
                temp = i;
            }
            //Log.i("el", "temp========== "+temp+" fullHeight=== "+fullHeight);
            if (linneNotText > 20) {
                int tempHeight = subtitleHeight + temp;
                if (tempHeight >= fullHeight*0.6) {
                    //Log.i("el", "改变字幕高度, temp="+temp);
                    subtitleHeight = tempHeight;
                }
                break;
            }
        }

        return subtitleHeight;
    }

    private boolean checkLineIsText(Bitmap bitmap, int height) {
        int colorNums = 0;
        for (int i=0;i<bitmap.getWidth();i++) {
            if (bitmap.getPixel(i, height) == Color.WHITE) {
                colorNums++;
            }
        }
        if ((float)colorNums/bitmap.getWidth() > 0.1) {   //FIXME 还可以优化
            //Log.i("el", "检测行是文字！");
            return true;
        }
        return false;
    }

    /**
    * 二值化bitmap（固定阀值法）
     * source: https://blog.csdn.net/huangxin388/article/details/80229103
    * */
    private Bitmap getBinaryzationPicture(Bitmap bitmap) {
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


    /*
    * from: https://wdhdmx.iteye.com/blog/1343856
    * */
    public static float levenshtein(String str1,String str2) {
        //计算两个字符串的长度。
        int len1 = str1.length();
        int len2 = str2.length();
        //建立上面说的数组，比字符长度大一个空间
        int[][] dif = new int[len1 + 1][len2 + 1];
        //赋初值，步骤B。
        for (int a = 0; a <= len1; a++) {
            dif[a][0] = a;
        }
        for (int a = 0; a <= len2; a++) {
            dif[0][a] = a;
        }
        //计算两个字符是否一样，计算左上的值
        int temp;
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    temp = 0;
                } else {
                    temp = 1;
                }
                //取三个值中最小的
                dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
                        dif[i - 1][j] + 1);
            }
        }
        //计算相似度
        float similarity =1 - (float) dif[len1][len2] / Math.max(str1.length(), str2.length());
        return similarity;
    }

    //得到最小值
    private static int min(int... is) {
        int min = Integer.MAX_VALUE;
        for (int i : is) {
            if (min > i) {
                min = i;
            }
        }
        return min;
    }

}
