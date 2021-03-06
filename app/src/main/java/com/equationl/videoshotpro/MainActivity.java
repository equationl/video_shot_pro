package com.equationl.videoshotpro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.feedback.FeedbackAgent;
    import com.equationl.ffmpeg.FFmpeg;
import com.equationl.videoshotpro.Adapter.MainWaterFallAdapter;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.rom.HuaweiUtils;
import com.equationl.videoshotpro.rom.MeizuUtils;
import com.equationl.videoshotpro.rom.MiuiUtils;
import com.equationl.videoshotpro.rom.QikuUtils;
import com.equationl.videoshotpro.rom.RomUtils;
import com.equationl.videoshotpro.utils.Share;
import com.equationl.videoshotpro.utils.Utils;
import com.equationl.videoshotpro.utils.WaterFallData;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.yancy.gallerypick.config.GalleryConfig;
import com.yancy.gallerypick.config.GalleryPick;
import com.yancy.gallerypick.inter.IHandlerCallBack;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.glide.ImageLoader;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import me.solidev.loadmore.AutoLoadMoreAdapter;
import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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
    DrawerLayout drawer;
    int activityResultMode = 0;
    int selectedCount = 0;
    Boolean isFirstBoot = false;
    boolean isTranslucentStatus = false;
    boolean isMultiSelect = false;
    FloatingActionsMenu main_floatBtn_menu;
    FloatingActionButton main_floatBtn_quick;
    FloatingActionButton main_floatBtn_splicing;
    FloatingActionButton main_floatBtn_shotScreen;
    FloatingActionButton main_floatBtn_frameByFrame;

    AutoLoadMoreAdapter mAutoLoadMoreAdapter;
    RecyclerView.Adapter mRecycleAdapter;
    RecyclerView mRecyclerView;

    List<WaterFallData> waterFallList = new ArrayList<>();
    int waterFallDataPager = 0;

    private final MyHandler handler = new MyHandler(this);
    @SuppressLint("StaticFieldLeak")
    public static MainActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final int HandlerStatusLoadLibsFailure = 0;
    private static final int HandlerStatusFFmpegNotSupported = 1;
    private static final int HandlerStatusPackageNameNotRight = 2;
    private static final int HandlerStatusCopyFileDone = 3;
    private static final int HandlerStatusCopyFileFail = 4;
    private static final int IntentResultCodeMediaProjection = 10;
    private static final int ActivityResultFrameByFrame = 100;

    private static final int RequestCodeQuickStart = 1000;
    private static final int RequestCodeAutoBuild = 1001;
    private static final int RequestCodeGoToSetting = 1002;

    private static final int SnackBarOnClickDoSure = 200;
    //private static final int SnackBarOnClickDoReloadFFmpeg = 201;
    private static final int SnackBarOnClickDoFeedback = 203;

    private static final String TAG = "el,In MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setBackgroundDrawable(null);
            isTranslucentStatus = true;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        initLayout();
        initRecycleView();
        showGuide();
    }

    private void initLayout() {
        dialog_copyFile = new ProgressDialog(this);
        dialog_copyFile.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置样式
        dialog_copyFile.setIndeterminate(false);
        dialog_copyFile.setCancelable(false);
        dialog_copyFile.setMessage("正在处理...");
        dialog_copyFile.setTitle("请稍等");

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        container = findViewById(R.id.container);

        tool = new Tools();
        res = getResources();

        //FIXME 应该从根源入手。找到为何启动了MainActivity就打不开其他Activity的原因，而不是老是这样投机取巧
        if (Utils.isServiceRunning(this, FloatWindowsService.class)) {
            Log.i(TAG, "initLayout: FloatWindowsService are running");
            Toast.makeText(this, R.string.main_toast_launcherWhenFloatServiceIsRunning, Toast.LENGTH_LONG).show();
            finish();
        }

        if (sp_init.getBoolean("isFirstBoot", true)) {
            //FIXME 显示警告某些应用商城审核就不给过？？？？
            //showAlertDialog();
            isFirstBoot = true;
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstBoot", false);
            editor.apply();
            //showGuide();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                isFirstBoot = true;
                showDialogTipUserRequestPermission();
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        main_floatBtn_menu = findViewById(R.id.main_floatBtn_menu);
        main_floatBtn_quick = findViewById(R.id.main_floatBtn_quick);
        main_floatBtn_splicing = findViewById(R.id.main_floatBtn_splicing);
        main_floatBtn_shotScreen = findViewById(R.id.main_floatBtn_shotScreen);
        main_floatBtn_frameByFrame = findViewById(R.id.main_floatBtn_frameByFrame);

        mRecyclerView = findViewById(R.id.main_recyclerView);

        main_floatBtn_quick.setOnClickListener(clickListener);
        main_floatBtn_splicing.setOnClickListener(clickListener);
        main_floatBtn_shotScreen.setOnClickListener(clickListener);
        main_floatBtn_frameByFrame.setOnClickListener(clickListener);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //noinspection deprecation
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        Thread t = new Thread(new MainActivity.InitThread());
        t.start();

        int userFlagID = sp_init.getInt("userFlagID", 0);
        if (userFlagID == 0) {
            userFlagID = tool.createID();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putInt("userFlagID", userFlagID);
            editor.apply();
        }
        Bugly.init(getApplicationContext(), "41a66442fd", false);
        CrashReport.setUserId(sp_init.getInt("userFlagID", 0)+"");


        int bootTimes = sp_init.getInt("bootTimes", 0);
        SharedPreferences.Editor editor = sp_init.edit();
        editor.putInt("bootTimes", bootTimes+1);
        editor.apply();
        if (bootTimes > 5 && bootTimes%5 == 0) {
            Utils.showSupportDialog(this, sp_init);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (isMultiSelect) {
            getMenuInflater().inflate(R.menu.activity_main_multi_select, menu);
        }
        else {
            getMenuInflater().inflate(R.menu.activity_main, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isMultiSelect) {
            menu.findItem(R.id.main_menu_isDirFirst).setChecked(sp_init.getBoolean("mainSortIsDirFirst", true));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        invalidateOptionsMenu();//通知系统刷新Menu
        int id = item.getItemId();
        SharedPreferences.Editor editor = sp_init.edit();
        switch (id) {
            case R.id.main_menu_sort_date_asc:
                editor.putString("mainSort", "dateAsc");
                editor.apply();
                refreshWaterfall();
                break;
            case R.id.main_menu_sort_date_desc:
                editor.putString("mainSort", "dateDest");
                editor.apply();
                refreshWaterfall();
                break;
            case R.id.main_menu_sort_type:
                editor.putString("mainSort", "type");
                editor.apply();
                refreshWaterfall();
                break;
            case R.id.main_menu_isDirFirst:
                if (item.isChecked()) {
                    editor.putBoolean("mainSortIsDirFirst", false);
                    item.setChecked(false);
                }
                else {
                    editor.putBoolean("mainSortIsDirFirst", true);
                    item.setChecked(true);
                }
                editor.apply();
                refreshWaterfall();
                break;
            case R.id.main_menu_refresh:
                refreshWaterfall();
                break;
            case R.id.main_menu_multiSelect:
                isMultiSelect = true;
                setTitle(R.string.main_title_multiSelect);
                invalidateOptionsMenu();
                break;
            case R.id.main_menu_exitSelect:
                exitMultiSelect();
                break;
            case R.id.main_menu_allDelete:
                allDelete();
                break;
            case R.id.main_menu_allSelect:
                clickAllSelect();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exitMultiSelect() {
        isMultiSelect = false;
        setTitle(R.string.app_name);
        selectedCount = 0;
        invalidateOptionsMenu();
        for (WaterFallData data : waterFallList) {
            data.isSelected = false;
        }

        mRecycleAdapter.notifyDataSetChanged();
    }

    private void clickAllSelect() {
        int hasSelectedCount = 0;
        for (WaterFallData data : waterFallList) {
            if (data.isSelected) {
                hasSelectedCount++;
            }
            data.isSelected = true;
            selectedCount++;
        }
        if (hasSelectedCount == waterFallList.size()) {
            for (WaterFallData data : waterFallList) {
                data.isSelected = false;
            }
            selectedCount = 0;
        }

        mRecycleAdapter.notifyDataSetChanged();
    }

    private void allDelete() {
        if (selectedCount < 1) {
            Toast.makeText(this, R.string.main_toast_unSelectedPic, Toast.LENGTH_SHORT).show();
        }
        else {
            showDeletePicDialog(this, null, 0, false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Tencent.onActivityResultData(requestCode,resultCode,data, shareListener);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IntentResultCodeMediaProjection) {
                //外部程序截图
                Log.i("EL", "try Start Service");
                Utils.finishActivity(BuildPictureActivity.instance);
                FloatWindowsService.setResultData(data);
                Intent startService = new Intent(this, FloatWindowsService.class);
                startService(startService);
            }
            else if (activityResultMode == ActivityResultFrameByFrame) {
                //逐帧截取
                activityResultMode = 0;
                Uri uri = data.getData();
                Intent intent = new Intent(MainActivity.this, PlayerForDataActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("do", "FrameByFrame");
                intent.putExtras(bundle);
                intent.setData(uri);
                startActivity(intent);
            }
            else if (requestCode == RequestCodeQuickStart){
                //快速开始
                Uri uri = data.getData();
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.setData(uri);
                startActivity(intent);
            }

            else if (requestCode == RequestCodeAutoBuild) {
                Uri uri = data.getData();
                Intent intent = new Intent(MainActivity.this, PlayerForDataActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("do", "AutoBuild");
                intent.putExtras(bundle);
                intent.setData(uri);
                startActivity(intent);
            }
            else if (requestCode == RequestCodeGoToSetting) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        showDialogTipUserGoToAppSettting();
                    } else {
                        if (dialog2 != null && dialog2.isShowing()) {
                            dialog2.dismiss();
                        }
                        Toast.makeText(this, R.string.main_toast_getPermission_success, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        activityResultMode = 0;

        switch (id) {
            case R.id.nav_help:
                btn_help();
                break;
            case R.id.nav_feedback:
                btn_feedback();
                break;
            case R.id.main_nav_setting:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_about:
                showAboutDialog();
                break;
            case R.id.nav_share:
                shareAPP();
                break;
            case R.id.main_nav_ffmpeg:
                intent = new Intent(MainActivity.this, CommandActivity.class);
                startActivity(intent);
                break;
            case R.id.main_nav_autoBuild:
                btn_AB();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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
                        SharedPreferences.Editor editor = sp_init.edit();
                        editor.putBoolean("isFirstBoot", true);
                        editor.apply();
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
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                        if (!b) {
                            showDialogTipUserGoToAppSettting();
                        } else
                            finish();
                    }
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

        startActivityForResult(intent, RequestCodeGoToSetting);
    }

    /*private void loadLib() {
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
    }  */

    private class InitThread implements Runnable {
        @Override
        public void run() {
            //loadLib();
            Utils.finishActivity(MarkPictureActivity.instance);
            Utils.finishActivity(ChooseActivity.instance);
            Utils.finishActivity(BuildPictureActivity.instance);
            Utils.finishActivity(PlayerActivity.instance);
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
            File cacheDir = getExternalCacheDir();
            if (cacheDir == null) {
                handler.sendEmptyMessage(HandlerStatusCopyFileFail);
            }
            else {
                if (tool.copyFileToCache(FileList, cacheDir.toString(), extension)) {
                    handler.sendEmptyMessage(HandlerStatusCopyFileDone);
                }
                else {
                    handler.sendEmptyMessage(HandlerStatusCopyFileFail);
                }
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
                    getSystemService(MEDIA_PROJECTION_SERVICE);
            try {
                startActivityForResult(
                        mediaProjectionManager.createScreenCaptureIntent(),
                        IntentResultCodeMediaProjection);
            } catch (ActivityNotFoundException e) {   //详见Bugly 异常id：11006
                CrashReport.postCatchedException(e);
                Toast.makeText(this, R.string.main_toast_canNotStartFloatBtn, Toast.LENGTH_SHORT).show();
            }
        } else {
            applyPermission(this);
        }
    }

    private boolean checkPermission(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            if (RomUtils.checkIsMiuiRom()) {
                return MiuiUtils.checkFloatWindowPermission(context);
            } else if (RomUtils.checkIsMeizuRom()) {
                return MeizuUtils.checkFloatWindowPermission(context);
            } else if (RomUtils.checkIsHuaweiRom()) {
                return HuaweiUtils.checkFloatWindowPermission(context);
            } else if (RomUtils.checkIs360Rom()) {
                return QikuUtils.checkFloatWindowPermission(context);
            }
        }
        return commonROMPermissionCheck(context);
    }

    private boolean commonROMPermissionCheck(Context context) {
        if (RomUtils.checkIsMeizuRom()) {
            return MeizuUtils.checkFloatWindowPermission(context);
        }
        if (RomUtils.checkIsMiuiRom()) {
            return MiuiUtils.checkFloatWindowPermission(context);
        }

        else {
            boolean result = true;
            if (Build.VERSION.SDK_INT >= 23) {
                result =  Settings.canDrawOverlays(this);
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

    private void showCommonDialog(int content, int btn, final int from) {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_title_hint)
                .setMessage(content)
                .setPositiveButton(res.getString(btn),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (from) {
                                    case SnackBarOnClickDoSure:
                                        break;
                                    /*case SnackBarOnClickDoReloadFFmpeg:
                                        loadLib();
                                        break;  */
                                    case SnackBarOnClickDoFeedback:
                                        startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
                                        break;
                                }
                            }
                        }).create();
        dialog.show();
    }

    private void showDeletePicDialog(Context context, final File f, final int pos, final boolean fromDir) {
        boolean isDeleteLocalPic = sp_init.getBoolean("isDeleteLocalPic", true);
        boolean[] initChoiceSets = {isDeleteLocalPic};
        int title = isMultiSelect ? R.string.main_dialog_deleteAllPic_title : R.string.main_dialog_deletePic_title;
        Dialog dialog = new AlertDialog.Builder(context).setCancelable(false)
                .setTitle(title)
                .setMultiChoiceItems(R.array.main_dialog_deletePic_content, initChoiceSets,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    SharedPreferences.Editor editor = sp_init.edit();
                                    editor.putBoolean("isDeleteLocalPic", true);
                                    editor.apply();
                                } else {
                                    SharedPreferences.Editor editor = sp_init.edit();
                                    editor.putBoolean("isDeleteLocalPic", false);
                                    editor.apply();
                                }
                            }
                        })
                .setPositiveButton(R.string.main_dialog_btn_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isMultiSelect) {
                                    clickDeleteAllPic();
                                }
                                else {
                                    clickDeletePic(f, pos, fromDir);
                                }
                            }
                        })
                .setNegativeButton(R.string.main_dialog_btn_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                        )
                .create();
        dialog.show();
    }

    private void showABDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_dialog_AB_title)
                .setMessage(R.string.main_dialog_AB_content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("video/*");
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),RequestCodeAutoBuild);
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
                                galleryConfig = new GalleryConfig.Builder()
                                        .imageLoader(new GlideImageLoader())
                                        .iHandlerCallBack(iHandlerCallBack)
                                        .provider("com.equationl.videoshotpro.fileprovider")
                                        .multiSelect(true, 100)
                                        .build();
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
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
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
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshWaterfall();
    }


    @Override
    public void onPause() {
        super.onPause();
        dialog_copyFile.dismiss();
    }

    private void btn_AB() {
        if (sp_init.getBoolean("isFirstUseAB", true)) {
            showABDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseAB", false);
            editor.apply();
        }
        else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),RequestCodeAutoBuild);
        }
    }

    private void btn_splicing() {
        if (sp_init.getBoolean("isFirstUseSplicing", true)) {
            showSplicingDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseSplicing", false);
            editor.apply();
        }
        else {
            galleryConfig = new GalleryConfig.Builder()
                    .imageLoader(new GlideImageLoader())
                    .iHandlerCallBack(iHandlerCallBack)
                    .provider("com.equationl.videoshotpro.fileprovider")
                    .multiSelect(true, 100)
                    .build();
            GalleryPick.getInstance().setGalleryConfig(galleryConfig).open(MainActivity.this);
        }
    }

    private void btn_shotScreen() {
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
        String content = String.format(res.getString(R.string.main_information_content),
                tool.getSaveRootPath());
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.main_information_title)
                .setMessage(content)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

    private void btn_feedback() {
        /*Intent intent = new Intent(MainActivity.this, FeedbackActivity.class);
        startActivity(intent);  */
        AVOSCloud.initialize(this,"a5naECbAexgSMUzsSMY5OnAs-gzGzoHsz","h6qYEWUGEGfFkVNcIS5IMb2l");
        FeedbackAgent agent = new FeedbackAgent(this);
        agent.startDefaultThreadActivity();
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
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),1);
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        private MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    /*case HandlerStatusLoadLibsFailure:
                        activity.showCommonDialog(R.string.main_snackbar_loadSo_fail, R.string.main_snackbar_btn_retry, SnackBarOnClickDoReloadFFmpeg);
                        break;  */
                    case HandlerStatusFFmpegNotSupported:
                        activity.showCommonDialog(R.string.main_snackbar_so_notAble, R.string.main_snackbar_btn_contact, SnackBarOnClickDoFeedback);
                        break;
                    case HandlerStatusPackageNameNotRight:
                        activity.showCommonDialog(R.string.main_snackbar_isPiracy, R.string.main_snackbar_sure, SnackBarOnClickDoSure);
                        break;
                    case HandlerStatusCopyFileDone:
                        activity.dialog_copyFile.dismiss();
                        if (activity.settings.getBoolean("isSortPicture", true)) {
                            Intent intent = new Intent(activity, ChooseActivity.class);
                            intent.putExtra("isFromExtra", true);
                            activity.startActivity(intent);
                        }
                        else {
                            Intent intent = new Intent(activity, MarkPictureActivity.class);
                            activity.startActivity(intent);
                        }
                        break;
                    case HandlerStatusCopyFileFail:
                        activity.dialog_copyFile.dismiss();
                        Toast.makeText(activity, R.string.main_toast_copyFileFail, Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    }

    private void shareAPP() {
        Share.showShareAppDialog(this, shareListener, MainActivity.this);
    }

    private void shareAPPSuccess() {
        Share.shareAppSuccess(this);
    }

    IUiListener shareListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            int isSuccess = -1;
            try {
                isSuccess = values.getInt("ret");
            } catch (org.json.JSONException e){
                Log.e(TAG, "doComplete: ", e);
            }
            if (isSuccess == 0) {
                shareAPPSuccess();
            }
            else {
                Log.e(TAG, "share fail, and code is:" + isSuccess);
            }
        }
    };

    private class BaseUiListener implements IUiListener {

        @Override
        public void onComplete(Object response) {
            Log.i(TAG, response.toString());
            doComplete((JSONObject) response);
        }
        protected void doComplete(JSONObject values) {
        }
        @Override
        public void onError(UiError e) {

        }
        @Override
        public void onCancel() {

        }
    }

    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
            if (isMultiSelect) {
                exitMultiSelect();
                return true;
            }
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(MainActivity.this,R.string.main_toast_confirmExit,Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                tool.cleanExternalCache(this);
                finish();
            }

            return true;

        }
        return super.onKeyDown(keyCode, event);
    }


    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            main_floatBtn_menu.collapseImmediately();
            switch (v.getId()) {
                case R.id.main_floatBtn_quick:
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("video/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),RequestCodeQuickStart);
                    break;
                case R.id.main_floatBtn_splicing:
                    btn_splicing();
                    break;
                case R.id.main_floatBtn_shotScreen:
                    btn_shotScreen();
                    break;
                case R.id.main_floatBtn_frameByFrame:
                    btn_shotFrame();
                    break;
            }
        }
    };

    private void initRecycleView() {
        RecyclerView.LayoutManager mLayoutManager;
        MainWaterFallAdapter mAdapter;

        mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mAdapter = new MainWaterFallAdapter(this, waterFallList);
        mAutoLoadMoreAdapter = new AutoLoadMoreAdapter(this, mAdapter);

        if (!isFirstBoot) {    //如果是第一次启动可能会因为初始化时缺少储存权限而闪退
            initWaterFallList();
        }
        else {
            WaterFallData data = new WaterFallData();
            data.img = null;
            data.text = res.getString(R.string.main_text_waterFall_firstUse);
            data.imgHeight = 0;
            data.isDirectory = false;
            data.isSelected = false;
            waterFallList.add(data);
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        //mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new MainWaterFallAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.ViewHolder vh) {
                String filepath =  tool.getSaveRootPath();
                String[] files = getFiles(filepath);
                if (files.length > 0) {
                    if (vh.getAdapterPosition() < files.length && vh.getAdapterPosition() != RecyclerView.NO_POSITION) {
                        int position = vh.getAdapterPosition();
                        if (isMultiSelect) {
                            boolean isSelected = waterFallList.get(position).isSelected;
                            selectedCount += isSelected ? -1 : 1;
                            waterFallList.get(position).isSelected = !isSelected;
                            mRecycleAdapter.notifyDataSetChanged();
                        }
                        else {
                            previewPicture(filepath+"/"+files[position]);
                        }
                    }
                }
            }

            @Override
            public void onItemLongClick(RecyclerView.ViewHolder vh) {
                Log.i(TAG, "call onItemLongClick");
                String filepath =  tool.getSaveRootPath();
                String[] files = getFiles(filepath);
                MainWaterFallAdapter.MyViewHolder holder2;
                try {     //避免长按vh为autoLoadMore对象导致转换类型出错闪退
                    holder2 = (MainWaterFallAdapter.MyViewHolder) vh;
                } catch (Exception e) {
                    return;
                }
                if (files.length > 0) {
                    //避免因为首次使用添加了一个 提示cardView 导致的闪退
                    if (vh.getAdapterPosition() < files.length &&
                            vh.getAdapterPosition() != RecyclerView.NO_POSITION &&
                            !isMultiSelect) {
                        showPopupMenu(holder2.img, filepath+"/"+files[vh.getAdapterPosition()], vh.getAdapterPosition());
                    }
                }
            }
        });

        mAutoLoadMoreAdapter.setOnLoadListener(new AutoLoadMoreAdapter.OnLoadListener() {
            @Override
            public void onRetry() {
                //do retry
                Log.i(TAG, "call  onRetry");
                addWaterFallList();
            }

            @Override
            public void onLoadMore() {
                //do load more
                Log.i(TAG, "call onLoadMore");
                addWaterFallList();
            }
        });
        mRecyclerView.setAdapter(mAutoLoadMoreAdapter);
        mRecycleAdapter = mRecyclerView.getAdapter();
    }

    private void previewPicture(String file) {
        //vImageWatcher.show(v, );
        Log.i(TAG, "showP file: "+file);
        File f = new File(file);
        if (f.isDirectory()) {
            List<String> imageList = new ArrayList<>();
            for (String s : getFiles(file)) {
                imageList.add(file+"/"+s);
            }
            if (imageList.size() < 1) {
                return;
            }
            //vImageWatcher.show(v, imageGroupList,  uriList);
            showPicture(imageList);
        }
        else {
            //vImageWatcher.show(v, imageGroupList,  Collections.singletonList(Uri.parse(file)));
            showPicture(file);
        }
    }

    private void addWaterFallList() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFirstBoot) {
                    waterFallList.remove(0);
                    mRecycleAdapter.notifyItemRemoved(0);
                    mRecycleAdapter.notifyDataSetChanged();
                    isFirstBoot = false;
                }

                String filepath =  tool.getSaveRootPath();
                String[] files = getFiles(filepath);//tool.getFileOrderByName(filepath, -1);

                Log.i(TAG, "files length="+files.length);

                int j = waterFallDataPager*10+10;
                if (j > files.length) {
                    j = files.length;
                }
                if (waterFallDataPager*10 > files.length) {
                    Log.i(TAG, "index out");
                    mAutoLoadMoreAdapter.showLoadComplete();
                    j = waterFallDataPager*10;
                }
                Log.i(TAG, "waterFallDataPager="+waterFallDataPager);
                Log.i(TAG, "j = "+j);
                for(int i=waterFallDataPager*10;i<j;i++) {
                    WaterFallData data = new WaterFallData();
                    //data.img = tool.getBitmapThumbnailFromFile(filepath+"/"+files[i], 400, 500);
                    data.img = filepath+"/"+files[i];
                    data.text = files[i];
                    data.imgHeight = (i % 2)*100 + 400;
                    if (new File(data.img).isDirectory()) {
                        data.text = String.format("（合集）%s", data.text);
                        data.isDirectory = true;
                        String[] imgs = getFiles(data.img);//tool.getFileOrderByName(data.img, -1);
                        if (imgs.length < 1) {
                            data.img = null;
                        }
                        else {
                            data.img = data.img+"/"+imgs[0];
                        }
                    }
                    data.isSelected = false;
                    waterFallList.add(data);
                }
                waterFallDataPager++;
                Log.i(TAG, "add waterFall data finish");
                mAutoLoadMoreAdapter.finishLoading();
                mRecycleAdapter.notifyDataSetChanged();
            }
        }, 1);
    }

    private void initWaterFallList() {
        String filepath =  tool.getSaveRootPath();
        String[] files = getFiles(filepath);//tool.getFileOrderByName(filepath, -1);

        waterFallList.clear();
        if (files.length <= 0) {
            WaterFallData data = new WaterFallData();
            data.img = null;
            data.text = res.getString(R.string.main_text_waterFall_noData);
            data.imgHeight = 0;
            data.isDirectory = false;
            data.isSelected = false;
            waterFallList.add(data);
        }

        if (files.length <= 10) {
            mAutoLoadMoreAdapter.showLoadComplete();
        }

        int j = 10;
        if (files.length < j) {
            j = files.length;
        }

        for(int i=0;i<j;i++) {
            WaterFallData data = new WaterFallData();
            //data.img = tool.getBitmapThumbnailFromFile(filepath+"/"+files[i], 400, 500);
            data.img = filepath+"/"+files[i];
            data.text = files[i];
            data.imgHeight = (i % 2)*100 + 400;
            if (new File(data.img).isDirectory()) {
                data.text = String.format("（合集）%s", data.text);
                data.isDirectory = true;
               //Log.i(TAG, "directory files:"+tool.getFileOrderByName(data.img)[0]);
                String[] imgs = getFiles(data.img);//tool.getFileOrderByName(data.img, -1);
                if (imgs.length < 1) {
                    data.img = null;
                }
                else {
                    data.img = data.img+"/"+imgs[0];
                }
            }
            data.isSelected = false;
            waterFallList.add(data);
        }
        waterFallDataPager++;
    }

    private void showPopupMenu(View view, final String img, final int pos) {
        final PopupMenu popupMenu;
        popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main_card_popup_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                popupMenu.dismiss();
                switch (item.getItemId()) {
                    case R.id.main_popupMenu_delete:
                        deletePicture(img, pos);
                        break;
                    case R.id.main_popupMenu_share:
                        sharePicture(MainActivity.this, img);
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void deletePicture(Context context, String img, final int pos, final boolean fromDir) {
        final File f = new File(img);
        showDeletePicDialog(context, f, pos, fromDir);
    }

    private void deletePicture(String img, final int pos) {
        deletePicture(this, img, pos, false);
    }

    private void sharePicture(Context context, String img) {
        File f = new File(img);
        if (f.isDirectory()) {
            showCommonDialog(R.string.main_snackbar_shareDirectoryTip, R.string.main_snackbar_sure, SnackBarOnClickDoSure);
        }
        else {
            Share.showSharePictureDialog(context, new File(img), shareListener, MainActivity.this);
        }
    }

    private void showGuide() {
        final FancyShowCaseView fancyShowCaseView1 = new FancyShowCaseView.Builder(this)
                .focusOn(findViewById(R.id.main_floatBtn_quick))
                .title(res.getString(R.string.main_guideView_startBtn))
                .showOnce("main_start")
                .build();
        final FancyShowCaseView fancyShowCaseView2 = new FancyShowCaseView.Builder(this)
                .title(res.getString(R.string.main_guideView_summary))
                .showOnce("main_summary")
                .build();
        FancyShowCaseQueue mQueue = new FancyShowCaseQueue()
                .add(fancyShowCaseView1)
                .add(fancyShowCaseView2);

        mQueue.show();
    }

    private String[] getFiles(String path) {
        String order = sp_init.getString("mainSort", "dateDest");
        boolean isDirFirst = sp_init.getBoolean("mainSortIsDirFirst", true);
        if (order != null) {
            switch (order) {
                case "dateDest":
                    return tool.getFileOrderByName(path, -1, isDirFirst);
                case "dateAsc":
                    return tool.getFileOrderByName(path, 1, isDirFirst);
                case "type":
                    return tool.getFileOrderByType(path, isDirFirst);
            }
        }

        return tool.getFileOrderByName(path, -1);
    }

    private void refreshWaterfall() {
        waterFallList.clear();
        mRecycleAdapter.notifyDataSetChanged();
        waterFallDataPager = 0;
        initRecycleView();
    }

    private void clickDeleteAllPic() {
        List<WaterFallData> selected = new ArrayList<>();
        for (WaterFallData data : waterFallList) {
            if (data.isSelected) {
                selected.add(data);
            }
        }
        deleteSelectedPic(selected);
    }

    private void deleteSelectedPic(List<WaterFallData> selected) {
        for (WaterFallData data : selected) {
            File f;
            Log.i(TAG, "data.img="+data.img);
            if (data.img != null) {
                try {
                    f = new File(data.img);
                } catch (NullPointerException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    continue;
                }
            }
            else {
                //FIXME 总觉得这样写不安全，要不还是用 data.text 来弄吧？
                String filepath =  tool.getSaveRootPath();
                String[] files = getFiles(filepath);
                try {
                    f = new File(filepath+"/"+files[waterFallList.indexOf(data)]);
                    data.isDirectory = false;   //避免因此而将f设为上一层目录，从而删掉别人珍藏多年的图片（来自开发者自己“血”的教育）
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    continue;
                }
            }

            if (data.isDirectory) {
                f = f.getParentFile();
            }
            deletePic(f);
        }
        waterFallList.removeAll(selected);
        mRecycleAdapter.notifyDataSetChanged();
    }


    private void deletePic(File f) {
        Log.i(TAG, "in deletePic, f= "+f.toString());
        if (!sp_init.getBoolean("isDeleteLocalPic", true)) {
            if (f.isDirectory()) {
                String newPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/"+f.getName();
                try {
                    tool.copyDir(f.toString(), newPath);
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                File filePath = new File(newPath);
                File[] files = filePath.listFiles();
                String[] filesl = new String[files.length];
                int i=0;
                for (File file:files) {
                    filesl[i] = file.getAbsolutePath();
                    i++;
                }
                MediaScannerConnection.scanFile(MainActivity.this, filesl, null, null);
            }
            else {
                String saveTo = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/"+f.getName();
                try {
                    tool.copyFile(f,
                            new File(saveTo));
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{saveTo}, null, null);
            }
        }

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            try {
                tool.deleteDirectory(f);
            } catch (IOException e) {
                Toast.makeText(this, R.string.main_toast_deleteDirectory_fail, Toast.LENGTH_SHORT).show();
                Log.e(TAG, Log.getStackTraceString(e));
            }
            String[] filesl = new String[files.length];
            int i=0;
            for (File file:files) {
                filesl[i] = file.getAbsolutePath();
                i++;
            }
            MediaScannerConnection.scanFile(MainActivity.this, filesl, null, null);
        }
        else {
            try {
                tool.deleteFile(f);
            } catch (IOException e) {
                Toast.makeText(this, R.string.main_toast_deleteFile_fail, Toast.LENGTH_SHORT).show();
                Log.e(TAG, Log.getStackTraceString(e));
            }
            MediaScannerConnection.scanFile(MainActivity.this, new String[]{f.getAbsolutePath()}, null, null);
        }
    }

    private void clickDeletePic(File f, int pos, boolean fromDir) {
       deletePic(f);

        if (fromDir) {
            //FIXME 应当改为直接退出imagewatcher界面而不是靠模拟返回键来退出
            try {
                Runtime.getRuntime().exec("input keyevent "+KeyEvent.KEYCODE_BACK);
            } catch (Exception e) {
                Log.e(TAG, "模拟返回键出错");
            }
        }
        else{
            if (waterFallList.size() > pos) {
                waterFallList.remove(pos);
            }
            mRecycleAdapter.notifyDataSetChanged();
        }
    }

    private void showPicture(final String image) {
        ImageLoader.cleanDiskCache(getApplicationContext());
        ImagePreview.getInstance()
                .setContext(MainActivity.this)
                .setEnableDragClose(true)
                .setShowDownButton(false)
                .setIndex(0)
                .setImage(image)
                .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                    @Override
                    public boolean onLongClick(final View view, final int pos) {
                        String[] items;
                        items = new String[] {"分享"};

                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (i == 0) {
                                    sharePicture(view.getContext(), image);
                                }
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        Log.i(TAG, "imageWatch onLongClick: show dialog");
                        return true;
                    }
                })
                .start();
    }

    private void showPicture(final List<String> fileList) {
        ImageLoader.cleanDiskCache(getApplicationContext());
        ImagePreview.getInstance()
                .setContext(MainActivity.this)
                .setEnableDragClose(true)
                .setShowDownButton(false)
                .setIndex(0)
                .setImageList(fileList)
                .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                    @Override
                    public boolean onLongClick(final View view, final int pos) {
                        String[] items;
                        items = new String[] {"分享", "删除"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                switch (i) {
                                    case 0:
                                        sharePicture(view.getContext(), fileList.get(pos));
                                        break;
                                    case 1:
                                        deletePicture(view.getContext(), fileList.get(pos), pos, true);
                                        break;
                                }
                            }
                        });
                        builder.create();
                        builder.show();
                        return false;
                    }
                })
                .start();
    }
}
