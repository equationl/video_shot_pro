package com.equationl.videoshotpro.Image;


import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Tools{

    public int AllowNotBlackNums = 20;
    public int AllowCheckBlackLines = 10;

    /**
     * 将 bitmap 保存为png
     *
     * @return File 返回保存的文件
     * */
    public File saveBitmap2png(Bitmap bmp, String bitName,File savePath,boolean isReduce, int quality) throws Exception {
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

        f.createNewFile();
        FileOutputStream fOut = null;
        fOut = new FileOutputStream(f);
        bmp.compress(imgFormat, quality, fOut);
        fOut.flush();
        fOut.close();

        return f;
    }
    public File saveBitmap2png(Bitmap bmp, String bitName, File savePath) throws Exception {
        return this.saveBitmap2png(bmp, bitName, savePath, false, 100);
    }

    /**
     * 拼接两个 bitmap
     * */
    public Bitmap jointBitmap(Bitmap first, Bitmap second) {
        int width = Math.max(first.getWidth(),second.getWidth());
        int height = first.getHeight() + second.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(first, 0, 0, null);
        canvas.drawBitmap(second, 0, first.getHeight(), null);
        return result;
    }

    /**
     * 从文件获取图片的bitmap
     * */
    public Bitmap getBitmapFromFile(String no, File dirPath, String extension) throws Exception {
        File path = new File(dirPath, no+"."+extension);
        FileInputStream f;
        Bitmap bm = null;
        f = new FileInputStream(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        BufferedInputStream bis = new BufferedInputStream(f);
        bm = BitmapFactory.decodeStream(bis, null, options);

        return bm;
    }

    /**
     * 裁切图片
     * */
    public Bitmap cutBimap(Bitmap bm, int startY, int width) {
        return Bitmap.createBitmap(bm, 0, startY, width, bm.getHeight()-startY);
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
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
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
            // Return the remote address
            if (isGooglePhotosUri(imageUri))
                return imageUri.getLastPathSegment();
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
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


    public Boolean copyFileToCahe(List <String> files, String save_path, String extension) {
        int i = 0;
        for (String path : files) {
            try {
                copyFile(new File(path), new File(save_path + "/" + + i + "." + extension));
            } catch (IOException e) {
                return false;
            }
            i++;
        }
        return true;
    }

    private void copyFile(File fromFile,File toFile) throws IOException {
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
                String NewName = file.split("_")[0]+"."+file.split("\\.")[1];
                Log.i("EL", file+" rename to "+NewName);
                renameFile(path.getPath(), file, NewName);
            }
        }
    }

    public Boolean renameFile(String path,String oldname,String newname) {
        if(!oldname.equals(newname)){
            File oldfile=new File(path+"/"+oldname);
            File newfile=new File(path+"/"+newname);
            oldfile.renameTo(newfile);
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
     * @return 处理完成的bitmap
     *
    * */
    public Bitmap removeImgBlackSide(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Log.i("cao", "width="+width+" height="+height);

        int[] area = getImgBlackArea(bitmap);

        Log.i("cao", "area="+area[0]+" "+area[1]);

        if (area[0] > 0) {
            bitmap = Bitmap.createBitmap(bitmap, 0, area[0], width, height-area[0]);
        }
        if (area[1] > 0) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, area[1]);
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
    public boolean checkLineColorIsBlack(Bitmap bitmap, int y) {
        int len = bitmap.getWidth();
        int NotBlackNum = 0;
        int color;
        for (int i=0;i<len;i+=2) {    //空一个像素检测一次，节省时间
            color = bitmap.getPixel(i, y);
            //Log.i("nidaye", "i="+i+" y="+y);
            //Log.i("nidaye", "color="+color);
            if (color != Color.BLACK) {
                NotBlackNum++;
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
    public int[] getImgBlackArea(Bitmap bitmap) {
        int i = 0;
        int temp1, temp2, blackLineNums=0;
        Boolean lineIsBlack = checkLineColorIsBlack(bitmap,i);
        while (true) {
            if (i >= bitmap.getHeight()/2) {
                i = 0;
                break;
            }
            if (!lineIsBlack) blackLineNums++;
            else {
                blackLineNums = 0;
            }
            if (blackLineNums >= AllowCheckBlackLines) break;
            i++;
            lineIsBlack = checkLineColorIsBlack(bitmap,i);
        }
        temp1 = i-AllowCheckBlackLines;


        blackLineNums = 0;
        //i = (int)((bitmap.getHeight())/1.3);
        Log.i("cao", "i="+i);
        lineIsBlack = checkLineColorIsBlack(bitmap,i);
        while (true) {
            if (i >= (bitmap.getHeight()-1) ) {
                Log.i("cao", "jiushi ni l ");
                break;
            }
            if (lineIsBlack) {
                blackLineNums++;
                Log.i("cao", "lineIsBlack:true");
            }
            else {
                blackLineNums = 0;
            }
            if (blackLineNums >= AllowCheckBlackLines) break;
            i++;
            lineIsBlack = checkLineColorIsBlack(bitmap,i);
        }
        temp2 = i-AllowCheckBlackLines-temp1;


        return new int[]{temp1, temp2};
    }

}
