package com.equationl.videoshotpro.Image;


import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.tencent.bugly.crashreport.CrashReport;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

public class Tools{

    public int AllowNotBlackNums = 20;
    public int AllowCheckBlackLines = 10;

    private static final String TAG = "EL,In Tools";

    /**
     * 将 bitmap 保存为图片文件（开启压缩保存为jpg，否则为png）
     * @param bmp bitmap
     * @param bitName 保存文件名（不含扩展名）
     * @param savePath 保存路径
     * @param isReduce 是否压缩
     * @param quality 图片质量
     *
     * @return File 返回保存的文件
     * */
    public File saveBitmap2File(Bitmap bmp, String bitName, File savePath, boolean isReduce, int quality) throws Exception {
        File f;
        Bitmap.CompressFormat imgFormat;

        if (isReduce) {
            f = new File(savePath,bitName + ".jpg");
            imgFormat = Bitmap.CompressFormat.JPEG;
        }
        else {
            f = new File(savePath,bitName + ".png");
            imgFormat = Bitmap.CompressFormat.PNG;
        }

        if (!f.createNewFile()) {
            Log.w(TAG, "file "+f+"has already exist");
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
            bmp.compress(imgFormat, quality, fOut);
        }  catch (Exception e) {
            Log.e(TAG, "save file fail:"+e);
            throw e;
        }
        finally {
            try {
                if (fOut != null) {
                    fOut.flush();
                    fOut.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close file fail"+e);
            }
        }

        return f;
    }

    /**
     * 将 bitmap 保存为图片文件（开启压缩保存为jpg，否则为png）
     * @param bmp bitmap
     * @param bitName 保存文件名（不含扩展名）
     * @param savePath 保存路径
     *
     * @return File 返回保存的文件
     * */
    public File saveBitmap2File(Bitmap bmp, String bitName, File savePath) throws Exception {
        return this.saveBitmap2File(bmp, bitName, savePath, false, 100);
    }

    /**
     * 拼接两个 bitmap
     *
     * @return Bitmap 返回拼接后的Bitmap, 如果拼接失败返回 null
     * */
    public Bitmap jointBitmap(Bitmap first, Bitmap second) {
        if (first == null || second == null){
            return null;
        }
        int width = Math.max(first.getWidth(),second.getWidth());
        int height = first.getHeight() + second.getHeight();

        Log.i(TAG, "in jointBitmap, width="+width+" height="+height);
        Log.i(TAG, "in jointBitmap, first.height="+first.getHeight());

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(first, 0, 0, null);
        canvas.drawBitmap(second, 0, first.getHeight(), null);
        return result;
    }

    public Bitmap jointTextImage(Bitmap textImg, Bitmap bgImg, int x, int y) {
        int width = Math.max(bgImg.getWidth(), textImg.getWidth());  //FIXME 应该修改为保证文字图片不会大于背景图片的宽度
        int height = Math.max(bgImg.getHeight(), textImg.getHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bgImg, 0, 0, null);
        canvas.drawBitmap(textImg, x, y, null);
        return bitmap;
    }

    /**
     * 从文件获取图片的bitmap
     * */
    public Bitmap getBitmapFromFile(String no, File dirPath, String extension) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        return getBitmapFromFile(no, dirPath, extension, options);
    }

    public Bitmap getBitmapFromFile(String no, File dirPath, String extension, BitmapFactory.Options options) throws Exception {
        File path = new File(dirPath, no+"."+extension);
        FileInputStream f;
        Bitmap bm;
        f = new FileInputStream(path);
        BufferedInputStream bis = new BufferedInputStream(f);
        bm = BitmapFactory.decodeStream(bis, null, options);

        return bm;
    }

