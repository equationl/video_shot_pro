package com.equationl.videoshotpro;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Button button_user;
    AlertDialog.Builder dialog;
    AlertDialog dialog2;
    android.support.design.widget.CoordinatorLayout container;
    Tools tool;
    Resources res;

    public static MainActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final int HandlerStatusLoadLibsFailure = 0;
    private static final int HandlerStatusFFmpegNotSupported = 1;
    private static final int HandlerStatusPackageNameNotRight = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        container =  (android.support.design.widget.CoordinatorLayout)findViewById(R.id.container);

        tool = new Tools();
        res = getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (i != PackageManager.PERMISSION_GRANTED) {
                showDialogTipUserRequestPermission();
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        button_user = (Button)findViewById(R.id.button_byUser);
        dialog = new AlertDialog.Builder(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        Thread t = new Thread(new MainActivity.MyThread());
        t.start();

        button_user.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
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


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_help) {
            String info_text = res.getString(R.string.main_information_content);
            String content = String.format(info_text, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
            dialog.setTitle(res.getString(R.string.main_information_title));
            dialog.setMessage(content);
            dialog.setIcon(R.mipmap.ic_launcher);
            dialog.create().show();
        } else if (id == R.id.nav_more) {
            Intent intent = new Intent(MainActivity.this, CommandActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_feedback) {
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
        } else if (id == R.id.nav_setting) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
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
            }

        }
    };

    public class MyThread implements Runnable {
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
}

