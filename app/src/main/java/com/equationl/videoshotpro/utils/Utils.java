package com.equationl.videoshotpro.utils;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.equationl.videoshotpro.AboutActivity;
import com.equationl.videoshotpro.DownloadActivity;
import com.equationl.videoshotpro.R;
import com.equationl.ffmpeg.FFmpeg;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class Utils {
    private static final String TAG = "el,In Utils";


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

    public static void finishActivity(Activity context) {
        try {
            context.finish();
        } catch (NullPointerException e) {
            Log.e("el, in finishActivity", e.toString());
        }
    }


    /**
     * 第三种方法
     * 首先先获取手机上已经安装的应用市场
     * 获取已安装应用商店的包名列表
     * 获取有在AndroidManifest 里面注册<category android:name="android.intent.category.APP_MARKET" />的app
     * 作者：ProcessZ
     * 链接：https://www.jianshu.com/p/fc340cc6f75f
     * @param context
     * @return
     */
    public static ArrayList<String> getInstallAppMarkets(Context context) {
        //默认的应用市场列表，有些应用市场没有设置APP_MARKET通过隐式搜索不到
        ArrayList<String>  pkgList = new ArrayList<>();
        //将我们上传的应用市场都传上去
        pkgList.add("com.android.vending");                        //Google Play
        pkgList.add("com.coolapk.market");                        //酷安
        pkgList.add("com.tencent.android.qqdownloader");        //腾讯应用宝
        pkgList.add("com.baidu.appsearch");                     //百度手机助手
        pkgList.add("com.wandoujia.phoenix2");                  //豌豆荚
        pkgList.add("com.hiapk.marketpho");                     //安智应用商店
        pkgList.add("com.qihoo.appstore");                      //360手机助手
        ArrayList<String> pkgs = new ArrayList<String>();
        if (context == null)
            return pkgs;
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.APP_MARKET");
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> info = pm.queryIntentActivities(intent, 0);
        if (info == null || info.size() == 0)
            return pkgs;
        int size = info.size();
        for (int i = 0; i < size; i++) {
            String pkgName = "";
            try {
                ActivityInfo activityInfo = info.get(i).activityInfo;
                pkgName = activityInfo.packageName;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!TextUtils.isEmpty(pkgName))
                pkgs.add(pkgName);

        }
        //取两个list并集,去除重复
        pkgList.removeAll(pkgs);
        pkgs.addAll(pkgList);
        return pkgs;
    }

    /**
     * 过滤出已经安装的包名集合
     * @param context
     * @param pkgs  待过滤包名集合
     * @return      已安装的包名集合
     */
    public static ArrayList<String> getFilterInstallMarkets(Context context,ArrayList<String> pkgs) {
        ArrayList<String> appList = new ArrayList<>();
        if (context == null || pkgs == null || pkgs.size() == 0)
            return appList;
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPkgs = pm.getInstalledPackages(0);
        int li = installedPkgs.size();
        int lj = pkgs.size();
        for (int j = 0; j < lj; j++) {
            for (int i = 0; i < li; i++) {
                String installPkg = "";
                String checkPkg = pkgs.get(j);
                PackageInfo packageInfo = installedPkgs.get(i);
                try {
                    installPkg = packageInfo.packageName;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (TextUtils.isEmpty(installPkg))
                    continue;
                if (installPkg.equals(checkPkg)) {
                    // 如果非系统应用，则添加至appList,这个会过滤掉系统的应用商店，如果不需要过滤就不用这个判断
                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        appList.add(installPkg);
                    }
                    break;
                }
            }
        }
        return appList;
    }

    /**
     * 跳转到应用市场app详情界面
     * @param appPkg    App的包名
     * @param marketPkg 应用市场包名
     */
    public static void launchAppDetail(Context context , String appPkg, String marketPkg) throws Exception{
        if (TextUtils.isEmpty(appPkg))
            return;
        Uri uri = Uri.parse("market://details?id=" + appPkg);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (!TextUtils.isEmpty(marketPkg)) {
            intent.setPackage(marketPkg);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    /**
     *
    *跳转至已经安装的第三方市场
    * */
    public void goToMarkPlay(Context context) {
        String pkName = context.getApplicationContext().getPackageName();
        ArrayList<String>  installedMarkList = getFilterInstallMarkets(context, getInstallAppMarkets(context));
        if (installedMarkList.isEmpty()) {
            //如果未安装指定的几个应用市场则跳转至系统默认
            try{
                Uri uri = Uri.parse("market://details?id="+pkName);
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        for (String mark : installedMarkList) {
            Log.i(TAG, "mark="+mark);
            try {
                launchAppDetail(context, pkName, mark);
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            Log.e(TAG, "fileToMD5: get md5 fail:", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "file to md5 failed", e);
                }
            }
        }
    }

    public static String bytesBeHuman(Long bytes) {
        if (bytes <= 1024) {
            return bytes+"BB";
        }
        if (bytes <= 1048576) {
            return String.format(Locale.CHINA,"%.2fKB", bytes/1024.0);
        }
        if (bytes <= 1073741824) {
            return String.format(Locale.CHINA,"%.2fMB", bytes/1048576.0);
        }
        return String.format(Locale.CHINA,"%.2fGB", bytes/1073741824.0);
    }

    public void copyAssets2Local(Context context, String asset, String savePath) throws IOException {
        InputStream is = context.getResources().getAssets().open(asset);
        FileOutputStream fos = new FileOutputStream(savePath);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = is.read(buffer)) > 0){
            fos.write(buffer, 0, count);
        }
        fos.close();
        is.close();
    }

    /**
     * 判断某个service是否正在运行
     *
     * @param context context
     * @param runService
     *            要验证的service组件的类名
     * @return 是否正在运行
     */
    public static boolean isServiceRunning(Context context,
                                           Class<? extends Service> runService) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) am
                .getRunningServices(1024);
        for (int i = 0; i < runningService.size(); ++i) {
            if (runService.getName().equals(
                    runningService.get(i).service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void showSupportDialog(final Context context, final SharedPreferences sharedPreferences) {
        final Dialog dialog = new AlertDialog.Builder(context).setCancelable(true)
                .setMessage(R.string.main_dialog_text_support)
                .setPositiveButton(R.string.main_dialog_support_btn_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(context, AboutActivity.class);
                                context.startActivity(intent);
                            }
                        })
                .setNegativeButton(R.string.main_dialog_support_btn_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                .setNeutralButton(R.string.main_dialog_support_btn_close,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("isShowSupportInMainActivity", false);
                                editor.apply();
                            }
                        })
                .create();
        boolean isShow = sharedPreferences.getBoolean("isShowSupportInMainActivity", true);
        if (isShow) {
            dialog.show();
        }
    }

    /**
     * <p>获取 ffmpeg 对象</p>
     * <p>如果 ffmpeg 可用且存在则返回 ffmpeg 对象</p>
     * <p>如果 ffmpeg 不可用则返回 null</p>
     * <p>如果 ffmpeg 不存在则弹框提示下载，确定后跳转至下载页面<，并抛出异常/p>
     * */
    public static FFmpeg getFFmpeg(final Context context)throws Exception{
        FFmpeg fFmpeg = FFmpeg.getInstance(context);
        if (fFmpeg.isSupported()) {
            if (fFmpeg.isFFmpegExist()) {
                return fFmpeg;
            }
            else {
                String prefix = fFmpeg.getPrefix();
                showNeedDownloadDialog(context, prefix);
                throw new Exception("ffmpeg not exist");
                /*File downFile = downloadFile(prefix);
                fFmpeg.setFFmpegFile(downFile);
                if (fFmpeg.isFFmpegExist()) {
                    return fFmpeg;
                }
                else {
                    return null;
                }   */
            }
        }
        else {
            return null;
        }
    }

    private static void showNeedDownloadDialog(final Context context, final String prefix) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.downloadActivity_dialog_askDownload_title)
                .setMessage(R.string.downloadActivity_dialog_askDownload_message)
                .setCancelable(false)
                .setNegativeButton(R.string.downloadActivity_dialog_askDownload_btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                })
                .setPositiveButton(R.string.downloadActivity_dialog_askDownload_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(context, DownloadActivity.class);
                        intent.putExtra("prefix", prefix);
                        context.startActivity(intent);
                    }
                })
                .show();
    }

    private static String convertHashToString(byte[] md5Bytes) {
        StringBuilder buf = new StringBuilder();
        for (byte md5Byte : md5Bytes) {
            buf.append(Integer.toString((md5Byte & 0xff) + 0x100, 16).substring(1));
        }
        return buf.toString().toUpperCase();
    }
}