    public Bitmap getBitmapFromFile(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(filePath);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            return BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
        } catch (Exception ex) {
            CrashReport.postCatchedException(ex);
        }
        return null;
    }


    /**
     * 获取图片的尺寸
     * */
    public float[] getBitmapSize(String no, File dirPath, String extension) {
        File path = new File(dirPath, no+"."+extension);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
        } catch (Exception e) {
            CrashReport.postCatchedException(e);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
        } catch (Exception e) {
            CrashReport.postCatchedException(e);
        }
        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        float size[] = {srcWidth, srcHeight};

        return size;
    }

    /**
     * 获取缩放后的本地图片
     *
     * <br /><br />by jianshu 闲庭CC
     *
     * @param filePath 文件路径
     * @param width    宽
     * @param height   高
     * @return
     */
    public Bitmap getBitmapThumbnailFromFile(String filePath, int width, int height) {
        try {
            FileInputStream fis = new FileInputStream(filePath);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
            float srcWidth = options.outWidth;
            float srcHeight = options.outHeight;
            int inSampleSize = 1;

            if (srcHeight > height || srcWidth > width) {
                if (srcWidth > srcHeight) {
                    inSampleSize = Math.round(srcHeight / height);
                } else {
                    inSampleSize = Math.round(srcWidth / width);
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;

            return BitmapFactory.decodeFileDescriptor(fis.getFD(), null, options);
        } catch (Exception ex) {
            CrashReport.postCatchedException(ex);
        }
        return null;
    }

    /**
    * by jianshu 闲庭CC
    * */
    public Bitmap drawableToBitmap(int drawbleId, Context context) {
        Drawable drawable  = context.getResources().getDrawable(drawbleId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 裁切图片
     * */
    public Bitmap cutBitmap(Bitmap bm, int startY, int width) {
        int height = bm.getHeight()-startY;
        if (height <= 0) {
            height = (int)(bm.getHeight()-bm.getHeight()*0.8);
            startY = (int)(bm.getHeight()*0.8);
        }
        if (width>bm.getWidth()) {
            width = bm.getWidth();
        }
        return Bitmap.createBitmap(bm, 0, startY, width, height);
    }


    /**
     * 根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换
     * @author yaoxing
     * @date 2014-10-12
     */
    public String getImageAbsolutePath(Activity context, Uri imageUri) {
        if (context == null || imageUri == null)
            return null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                Log.i(TAG, "isDocumentUri isExternalStorageDocument");
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            else if (isDownloadsDocument(imageUri)) {
                Log.i(TAG, "isDocumentUri isDownloadsDocument");
                String id = DocumentsContract.getDocumentId(imageUri);

                //解决华为手机URI不规范的问题
                if (id.startsWith("raw:")) {
                    final String path = id.replaceFirst("raw:", "");
                    return path;
                }

                Uri contentUri = imageUri;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                }
                return getDataColumn(context, contentUri, null, null);
            }
            else if (isMediaDocument(imageUri)) {
                Log.i(TAG, "isDocumentUri isMediaDocument");
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            Log.i(TAG, "is content");
            // Return the remote address
            if (isGooglePhotosUri(imageUri))
                return imageUri.getLastPathSegment();
            try {   //7.0 之前
                return getDataColumn(context, imageUri, null, null);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            return getFPUriToPath(context, imageUri);   //7.0 之后
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            Log.i(TAG, "is file");
            return imageUri.getPath();
        }
        return null;
    }

    private static String getFPUriToPath(Context context, Uri uri) {
        // FIXME 随时可能失效
        Log.i(TAG, "call getFPUriToPath");
        try {
            List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
            if (packs != null) {
                String fileProviderClassName = FileProvider.class.getName();
                for (PackageInfo pack : packs) {
                    ProviderInfo[] providers = pack.providers;
                    if (providers != null) {
                        for (ProviderInfo provider : providers) {
                            if (uri.getAuthority().equals(provider.authority)) {
                                if (provider.name.equalsIgnoreCase(fileProviderClassName)) {
                                    Class<FileProvider> fileProviderClass = FileProvider.class;
                                    try {
                                        Method getPathStrategy = fileProviderClass.getDeclaredMethod("getPathStrategy", Context.class, String.class);
                                        getPathStrategy.setAccessible(true);
                                        Object invoke = getPathStrategy.invoke(null, context, uri.getAuthority());
                                        if (invoke != null) {
                                            String PathStrategyStringClass = FileProvider.class.getName() + "$PathStrategy";
                                            Class<?> PathStrategy = Class.forName(PathStrategyStringClass);
                                            Method getFileForUri = PathStrategy.getDeclaredMethod("getFileForUri", Uri.class);
                                            getFileForUri.setAccessible(true);
                                            Object invoke1 = getFileForUri.invoke(invoke, uri);
                                            if (invoke1 instanceof File) {
                                                String filePath = ((File) invoke1).getAbsolutePath();
                                                return filePath;
                                            }
                                        }
                                    } catch (NoSuchMethodException e) {
                                        Log.i(TAG, Log.getStackTraceString(e));
                                    } catch (InvocationTargetException e) {
                                        Log.i(TAG, Log.getStackTraceString(e));
                                    } catch (IllegalAccessException e) {
                                        Log.i(TAG, Log.getStackTraceString(e));
                                    } catch (ClassNotFoundException e) {
                                        Log.i(TAG, Log.getStackTraceString(e));
                                    }
                                    break;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, Log.getStackTraceString(e));
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    public Boolean copyFileToCache(List <String> files, String save_path, String extension) {
        int i = 0;
        try {
            for (String path : files) {
                try {
                    copyFile(new File(path), new File(save_path + "/" + + i + "." + extension));
                } catch (IOException e) {
                    return false;
                }
                i++;
            }
        } catch (ConcurrentModificationException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    public void copyDir(String sourcePath, String newPath) throws IOException {
        File file = new File(sourcePath);
        String[] filePath = file.list();

        if (!(new File(newPath)).exists()) {
            (new File(newPath)).mkdir();
        }

        for (int i = 0; i < filePath.length; i++) {
            if (new File(sourcePath  + file.separator + filePath[i]).isFile()) {
                copyFile(new File(sourcePath + file.separator + filePath[i]),
                        new File(newPath + file.separator + filePath[i]));
            }
        }
    }

    public void copyFile(File fromFile,File toFile) throws IOException {
        FileInputStream ins = new FileInputStream(fromFile);
        FileOutputStream out = new FileOutputStream(toFile);
        byte[] b = new byte[1024];
        int n=0;
        while((n=ins.read(b))!=-1){
            out.write(b, 0, b.length);
        }

        ins.close();
        out.close();
    }

    public void deleteFile(File file) {
        file.delete();
    }

    public void deleteDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            for (File item : directory.listFiles()) {
                item.delete();
            }
        }
        directory.delete();
    }

    public void cleanExternalCache(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            deleteFilesByDirectory(context.getExternalCacheDir());
        }
    }

    private static void deleteFilesByDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            for (File item : directory.listFiles()) {
                item.delete();
            }
        }
    }

    public int getInverseColorCode(int source) {
        if (source>=0 && source<64) {
            return source+192;
        }
        else if (source>=64 && source<128) {
            return source+64;
        }
        else if (source>=128 && source<192) {
            return source-64;
        }
        else if (source>=192 && source<=255) {
            return  source-192;
        }
        return -1;
    }

    public int getInverseColor(int source) {
        return Color.rgb(
                getInverseColorCode(Color.red(source)),
                getInverseColorCode(Color.green(source)),
                getInverseColorCode(Color.blue(source))
                );
    }

    public Bitmap addRight(Bitmap base) {
        Canvas canvas = new Canvas(base);
        Paint paint= new Paint();
        int width = base.getWidth();
        int height = base.getHeight();

        paint.setColor(base.getPixel(1,0));
        canvas.drawPoint(0, 0, paint);   //0

        paint.setColor(getInverseColor(base.getPixel(1,1)));
        canvas.drawPoint(0, 1, paint);   //1

        paint.setColor(base.getPixel(width-2, height-2));
        canvas.drawPoint(width-2, height-1, paint);   //0

        paint.setColor(base.getPixel(width-1, height-2));
        canvas.drawPoint(width-1, height-1, paint);   //0

        paint.setColor(base.getPixel(width-2, 1));
        canvas.drawPoint(width-2, 0, paint);   //0

        paint.setColor(getInverseColor(base.getPixel(width-1, 1)));
        canvas.drawPoint(width-1, 0, paint);   //1

        paint.setColor(base.getPixel(0, height-2));
        canvas.drawPoint(0, height-1, paint);   //0

        paint.setColor(getInverseColor(base.getPixel(1, height-2)));
        canvas.drawPoint(1, height-1, paint);   //1

        return base;
    }

    public Boolean checkRight(Bitmap base) {
        int width = base.getWidth();
        int height = base.getHeight();


        if (base.getPixel(1,0) != base.getPixel(0,0)) {
            return false;
        }

        else if (base.getPixel(1,1) != getInverseColor(base.getPixel(0,1))) {
            return false;
        }

        else if (base.getPixel(width-2, height-1) != base.getPixel(width-2, height-2)) {
            return false;
        }

        else if (base.getPixel(width-1, height-1) != base.getPixel(width-1, height-2)) {
            return false;
        }

        else if (base.getPixel(width-2, 0) != base.getPixel(width-2, 1)) {
            return false;
        }

        else if (base.getPixel(width-1, 0) != getInverseColor(base.getPixel(width-1, 1))) {
            return false;
        }

        else if (base.getPixel(0, height-1) != base.getPixel(0, height-2)) {
            return false;
        }

        else if (base.getPixel(1, height-1) != getInverseColor(base.getPixel(1, height-2))) {
            return false;
        }

        return true;
    }

    public void MakeCacheToStandard(Context context) {



        File path = new File(context.getExternalCacheDir().toString());
        String files[] = path.list();

        for (String file:files) {
            Log.i("EL", "file="+file);
            if (file.contains("_")) {
                /*String NewName = file.split("_")[0]+"."+file.split("\\.")[1];
                Log.i("EL", file+" rename to "+NewName);
                renameFile(path.getPath(), file, NewName);   */
                File f = new File(path+"/"+file);
                if (!f.delete()) {
                    Log.w("el", "delete file fail"+f);
                }
            }
        }
    }

    public Boolean renameFile(String oldname,String newname) {
        if(!oldname.equals(newname)){
            File oldfile=new File(oldname);
            File newfile=new File(newname);
            if(newfile.exists()) {
                Log.i(TAG, "rename file fail:file is already exist");
                return false;
            }
            else {
                if (!oldfile.renameTo(newfile)) {
                    Log.i(TAG, "rename file fail: unknown reason");
                    return false;
                }
            }
            /*if(!oldfile.exists()){
                Log.i("EL", "wrong in rename: 文件不存在");
                return false;
            }
            if(newfile.exists()) {//若在该目录下已经有一个文件和新文件名相同，则不允许重命名
                Log.i("EL", "wrong in rename: 文件名已存在");
                return false;
            }
            else{
                oldfile.renameTo(newfile);
            }
        }else{
            Log.i("EL", "wrong in rename: 文件名相同");
            return false;   */
        }

        return true;
    }

    /**
     * 去除图片中的黑色无内容区域
     *
     * @param  bitmap 源图片
     * @param isJpg 传入图片是否是jpg格式
     * @return 处理完成的bitmap
     *
     * */
    public Bitmap removeImgBlackSide(Bitmap bitmap, Boolean isJpg) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Log.i(TAG, "width="+width+" height="+height);

        int[] area = getImgBlackArea(bitmap, isJpg);

        Log.i(TAG, "area="+area[0]+" "+area[1]);

        if (area[0] > 0) {
            bitmap = Bitmap.createBitmap(bitmap, 0, area[0], width, height-area[0]);
        }
        if (area[1] > 0) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, bitmap.getHeight()-(height-area[1]));
        }
        return bitmap;
    }


    /**
     * 检查指定行是否为无内容区域（仅检查横向）
     *
     * @param bitmap 源bitmap
     *@param y 欲检查的行的坐标
     * @return 是否为无内容区域
     *
     * */
    private boolean checkLineColorIsBlack(Bitmap bitmap, int y, boolean isjpg) {
        int len = bitmap.getWidth();
        int NotBlackNum = 0;
        int color;
        for (int i=0;i<len;i+=2) {    //空一个像素检测一次，节省时间
            color = bitmap.getPixel(i, y);
            //Log.i("nidaye", "i="+i+" y="+y);
            //Log.i("nidaye", "color="+color);
            //Log.i("el", "ALPHA="+Color.alpha(color));
            if (isjpg) {
                if (color != Color.BLACK || Color.alpha(color) != 255) {
                    NotBlackNum++;
                }
            }
            else {
                if (Color.alpha(color) == 255 |   //不是透明边
                        (color != Color.BLACK & Color.alpha(color) == 255)) {   //不是黑边
                    NotBlackNum++;
                }
            }
        }

        if (NotBlackNum > AllowNotBlackNums) {
            return false;
        }
        return true;
    }

    /**
     * 获取无内容区域
     *
     * @param bitmap 源bitmap
     * @return 无内容区域的终点坐标
     * */
    private int[] getImgBlackArea(Bitmap bitmap, boolean isJpg) {
        int i = 0;
        int temp1, temp2, blackLineNums=0;
        Boolean lineIsBlack = checkLineColorIsBlack(bitmap, i, isJpg);
        while (true) {
            if (i >= bitmap.getHeight()/2) {
                Log.i(TAG, "i >= bitmap.getHeight()/2");
                i = AllowCheckBlackLines;
                break;
            }
            if (!lineIsBlack) blackLineNums++;
            else {
                blackLineNums = 0;
            }
            if (blackLineNums >= AllowCheckBlackLines) break;
            i++;
            lineIsBlack = checkLineColorIsBlack(bitmap, i, isJpg);
        }
        temp1 = i-AllowCheckBlackLines;
        temp2 = getImgBlackAreaDown(bitmap, isJpg);

        return new int[]{temp1, temp2};
    }

    private int getImgBlackAreaDown(Bitmap bitmap, boolean isJpg) {
        int blackLineNums = 0;
        boolean lineIsBlack;
        int i = bitmap.getHeight()-1;
        //i = (int)((bitmap.getHeight())*0.6);
        Log.i("cao", "i="+i);
        lineIsBlack = checkLineColorIsBlack(bitmap, i, isJpg);
        while (true) {
            Log.i(TAG, "i="+i+" lineIsBlack="+lineIsBlack);
            if (i <= bitmap.getHeight()/2) {
                Log.i(TAG, "break case i out");
                i = bitmap.getHeight();
                break;
            }
            if (!lineIsBlack) {
                blackLineNums++;
                Log.i(TAG, "lineIsBlack:true");
            }
            else {
                blackLineNums = 0;
            }
            if (blackLineNums >= AllowCheckBlackLines) {
                Log.i(TAG, "break: blackLineNums="+blackLineNums+"AllowCheckBlackLines="+AllowCheckBlackLines);
                break;
            }
            i--;
            lineIsBlack = checkLineColorIsBlack(bitmap, i, isJpg);
        }
        Log.i(TAG, "i="+i+" lineIsBlack="+lineIsBlack);

        Log.i(TAG, "AllowCheckBlackLines="+AllowCheckBlackLines);
        return i+AllowCheckBlackLines;
    }


    /**
    * 获取imageview实际绘制的图片大小
     *
     * @param imageview 欲获取的imgaeview
     * @return 返回实际大小数组
    * */
    public int[] getImageRealSize(ImageView imageview) {
        int realImgShowWidth=0;
        int realImgShowHeight=0;
        Drawable imgDrawable = imageview.getDrawable();
        if (imgDrawable != null) {
            //获得ImageView中Image的真实宽高，
            int dw = imageview.getDrawable().getBounds().width();
            int dh = imageview.getDrawable().getBounds().height();

            //获得ImageView中Image的变换矩阵
            Matrix m = imageview.getImageMatrix();
            float[] values = new float[10];
            m.getValues(values);

            //Image在绘制过程中的变换矩阵，从中获得x和y方向的缩放系数
            float sx = values[0];
            float sy = values[4];

            //计算Image在屏幕上实际绘制的宽高
            realImgShowWidth = (int) (dw * sx);
            realImgShowHeight = (int) (dh * sy);
        }
        int size[] = {realImgShowWidth, realImgShowHeight};
        return size;
    }


    /**
     * 将缓存的图片顺序重置为用户自定义顺序
     *
     * @param newFileName 新的图片图片顺序
     * @param context Context
     * */
    public void sortCachePicture(List <String> newFileName, Context context) {
        rename(newFileName, context);
        String path = context.getExternalCacheDir().toString();
        for (int i=0; i<newFileName.size(); i++) {
            String newFile = path+"/"+newFileName.get(i);
            renameFile(newFile+"_c",
                    path+"/"+i+"."+newFileName.get(i).split("\\.")[1]);
        }
    }

    /**
    * 避免重名
     *
    * */
    private void rename(List <String> newFileName, Context context) {
        String[] files;
        String path = context.getExternalCacheDir().toString();
        files = getFileOrderByName(path, 1);
        if (files.length != newFileName.size()) {
            Toast.makeText(context, "重命名文件错误：新旧文件数量不一致!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "重命名文件错误：新旧文件数量不一致");
            return;
        }
        for (int i=0; i<newFileName.size(); i++) {
            String oldFile = path+"/"+files[i]+"_c";
            renameFile(path+"/"+files[i], oldFile);
            Log.i(TAG, "t old:"+path+"/"+files[i]+" new"+oldFile);
        }
    }


    /**
    * 获取按照文件名排序的指定路径下所有文件（夹）名
     *
     * @param filePath 路径
     * @param sort 排序方式（正序：1， 倒序：-1）
     * @param isDirFirst 是否文件夹优先
     * @return 返回文件名数组
    * */
    public String[] getFileOrderByName(String filePath, final int sort, final boolean isDirFirst) {
        List <File> files;
        try {
            files = Arrays.asList(new File(filePath).listFiles());
        } catch(NullPointerException e) {
            CrashReport.postCatchedException(e);
            return new String[0];
        }
        Collections.sort(files, new Comparator< File>() {
            @Override
            public int compare(File o1, File o2) {
                long i1 = getFileNameToLong(o1);
                long i2 = getFileNameToLong(o2);
                if (isDirFirst) {
                    if (o1.isDirectory() && o2.isFile())
                        return -1;
                    if (o1.isFile() && o2.isDirectory())
                        return 1;
                }
                if (i1 > i2)
                    return sort==1 ? 1:-1;
                if (i1 < i2)
                    return sort==1 ? -1:1;
                if (i1 == i2)
                    return 0;
                return o1.getName().compareTo(o2.getName());
            }
        });
        String[] array = new String[files.size()];
        for (int i=0; i<files.size(); i++) {
            array[i] = files.get(i).getName();
        }
        return array;
    }

    /**
     * 获取按照文件名排序的指定路径下所有文件（夹）名
     *
     * @param filePath 路径
     * @param sort 排序方式（正序：1， 倒序：-1）
     * @return 返回文件名数组
     * */
    public String[] getFileOrderByName(String filePath, final int sort) {
        return getFileOrderByName(filePath, sort, true);
    }

    public String[] getFileOrderByType(String filePath, final boolean isDirFirst) {
        List <File> files;
        try {
            files = Arrays.asList(new File(filePath).listFiles());
        } catch(NullPointerException e) {
            CrashReport.postCatchedException(e);
            return new String[0];
        }
        Collections.sort(files, new Comparator< File>() {
            @Override
            public int compare(File o1, File o2) {
                String t1="",t2="";
                if (!o1.isDirectory())
                    t1 = getFileType(o1);
                if (!o2.isDirectory())
                    t2 = getFileType(o2);
                if (isDirFirst) {
                    if (o1.isDirectory() && o2.isFile())
                        return -1;
                    if (o1.isFile() && o2.isDirectory())
                        return 1;
                }
                if (t1.equals(t2))
                    return 0;

                return t1.compareTo(t2);
            }
        });
        String[] array = new String[files.size()];
        for (int i=0; i<files.size(); i++) {
            array[i] = files.get(i).getName();
        }
        return array;
    }

    private String getFileType(File file) {
        String fileName=file.getName();
        return fileName.substring(fileName.lastIndexOf("."),fileName.length());
    }

    private long getFileNameToLong(File f1) {
        /*String s = f1.getName();
        s = s.split("\\.")[0];     //去除扩展名

        s = s.replaceAll("[^\\d]+", "");

        if (s.contains("_")) {
            s = s.split("_")[0];
        }   */

        String s = f1.getName();
        s = s.replaceAll("[^\\d]+", "");   //去除非数字字符
        long i=-1;
        try {
            i = Long.parseLong(s);
        }
        catch (Exception e) {
            CrashReport.postCatchedException(e);
            Log.e("el,in tools", e.toString());
        }

        //Log.i("el,in tools", "in getFileNameToInt, i="+i);
        return  i;
    }



    public Uri getUriFromFile(File file, Context context) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context,
                    context.getApplicationContext().getPackageName() + ".fileprovider",
                    file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    public int createID() {
        return (int)((Math.random()*9+1)*100000);
    }


    public String getSaveRootPath() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/隐云图解制作/");
        if (!file.exists()) {
            file.mkdirs();
        }

        return file.toString();
    }


    /**
     * 获取当前本地apk的版本
     *
     * @param mContext
     * @return
     */
    public static int getVersionCode(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     * @return
     */
    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    /**
    * 获取截取GIF时的分辨率
     *
     * @param videoPath 视频地址
     * @param ratio 缩放比例
    * */
    public String getVideo2GifRP(String videoPath, String ratio) {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(videoPath);
        String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

        return getRP(Integer.valueOf(width), Integer.valueOf(height), ratio);
    }

    /**
    * 获取根据比例缩放后的视频分辨率
     *
     * @param width 宽
     * @param height 高
     * @param ratio 比例
    * */
    public String getRP(int width, int height, String ratio) {
        String RP = "-1";

        switch (ratio) {
            case "-1":
                break;
            case "2":
                height = height/2;
                width = width/2;
                RP = width+"x"+height;
                break;
            case "4":
                height = height/4;
                width = width/4;
                RP = width+"x"+height;
                break;
            case "8":
                height = height/8;
                width = width/8;
                RP = width+"x"+height;
                break;
        }
        return RP;
    }
}
