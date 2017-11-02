package com.equationl.videoshotpro;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.rom.HuaweiUtils;
import com.equationl.videoshotpro.rom.MeizuUtils;
import com.equationl.videoshotpro.rom.MiuiUtils;
import com.equationl.videoshotpro.rom.QikuUtils;
import com.equationl.videoshotpro.rom.RomUtils;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.yancy.gallerypick.config.GalleryConfig;
import com.yancy.gallerypick.config.GalleryPick;
import com.yancy.gallerypick.inter.IHandlerCallBack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Button button_user;
    AlertDialog.Builder dialog;
    AlertDialog dialog2;
    android.support.design.widget.CoordinatorLayout container;
    Tools tool;
    Resources res;
    GalleryConfig galleryConfig;
    SharedPreferences settings, sp_init;
    Dialog dialog_permission;
    List<String> FileList;
    String extension;
    ProgressDialog dialog_copyFile;
    Button button_splicing, button_floatBtn, button_ffmpeg,button_help, button_setting;
    TextView text_bottom;
    DrawerLayout drawer;
    int activityResultMode = 0;

    public static MainActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final int HandlerStatusLoadLibsFailure = 0;
    private static final int HandlerStatusFFmpegNotSupported = 1;
    private static final int HandlerStatusPackageNameNotRight = 2;
    private static final int HandlerStatusCopyFileDone = 3;
    private static final int IntentResultCodeMediaProjection = 10;
    private static final int ActivityResultFrameByFrame = 100;

    private static final String TAG = "In MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        dialog_copyFile = new ProgressDialog(this);
        dialog_copyFile.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置样式
        dialog_copyFile.setIndeterminate(false);
        dialog_copyFile.setCancelable(false);
        dialog_copyFile.setMessage("正在处理...");
        dialog_copyFile.setTitle("请稍等");

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        container =  (android.support.design.widget.CoordinatorLayout)findViewById(R.id.container);

        tool = new Tools();
        res = getResources();

        if (sp_init.getBoolean("isFirstBoot", true)) {
            showAlertDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstBoot", false);
            editor.apply();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (i != PackageManager.PERMISSION_GRANTED) {
                showDialogTipUserRequestPermission();
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        button_user = (Button)findViewById(R.id.btn_main_quickStart);
        button_splicing = (Button)findViewById(R.id.btn_main_splicing);
        button_ffmpeg = (Button)findViewById(R.id.btn_main_ffmpeg);
        button_floatBtn = (Button)findViewById(R.id.btn_main_floatBtn);
        button_help = (Button)findViewById(R.id.btn_main_help);
        button_setting = (Button)findViewById(R.id.btn_main_setting);

        text_bottom = (TextView)findViewById(R.id.text_main_bottom);
        text_bottom.getPaint().setFlags(Paint. UNDERLINE_TEXT_FLAG );

        dialog = new AlertDialog.Builder(this);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        Thread t = new Thread(new MainActivity.MyThread());
        t.start();

        galleryConfig = new GalleryConfig.Builder()
                .imageLoader(new GlideImageLoader())
                .iHandlerCallBack(iHandlerCallBack)
                .provider("com.equationl.videoshotpro.fileprovider")
                .multiSelect(true, 100)
                .build();

        text_bottom.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drawer.openDrawer(Gravity.START);
            }
        });

        button_user.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),1);
            }
        });

        button_splicing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btn_splicing();
            }
        });

        button_floatBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btn_floatBtn();
            }
        });

        button_ffmpeg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CommandActivity.class);
                startActivity(intent);
            }
        });

        button_help.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btn_help();
            }
        });

        button_setting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IntentResultCodeMediaProjection) {
                Log.i("EL", "try Start Service");

                try {
                    BuildPictureActivity.instance.finish();
                } catch (NullPointerException e){
                    Log.i("EL", "try to finish BuildPictureActivity fail");
                }

                FloatWindowsService.setResultData(data);
                Intent startService = new Intent(this, FloatWindowsService.class);
                startService(startService);
            }
            else {
                if (activityResultMode == ActivityResultFrameByFrame) {
                    //逐帧截取
                    activityResultMode = 0;
                    Uri uri = data.getData();
                    //String path = uri.getPath();
                    String path = tool.getImageAbsolutePath(this, uri);
                    Intent intent = new Intent(MainActivity.this, PlayerForDataActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("path", path);
                    bundle.putString("do", "FrameByFrame");
                    intent.putExtras(bundle);
                    intent.setData(uri);
                    startActivity(intent);
                }
                else {
                    //快速开始
                    Uri uri = data.getData();
                    //String path = uri.getPath();
                    String path = tool.getImageAbsolutePath(this, uri);
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("path", path);
                    intent.putExtras(bundle);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }

        //权限判断
        else if (requestCode == 123) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int i = ContextCompat.checkSelfPermission(this,  Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (i != PackageManager.PERMISSION_GRANTED) {
                    showDialogTipUserGoToAppSettting();
                } else {
                    if (dialog2 != null && dialog2.isShowing()) {
                        dialog2.dismiss();
                    }
                    Toast.makeText(this, R.string.main_toast_getPermission_success, Toast.LENGTH_SHORT).show();
                }
            }
        }

        else {
            Toast.makeText(getApplicationContext(),R.string.main_toast_chooseFile_fail,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        activityResultMode = 0;

        if (id == R.id.nav_help) {
            btn_help();
        }
        /*else if (id == R.id.nav_more) {
            Intent intent = new Intent(MainActivity.this, CommandActivity.class);
            startActivity(intent);
        } */
        else if (id == R.id.nav_feedback) {
            String versionName;
            int currentapiVersion=0;
            try {
                PackageManager packageManager = getPackageManager();
                PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(),0);
                versionName = packInfo.versionName;
                currentapiVersion=android.os.Build.VERSION.SDK_INT;
            }
            catch (Exception ex) {
                versionName = "NULL";
            }
            String mail_content = String.format(getResources().getString(R.string.main_mail_content),
                    versionName, currentapiVersion+"", android.os.Build.MODEL);
            Intent data=new Intent(Intent.ACTION_SENDTO);
            data.setData(Uri.parse("mailto:admin@likehide.com"));
            data.putExtra(Intent.EXTRA_SUBJECT, this.getResources().getString(R.string.main_mail_title));
            data.putExtra(Intent.EXTRA_TEXT, mail_content);
            startActivity(data);
        }
        /*else if (id == R.id.nav_setting) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_splicing) {
            btn_splicing();
        } else if (id == R.id.nav_floatBtn) {
            btn_floatBtn();
        }  */
        else if (id == R.id.nav_support) {
            showSupportDialog();
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        } else if (id == R.id.nav_frameByFrame) {
            btn_shotFrame();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showDialogTipUserRequestPermission() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.main_dialog_noPermission_title)
                .setMessage(R.string.main_dialog_noPermission_content)
                .setPositiveButton(R.string.main_dialog_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton(R.string.main_dialog_btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                , 321);
    }

    // 用户权限 申请 的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!b) {
                        showDialogTipUserGoToAppSettting();
                    } else
                        finish();
                } else {
                    Toast.makeText(this,  R.string.main_toast_getPermission_success, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 提示用户去应用设置界面手动开启权限
    private void showDialogTipUserGoToAppSettting() {
        dialog2 = new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.main_dialog_disablePermission_title))
                .setMessage(String.format(res.getString(R.string.main_dialog_disablePermission_content), res.getString(R.string.app_name)))
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToAppSetting();
                    }
                })
                .setNegativeButton(res.getString(R.string.main_dialog_btn_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    private void goToAppSetting() {
        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);

        startActivityForResult(intent, 123);
    }

    private void loadLib() {
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    handler.sendEmptyMessage(HandlerStatusLoadLibsFailure);
                }
            });
        } catch (FFmpegNotSupportedException e) {
            handler.sendEmptyMessage(HandlerStatusFFmpegNotSupported);
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HandlerStatusLoadLibsFailure:
                    Snackbar snackbar = Snackbar.make(container, R.string.main_snackbar_loadSo_fail, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.main_snackbar_btn_retry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadLib();
                        }
                    });
                    snackbar.setActionTextColor(Color.BLUE);
                    snackbar.show();
                    break;
                case HandlerStatusFFmpegNotSupported:
                    Snackbar snackbar2 = Snackbar.make(container, R.string.main_snackbar_so_notAble, Snackbar.LENGTH_SHORT);
                    snackbar2.setAction(R.string.main_snackbar_btn_contact, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String versionName;
                            int currentapiVersion=0;
                            try {
                                PackageManager packageManager = getPackageManager();
                                PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(),0);
                                versionName = packInfo.versionName;
                                currentapiVersion=android.os.Build.VERSION.SDK_INT;
                            }
                            catch (Exception ex) {
                                versionName = "NULL";
                            }
                            String mail_content = String.format(getResources().getString(R.string.main_mail_content),
                                    versionName, currentapiVersion+"", android.os.Build.MODEL);
                            Intent data=new Intent(Intent.ACTION_SENDTO);
                            data.setData(Uri.parse("mailto:admin@likehide.com"));
                            data.putExtra(Intent.EXTRA_SUBJECT, MainActivity.this.getResources().getString(R.string.main_mail_title));
                            data.putExtra(Intent.EXTRA_TEXT, mail_content);
                            startActivity(data);
                        }
                    });
                    snackbar2.setActionTextColor(Color.BLUE);
                    snackbar2.show();
                    break;
                case HandlerStatusPackageNameNotRight:
                    Snackbar snackbar3 = Snackbar.make(container, R.string.main_snackbar_isPiracy, Snackbar.LENGTH_LONG);
                    snackbar3.show();
                    break;
                case HandlerStatusCopyFileDone:

                    Intent intent = new Intent(MainActivity.this, MarkPictureActivity.class);
                    startActivity(intent);
            }

        }
    };

    private class MyThread implements Runnable {
        @Override
        public void run() {
            loadLib();
            try {
                String pkName = getApplicationContext().getPackageName();
                if (!pkName.equals("com.equationl.videoshotpro")) {
                    handler.sendEmptyMessage(HandlerStatusPackageNameNotRight);
                }
            } catch (Exception e) {
                handler.sendEmptyMessage(HandlerStatusPackageNameNotRight);
            }
        }
    }

    private class MyThreadCopyFile implements Runnable {
        @Override
        public  void run() {
            if (tool.copyFileToCahe(FileList, getExternalCacheDir().toString(), extension)) {
                handler.sendEmptyMessage(HandlerStatusCopyFileDone);
            }
        }
    }

    IHandlerCallBack iHandlerCallBack = new IHandlerCallBack() {
        @Override
        public void onStart() {
            Log.i(TAG, "onStart: 开启");
        }

        @Override
        public void onSuccess(List<String> photoList) {
            Log.i(TAG, "onSuccess: 返回数据");
            tool.cleanExternalCache(MainActivity.this);
            extension = settings.getBoolean("isShotToJpg", true) ? "jpg":"png";
            FileList = photoList;
            dialog_copyFile.show();
            new Thread(new MainActivity.MyThreadCopyFile()).start();
        }

        @Override
        public void onCancel() {
            Log.i(TAG, "onCancel: 取消");
        }

        @Override
        public void onFinish() {
            Log.i(TAG, "onFinish: 结束");
        }

        @Override
        public void onError() {
            Log.i(TAG, "onError: 出错");
            Toast.makeText(MainActivity.this, R.string.main_toast_choosePictures_fail, Toast.LENGTH_SHORT).show();
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showFloatBtn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, R.string.main_toast_unSupportFloatBtn, Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkPermission(this)) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                    getSystemService(this.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(),
                    IntentResultCodeMediaProjection);
        } else {
            applyPermission(this);
        }
    }

    private boolean checkPermission(Context context) {
        //6.0 版本之后由于 google 增加了对悬浮窗权限的管理，所以方式就统一了
        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                return miuiPermissionCheck(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                return meizuPermissionCheck(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                return huaweiPermissionCheck(context);
            } else if (RomUtils.checkIs360Rom()) {
                return qikuPermissionCheck(context);
            }
        }
        return commonROMPermissionCheck(context);
    }

    private boolean huaweiPermissionCheck(Context context) {
        return HuaweiUtils.checkFloatWindowPermission(context);
    }

    private boolean miuiPermissionCheck(Context context) {
        return MiuiUtils.checkFloatWindowPermission(context);
    }

    private boolean meizuPermissionCheck(Context context) {
        return MeizuUtils.checkFloatWindowPermission(context);
    }

    private boolean qikuPermissionCheck(Context context) {
        return QikuUtils.checkFloatWindowPermission(context);
    }

    private boolean commonROMPermissionCheck(Context context) {
        //最新发现魅族6.0的系统这种方式不好用，天杀的，只有你是奇葩，没办法，单独适配一下
        if (RomUtils.checkIsMeizuRom()) {
            return meizuPermissionCheck(context);
        }
        else if (RomUtils.checkIsMiuiRom()) {
            return miuiPermissionCheck(context);
        }

        else {
            Boolean result = true;
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class clazz = Settings.class;
                    Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
                    result = (Boolean) canDrawOverlays.invoke(null, context);
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            return result;
        }
    }

    private void applyPermission(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                miuiROMPermissionApply(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                meizuROMPermissionApply(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                huaweiROMPermissionApply(context);
            } else if (RomUtils.checkIs360Rom()) {
                ROM360PermissionApply(context);
            }
        }
        commonROMPermissionApply(context);
    }

    private void ROM360PermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    QikuUtils.applyPermission(context);
                } else {
                    Log.e(TAG, "ROM:360, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void huaweiROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    HuaweiUtils.applyPermission(context);
                } else {
                    Log.e(TAG, "ROM:huawei, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void meizuROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    MeizuUtils.applyPermission(context);
                } else {
                    Log.e(TAG, "ROM:meizu, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    private void miuiROMPermissionApply(final Context context) {
        showConfirmDialog(context, new OnConfirmResult() {
            @Override
            public void confirmResult(boolean confirm) {
                if (confirm) {
                    MiuiUtils.applyMiuiPermission(context);
                } else {
                    Log.e(TAG, "ROM:miui, user manually refuse OVERLAY_PERMISSION");
                }
            }
        });
    }

    /**
     * 通用 rom 权限申请
     */
    private void commonROMPermissionApply(final Context context) {
        //这里也一样，魅族系统需要单独适配
        if (RomUtils.checkIsMeizuRom()) {
            meizuROMPermissionApply(context);
        }
        else if (RomUtils.checkIsMiuiRom()) {
            miuiROMPermissionApply(context);
        }
        else {
            if (Build.VERSION.SDK_INT >= 23) {
                showConfirmDialog(context, new OnConfirmResult() {
                    @Override
                    public void confirmResult(boolean confirm) {
                        if (confirm) {
                            try {
                                Class clazz = Settings.class;
                                Field field = clazz.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION");

                                Intent intent = new Intent(field.get(null).toString());
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.setData(Uri.parse("package:" + context.getPackageName()));
                                context.startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                        } else {
                            Log.d(TAG, "user manually refuse OVERLAY_PERMISSION");
                            //需要做统计效果
                        }
                    }
                });
            }
        }
    }

    private void showConfirmDialog(Context context, OnConfirmResult result) {
        showConfirmDialog(context, res.getString(R.string.main_dialog_needFloatPermission_content), result);
    }

    private void showConfirmDialog(Context context, String message, final OnConfirmResult result) {
        if (dialog_permission != null && dialog_permission.isShowing()) {
            dialog_permission.dismiss();
        }

        dialog_permission = new AlertDialog.Builder(context).setCancelable(true).setTitle("")
                .setMessage(message)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirmResult(true);
                                dialog.dismiss();
                            }
                        }).setNegativeButton(res.getString(R.string.main_dialog_btn_cancel),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirmResult(false);
                                dialog.dismiss();
                            }
                        }).create();
        dialog_permission.show();
    }

    private interface OnConfirmResult {
        void confirmResult(boolean confirm);
    }

    private void showSupportDialog() {
        LayoutInflater mLayoutInflater= LayoutInflater.from(MainActivity.this);
        View view=mLayoutInflater.inflate(R.layout.dialog_main_support, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.main_dialog_support_title)
                .setView(view)
                .setCancelable(true)
                .create();
        builder.show();
        TextView bottomText = (TextView) view.findViewById(R.id.main_dialog_support_bottomText);
        bottomText.setText(Html.fromHtml(getString(R.string.main_dialog_support_bottomText)));
        bottomText.setTextIsSelectable(true);
    }

    private void showAlertDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_alert_title)
                .setMessage(R.string.main_dialog_alert_content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                showNavigationGuider();
                            }
                        }).create();
        dialog.show();
    }

    private void showSplicingDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_splicing_title)
                .setMessage(R.string.main_dialog_splicing_content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                GalleryPick.getInstance().setGalleryConfig(galleryConfig).open(MainActivity.this);
                            }
                        }).create();
        dialog.show();
    }

    private void showShotFrmeDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_shotFrame_title)
                .setMessage(R.string.main_dialog_shotFrame_content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                activityResultMode = ActivityResultFrameByFrame;
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("video/*");
                                intent.addCategory(intent.CATEGORY_OPENABLE);
                                startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),1);
                            }
                        }).create();
        dialog.show();
    }

    private void showFloatDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_float_title)
                .setMessage(R.string.main_dialog_float_content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                showFloatBtn();
                            }
                        }).create();
        dialog.show();
    }

    private void showAboutDialog() {
        String content = String.format(
                res.getString(R.string.main_dialog_about_content),
                res.getString(R.string.main_updateHistory_text),
                res.getString(R.string.main_right_text));
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_about_title)
                .setMessage(content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

    /*@Override
    public void onResume() {
        super.onResume();
        if (dialog_copyFile.isShowing()) {
            dialog_copyFile.dismiss();
        }
    }  */


    @Override
    public void onPause() {
        super.onPause();
        dialog_copyFile.dismiss();
    }


    private void btn_splicing() {
        if (sp_init.getBoolean("isFirstUseSplicing", true)) {
            showSplicingDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseSplicing", false);
            editor.apply();
        }
        else {
            GalleryPick.getInstance().setGalleryConfig(galleryConfig).open(MainActivity.this);
        }
    }

    private void btn_floatBtn() {
        if (sp_init.getBoolean("isFirstUseFloat", true)) {
            showFloatDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseFloat", false);
            editor.apply();
        }
        else {
            showFloatBtn();
        }
    }

    private void btn_help() {
        String info_text = res.getString(R.string.main_information_content);
        String content = String.format(info_text, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
        dialog.setTitle(res.getString(R.string.main_information_title));
        dialog.setMessage(content);
        dialog.setIcon(R.mipmap.ic_launcher);
        dialog.create().show();
    }

    private void btn_shotFrame() {
        if (sp_init.getBoolean("isFirstUseShotFrame", true)) {
            showShotFrmeDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseShotFrame", false);
            editor.apply();
        }
        else {
            activityResultMode = ActivityResultFrameByFrame;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),1);
        }
    }

    private void showNavigationGuider() {
        drawer.openDrawer(Gravity.START);
        Toast.makeText(this, R.string.main_toast_showNavigationGuider, Toast.LENGTH_LONG).show();
    }
}

