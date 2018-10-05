package com.equationl.videoshotpro;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.rom.HuaweiUtils;
import com.equationl.videoshotpro.rom.MeizuUtils;
import com.equationl.videoshotpro.rom.MiuiUtils;
import com.equationl.videoshotpro.rom.QikuUtils;
import com.equationl.videoshotpro.rom.RomUtils;
import com.equationl.videoshotpro.utils.GlideSimpleLoader;
import com.equationl.videoshotpro.utils.OnRecylerViewItemClickListener;
import com.equationl.videoshotpro.utils.Share;
import com.equationl.videoshotpro.utils.Utils;
import com.equationl.videoshotpro.utils.WaterFallData;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.github.ielse.imagewatcher.ImageWatcher;
import com.github.ielse.imagewatcher.ImageWatcherHelper;
import com.qq.e.ads.banner.ADSize;
import com.qq.e.ads.banner.AbstractBannerADListener;
import com.qq.e.ads.banner.BannerView;
import com.qq.e.comm.util.AdError;
import com.tencent.bugly.Bugly;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.yancy.gallerypick.config.GalleryConfig;
import com.yancy.gallerypick.config.GalleryPick;
import com.yancy.gallerypick.inter.IHandlerCallBack;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import me.solidev.loadmore.AutoLoadMoreAdapter;
import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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
    DrawerLayout drawer;
    ViewGroup bannerContainer;
    BannerView bv;
    int activityResultMode = 0;
    Boolean isFirstBoot = false;
    boolean isTranslucentStatus = false;
    IWXAPI wxApi;
    Utils utils = new Utils();
    FloatingActionsMenu main_floatBtn_menu;
    FloatingActionButton main_floatBtn_quick;
    FloatingActionButton main_floatBtn_splicing;
    FloatingActionButton main_floatBtn_shotScreen;
    FloatingActionButton main_floatBtn_frameByFrame;

    AutoLoadMoreAdapter mAutoLoadMoreAdapter;

    ImageWatcherHelper vImageWatcher;
    ImageWatcher.OnPictureLongPressListener mOnPictureLongPressListener;

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private MainWaterFallAdapter mAdapter;

    List<WaterFallData> waterFallList = new ArrayList<>();
    int waterFallDataPager = 0;

    private final MyHandler handler = new MyHandler(this);
    public static MainActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final int HandlerStatusLoadLibsFailure = 0;
    private static final int HandlerStatusFFmpegNotSupported = 1;
    private static final int HandlerStatusPackageNameNotRight = 2;
    private static final int HandlerStatusCopyFileDone = 3;
    private static final int IntentResultCodeMediaProjection = 10;
    private static final int ActivityResultFrameByFrame = 100;

    private static final int RequestCodeQuickStart = 1000;

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
            isTranslucentStatus = true;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        utils.finishActivity(LauncherActivity.instance);

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
            else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                isFirstBoot = true;
                showDialogTipUserRequestPermission();
            }
            else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                isFirstBoot = true;
                showDialogTipUserRequestPermission();
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //button_user = findViewById(R.id.btn_main_quickStart);
        //button_splicing = (Button)findViewById(R.id.btn_main_splicing);
        //button_ffmpeg = (Button)findViewById(R.id.btn_main_ffmpeg);
        //button_floatBtn = (Button)findViewById(R.id.btn_main_floatBtn);
        //button_help = (Button)findViewById(R.id.btn_main_help);
        //button_setting = (Button)findViewById(R.id.btn_main_setting);

        main_floatBtn_menu = (FloatingActionsMenu) findViewById(R.id.main_floatBtn_menu);
        main_floatBtn_quick = (FloatingActionButton) findViewById(R.id.main_floatBtn_quick);
        main_floatBtn_splicing = (FloatingActionButton) findViewById(R.id.main_floatBtn_splicing);
        main_floatBtn_shotScreen = (FloatingActionButton) findViewById(R.id.main_floatBtn_shotScreen);
        main_floatBtn_frameByFrame = (FloatingActionButton) findViewById(R.id.main_floatBtn_frameByFrame);

        mRecyclerView = (RecyclerView) findViewById(R.id.main_recyclerView);

        //main_floatBtn_menu.setClosedOnTouchOutside(true);
        //main_floatBtn_menu.hideMenuButton(false);

        main_floatBtn_quick.setOnClickListener(clickListener);
        main_floatBtn_splicing.setOnClickListener(clickListener);
        main_floatBtn_shotScreen.setOnClickListener(clickListener);
        main_floatBtn_frameByFrame.setOnClickListener(clickListener);

        initRecycleView();

        if (!sp_init.getBoolean("isCloseAd", false)) {
            initBanner();
            bv.loadAD();
        } else {
            mRecyclerView.setPadding(0,0,0,0);
        }


        //main_floatBtn_menu.showMenuButton(true);

        /*main_floatBtn_menu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (main_floatBtn_menu.isOpened()) {
                    //Toast.makeText(MainActivity.this, main_floatBtn_menu.getMenuButtonLabelText(), Toast.LENGTH_SHORT).show();
                }

                main_floatBtn_menu.toggle(true);
            }
        });   */


        //text_bottom = (TextView)findViewById(R.id.text_main_bottom);
        //text_bottom.getPaint().setFlags(Paint. UNDERLINE_TEXT_FLAG );

        bannerContainer = (ViewGroup) findViewById(R.id.main_bannerContainer);

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

        int userFlagID = sp_init.getInt("userFlagID", 0);
        if (userFlagID == 0) {
            userFlagID = tool.createID();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putInt("userFlagID", userFlagID);
            editor.apply();
        }
        Bugly.init(getApplicationContext(), "41a66442fd", false);

        galleryConfig = new GalleryConfig.Builder()
                .imageLoader(new GlideImageLoader())
                .iHandlerCallBack(iHandlerCallBack)
                .provider("com.equationl.videoshotpro.fileprovider")
                .multiSelect(true, 100)
                .build();

        showGuide();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Tencent.onActivityResultData(requestCode,resultCode,data, shareListener);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IntentResultCodeMediaProjection) {
                Log.i("EL", "try Start Service");
                utils.finishActivity(BuildPictureActivity.instance);
                FloatWindowsService.setResultData(data);
                Intent startService = new Intent(this, FloatWindowsService.class);
                startService(startService);
            }
            else if (activityResultMode == ActivityResultFrameByFrame) {
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
            else if (requestCode == RequestCodeQuickStart){
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
            else if (requestCode == 123) {
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
        /*else {
            Toast.makeText(getApplicationContext(),R.string.main_toast_chooseFile_fail,Toast.LENGTH_LONG).show();
        }*/
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
            /*case R.id.nav_support:
                showSupportDialog();
                break;   */
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
                        SharedPreferences.Editor editor = sp_init.edit();
                        editor.putBoolean("isFirstBoot", true);
                        editor.apply();
                        finish();
                    }
                }).setCancelable(false).show();
    }

    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION}
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
                    } else {
                        //Toast.makeText(this,  R.string.main_toast_getPermission_success, Toast.LENGTH_SHORT).show();
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

    private class MyThread implements Runnable {
        @Override
        public void run() {
            loadLib();
            utils.finishActivity(MarkPictureActivity.instance);
            utils.finishActivity(ChooseActivity.instance);
            utils.finishActivity(BuildPictureActivity.instance);
            utils.finishActivity(PlayerActivity.instance);
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
        if (RomUtils.checkIsMeizuRom()) {
            return meizuPermissionCheck(context);
        }
        if (RomUtils.checkIsMiuiRom()) {
            return miuiPermissionCheck(context);
        }

        else {
            Boolean result = true;
            if (Build.VERSION.SDK_INT >= 23) {
                /*try {
                    Class clazz = Settings.class;
                    Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
                    result = (Boolean) canDrawOverlays.invoke(null, context);
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }   */
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
        /*String content = String.format(
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
        dialog.show();   */
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(intent);
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
        String info_text = res.getString(R.string.main_information_content);
        String content = String.format(info_text, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
        dialog.setTitle(res.getString(R.string.main_information_title));
        dialog.setMessage(content);
        dialog.setIcon(R.mipmap.ic_launcher);
        dialog.create().show();
    }

    private void btn_feedback() {
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

        //Fixme
        Intent intent = new Intent(MainActivity.this, FeedbackActivity.class);
        startActivity(intent);
            /*Intent data=new Intent(Intent.ACTION_SENDTO);
            data.setData(Uri.parse("mailto:admin@likehide.com"));
            data.putExtra(Intent.EXTRA_SUBJECT, this.getResources().getString(R.string.main_mail_title));
            data.putExtra(Intent.EXTRA_TEXT, mail_content);
            startActivity(data);   */
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

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        private MyHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerStatusLoadLibsFailure:
                        Snackbar snackbar = Snackbar.make(activity.container, R.string.main_snackbar_loadSo_fail, Snackbar.LENGTH_LONG);
                        snackbar.setAction(R.string.main_snackbar_btn_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                activity.loadLib();
                            }
                        });
                        snackbar.setActionTextColor(Color.BLUE);
                        snackbar.show();
                        break;
                    case HandlerStatusFFmpegNotSupported:
                        Snackbar snackbar2 = Snackbar.make(activity.container, R.string.main_snackbar_so_notAble, Snackbar.LENGTH_SHORT);
                        snackbar2.setAction(R.string.main_snackbar_btn_contact, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String versionName;
                                int currentapiVersion=0;
                                try {
                                    PackageManager packageManager = activity.getPackageManager();
                                    PackageInfo packInfo = packageManager.getPackageInfo(activity.getPackageName(),0);
                                    versionName = packInfo.versionName;
                                    currentapiVersion=android.os.Build.VERSION.SDK_INT;
                                }
                                catch (Exception ex) {
                                    versionName = "NULL";
                                }
                                String mail_content = String.format(activity.getResources().getString(R.string.main_mail_content),
                                        versionName, currentapiVersion+"", android.os.Build.MODEL);
                                Intent data=new Intent(Intent.ACTION_SENDTO);
                                data.setData(Uri.parse("mailto:admin@likehide.com"));
                                data.putExtra(Intent.EXTRA_SUBJECT, activity.getResources().getString(R.string.main_mail_title));
                                data.putExtra(Intent.EXTRA_TEXT, mail_content);
                                activity.startActivity(data);
                            }
                        });
                        snackbar2.setActionTextColor(Color.BLUE);
                        snackbar2.show();
                        break;
                    case HandlerStatusPackageNameNotRight:
                        Snackbar snackbar3 = Snackbar.make(activity.container, R.string.main_snackbar_isPiracy, Snackbar.LENGTH_LONG);
                        snackbar3.show();
                        break;
                    case HandlerStatusCopyFileDone:
                        if (activity.settings.getBoolean("isSortPicture", true)) {
                            Intent intent = new Intent(activity, ChooseActivity.class);
                            activity.startActivity(intent);
                        }
                        else {
                            Intent intent = new Intent(activity, MarkPictureActivity.class);
                            activity.startActivity(intent);
                        }
                        break;
                }
            }
        }
    }

    private void shareAPP() {
        /*final String[] items = res.getStringArray(R.array.main_dialog_shareAPP_items);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.buildPicture_dialog_share_title);
        alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int index) {
                switch (index) {
                    case 0:
                        //qq
                        shareAPPByQq();
                        break;
                    case 1:
                        //微信朋友圈
                        shareAPPByWx(SendMessageToWX.Req.WXSceneTimeline);
                        break;
                    case 2:
                        //微信好友
                        shareAPPByWx(SendMessageToWX.Req.WXSceneSession);
                        break;
                }
            }
        });
        alertBuilder.create().show();  */
        Share.showShareAppDialog(this, shareListener, MainActivity.this);
    }

    /*private void shareAPPByWx(int shareTo) {
        wxApi = WXAPIFactory.createWXAPI(this, "wx45ceac6c6d2f1aff", true);
        wxApi.registerApp("wx45ceac6c6d2f1aff");

        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = "http://sj.qq.com/myapp/detail.htm?apkName=com.equationl.videoshotpro";//收到分享的好友点击会跳转到这个地址里面去
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = res.getString(R.string.main_SHARE_TO_QQ_TITLE);
        msg.description = res.getString(R.string.main_SHARE_TO_QQ_SUMMARY);
        try
        {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.preview);
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 147, 237, true);
            bmp.recycle();
            msg.setThumbImage(thumbBmp);
        }
        catch (Exception e)
        {
            Toast.makeText(this, R.string.main_toast_createThumb_fail, Toast.LENGTH_SHORT).show();
            CrashReport.postCatchedException(e);
            return;
        }
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = "shareAPP";
        req.message = msg;
        req.scene = shareTo;

        // 调用api接口发送数据到微信
        wxApi.sendReq(req);
        Share.shareAppToWx(this, shareTo);
    }   */

    /*private void shareAPPByQq() {
        Tencent mTencent = Tencent.createInstance("1106257597", this);
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_APP);
        params.putString(QQShare.SHARE_TO_QQ_TITLE, res.getString(R.string.main_SHARE_TO_QQ_TITLE));
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, res.getString(R.string.main_SHARE_TO_QQ_SUMMARY));
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, res.getString(R.string.main_SHARE_TO_QQ_IMAGE_URL));
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, res.getString(R.string.main_SHARE_TO_QQ_APP_NAME));
        //params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
        mTencent.shareToQQ(MainActivity.this, params, shareListener);
        Share.shareAppToQQ(this, shareListener, MainActivity.this);
    }   */

    private void shareAPPSuccess() {
        /*sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);
        Toast.makeText(MainActivity.this, "分享成功！", Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = sp_init.edit();
        editor.putBoolean("isCloseAd", true);
        editor.apply();  */
        Share.shareAppSuccess(this);
        /*bv.destroy();
        mRecyclerView.setPadding(0,0,0,0); */
    }

    IUiListener shareListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            int isSuccess = -1;
            try {
                isSuccess = values.getInt("ret");
            } catch (org.json.JSONException e){}
            if (isSuccess == 0) {
                shareAPPSuccess();
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

    private void initBanner() {
        bv = new BannerView(this, ADSize.BANNER, "1105671977", "1000732026922833");
        bv.setRefresh(30);
        bv.setShowClose(true);
        bv.setADListener(new AbstractBannerADListener() {
            @Override
            public void onNoAD(AdError error) {
                Log.i(
                        "AD_DEMO",
                        String.format("Banner onNoAD，eCode = %d, eMsg = %s", error.getErrorCode(),
                                error.getErrorMsg()));
            }
            @Override
            public void onADReceiv() {
                Log.i("AD_DEMO", "ONBannerReceive");
                if (bannerContainer.getParent()!=null) {   //FIXME 不确定是否会影响正常计算广告
                    bannerContainer.removeAllViews();
                }
                bannerContainer.addView(bv);
            }
            @Override
            public void onADClosed() {
                showCloseAdDialog();
                mRecyclerView.setPadding(0,0,0,0);
            }
        });
        //bannerContainer.addView(bv);
    }

    private void showCloseAdDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.dialog_closeAD_title)
                .setMessage(R.string.dialog_closeAD_content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (!vImageWatcher.handleBackPressed() && !drawer.isDrawerOpen(GravityCompat.START)) {    //没有打开预览图片或者侧边栏
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    Toast.makeText(MainActivity.this,R.string.main_toast_confirmExit,Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                } else {
                    tool.cleanExternalCache(this);
                    finish();
                    System.exit(0);
                }
                return true;
            }

            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }

            return false;
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
                    intent.addCategory(intent.CATEGORY_OPENABLE);
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
        if (!isFirstBoot) {    //如果是第一次启动可能会因为初始化时缺少储存权限而闪退
            initWaterFallList();
        }
        else {
            WaterFallData data = new WaterFallData();
            data.img = null;
            data.text = res.getString(R.string.main_text_waterFall_firstUse);
            data.imgHeight = 0;
            data.isDirectory = false;
            waterFallList.add(data);
        }
        mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mAdapter = new MainWaterFallAdapter(this, waterFallList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        //mRecyclerView.setAdapter(mAdapter);


        mAutoLoadMoreAdapter = new AutoLoadMoreAdapter(this, mAdapter);
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

        mRecyclerView.addOnItemTouchListener(new OnRecylerViewItemClickListener(mRecyclerView) {
            @Override
            public void onItemClick(RecyclerView.ViewHolder vh) {
                //Toast.makeText(MainActivity.this,vh.getAdapterPosition()+"",Toast.LENGTH_SHORT).show();
                String filepath =  tool.getSaveRootPath();
                String[] files = tool.getFileOrderByName(filepath, -1);
                //ImageZoom.show(MainActivity.this, filepath+"/"+files[vh.getAdapterPosition()], ImageUrlType.LOCAL);
                MainWaterFallAdapter.MyViewHolder holder2 = (MainWaterFallAdapter.MyViewHolder) vh;
                if (files.length > 0) {
                    showPicture(holder2.img, filepath+"/"+files[vh.getAdapterPosition()]);
                }
            }

            @Override
            public void onItemLongClick(RecyclerView.ViewHolder vh) {
                /*if (vh.getLayoutPosition()!=waterFallList.size()-1) {
                    //helper.startDrag(vh);
                }
                Toast.makeText(MainActivity.this,vh.getAdapterPosition()+"buke",Toast.LENGTH_SHORT).show();  */
                String filepath =  tool.getSaveRootPath();
                String[] files = tool.getFileOrderByName(filepath, -1);
                MainWaterFallAdapter.MyViewHolder holder2 = (MainWaterFallAdapter.MyViewHolder) vh;
                if (files.length > 0) {
                    showPopupMenu(holder2.img, filepath+"/"+files[vh.getAdapterPosition()], vh.getAdapterPosition());
                }
            }
        });


        initOnPictureLongPressListener();
        vImageWatcher = ImageWatcherHelper.with(this, new GlideSimpleLoader()) // 一般来讲， ImageWatcher 需要占据全屏的位置
                .setTranslucentStatus(!isTranslucentStatus ? Utils.calcStatusBarHeight(this) : 0) // 如果是透明状态栏，你需要给ImageWatcher标记 一个偏移值，以修正点击ImageView查看的启动动画的Y轴起点的不正确
                .setErrorImageRes(R.mipmap.error_picture) // 配置error图标 如果不介意使用lib自带的图标，并不一定要调用这个API
                .setOnPictureLongPressListener(mOnPictureLongPressListener); // 长按图片的回调，你可以显示一个框继续提供一些复制，发送等功能
    }

    private void showPicture(ImageView v, String file) {
        //vImageWatcher.show(v, );
        Log.i(TAG, "showP file: "+file);
        File f = new File(file);
        SparseArray<ImageView> imageGroupList = new SparseArray<>();
        imageGroupList.put(0, v);
        if (f.isDirectory()) {
            List<Uri> uriList = new LinkedList<>();
            for (String s : tool.getFileOrderByName(file, -1)) {
                uriList.add(Uri.parse(file+"/"+s));
            }
            if (uriList.size() < 1) {
                return;
            }
            vImageWatcher.show(v, imageGroupList,  uriList);
        }
        else {
            vImageWatcher.show(v, imageGroupList,  Collections.singletonList(Uri.parse(file)));
        }
    }

    private List<WaterFallData> addWaterFallList() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFirstBoot) {
                    waterFallList.remove(0);
                    mRecyclerView.getAdapter().notifyItemRemoved(0);
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                    isFirstBoot = false;
                }

                String filepath =  tool.getSaveRootPath();
                String[] files = tool.getFileOrderByName(filepath, -1);

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
                        data.text = "（合集）" + data.text;
                        data.isDirectory = true;
                        String[] imgs = tool.getFileOrderByName(data.img, -1);
                        if (imgs.length < 1) {
                            data.img = null;
                        }
                        else {
                            data.img = data.img+"/"+imgs[0];
                        }
                    }
                    waterFallList.add(data);
                }
                waterFallDataPager++;
                Log.i(TAG, "add waterFall data finish");
                mAutoLoadMoreAdapter.finishLoading();
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }, 1);
        return waterFallList;
    }

    private List<WaterFallData> initWaterFallList() {
        String filepath =  tool.getSaveRootPath();
        String[] files = tool.getFileOrderByName(filepath, -1);

        waterFallList.clear();
        if (files.length <= 0) {
            WaterFallData data = new WaterFallData();
            data.img = null;
            data.text = res.getString(R.string.main_text_waterFall_noData);
            data.imgHeight = 0;
            data.isDirectory = false;
            waterFallList.add(data);
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
                data.text = "（合集）" + data.text;
                data.isDirectory = true;
               //Log.i(TAG, "directory files:"+tool.getFileOrderByName(data.img)[0]);
                String[] imgs = tool.getFileOrderByName(data.img, -1);
                if (imgs.length < 1) {
                    data.img = null;
                }
                else {
                    data.img = data.img+"/"+imgs[0];
                }
            }
            waterFallList.add(data);
        }
        waterFallDataPager++;
        return waterFallList;
    }

    private void initOnPictureLongPressListener() {
        mOnPictureLongPressListener = new ImageWatcher.OnPictureLongPressListener() {
            @Override
            public void onPictureLongPress(ImageView v, final Uri url, final int pos) {
                //Toast.makeText(MainActivity.this, "call long press:"+url, Toast.LENGTH_SHORT).show();
                String[] items;
                //Log.i(TAG, "in longPress path= "+new File(url.toString()).getParent()+" RootPath= "+tool.getSaveRootPath());
                if (!new File(url.toString()).getParent().equals(tool.getSaveRootPath())) {
                    items = new String[] {"分享", "删除"};
                }
                else {
                    items = new String[] {"分享"};
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                sharePicture(url.toString());
                                //Toast.makeText(MainActivity.this, "分享 "+url, Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                deletePicture(url.toString(), pos, true);
                                //Toast.makeText(MainActivity.this, "删除 "+url, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        };
    }

    private void showPopupMenu(View view, final String img, final int pos) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.main_card_popup_menu, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.main_popupMenu_delete:
                        deletePicture(img, pos);
                        //Toast.makeText(MainActivity.this, "删除 "+img, Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.main_popupMenu_share:
                        sharePicture(img);
                        //Toast.makeText(MainActivity.this, "分享 "+img, Toast.LENGTH_SHORT).show();
                        break;
                }
                return false;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                // 控件消失时的事件
            }
        });
        popupMenu.show();
    }

    private void deletePicture(String img, final int pos, final boolean fromDir) {
        final File f = new File(img);

        Snackbar.make(getWindow().getDecorView(),R.string.main_snackbar_deleteTip, Snackbar.LENGTH_LONG).setAction(R.string.main_snackbar_deleteSure, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (f.isDirectory()) {
                    tool.deleteDirectory(f);
                }
                else {
                    tool.deleteFile(f);
                }
                if (fromDir) {
                    //FIXME 应当改为直接退出imagewatcher界面而不是靠模拟返回键来退出
                    try {
                        Runtime.getRuntime().exec("input keyevent "+KeyEvent.KEYCODE_BACK);
                    } catch (Exception e) {
                        Log.e(TAG, "模拟返回键出错");
                    }
                }
                else{
                    waterFallList.remove(pos);
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        }).show();
    }

    private void deletePicture(String img, final int pos) {
        deletePicture(img, pos, false);
    }

    private void sharePicture(String img) {
        File f = new File(img);
        if (f.isDirectory()) {
            Snackbar.make(getWindow().getDecorView(),R.string.main_snackbar_shareDirectoryTip, Snackbar.LENGTH_LONG).show();
        }
        else {
            Share.showSharePictureDialog(this, new File(img), shareListener, MainActivity.this);
        }
    }

    /*@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (main_floatBtn_menu.isExpanded()) {
                    main_floatBtn_menu.setEnabled(false);
                }
                main_floatBtn_menu.collapse();
                break;
            case MotionEvent.ACTION_UP:
                main_floatBtn_menu.setEnabled(true);
                break;
        }
        return super.dispatchTouchEvent(ev);

    }   */


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

}