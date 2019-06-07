package com.equationl.videoshotpro;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.Utils;

public class ShortCutsActivity extends AppCompatActivity {
    Tools tool;

    @SuppressLint("StaticFieldLeak")
    public static ShortCutsActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final int RequestCodeQuickStart = 1000;
    private static final int IntentResultCodeMediaProjection = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_cuts);

        instance = this;
        tool = new Tools();

        String mode = getIntent().getData().toString();

        if (mode.equals("quick")) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),RequestCodeQuickStart);
        }
        else if (mode.equals("float")) {
            showFloatBtn();
        }
        else {
            Toast.makeText(this, R.string.shortcut_toast_unknownMode, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IntentResultCodeMediaProjection) {
                Log.i("EL", "try Start Service");
                Utils.finishActivity(BuildPictureActivity.instance);

                FloatWindowsService.setResultData(data);
                Intent startService = new Intent(this, FloatWindowsService.class);
                startService(startService);
                finish();
            }

            else if (requestCode == RequestCodeQuickStart){
                //快速开始
                Uri uri = data.getData();
                Intent intent = new Intent(ShortCutsActivity.this, PlayerActivity.class);
                intent.setData(uri);
                startActivity(intent);
            }
            else {
                finish();
            }
        }
        else {
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showFloatBtn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, R.string.main_toast_unSupportFloatBtn, Toast.LENGTH_SHORT).show();
            return;
        }

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                IntentResultCodeMediaProjection);
    }
}
