package com.equationl.videoshotpro.Image;


import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Tools{
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
    public static String getImageAbsolutePath(Activity context, Uri imageUri) {
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

    public static void cleanExternalCache(Context context) {
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

}
