package com.equationl.videoshotpro;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.Utils;
import com.tencent.tauth.Tencent;

public class ShortCutsActivity extends AppCompatActivity {
    Tools tool;

    private static final int RequestCodeQuickStart = 1000;
    private static final int IntentResultCodeMediaProjection = 10;

    Utils utils = new Utils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_cuts);

        //Toast.makeText(this, getIntent().getData().toString(), Toast.LENGTH_SHORT).show();

        tool = new Tools();

        String mode = getIntent().getData().toString();

        if (mode.equals("quick")) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择视频文件"),RequestCodeQuickStart);
        }
        else if (mode.equals("float")) {
            showFloatBtn();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IntentResultCodeMediaProjection) {
                Log.i("EL", "try Start Service");
                utils.finishActivity(BuildPictureActivity.instance);

                FloatWindowsService.setResultData(data);
                Intent startService = new Intent(this, FloatWindowsService.class);
                startService(startService);
            }

            else if (requestCode == RequestCodeQuickStart){
                //快速开始
                Uri uri = data.getData();
                //String path = uri.getPath();
                String path = tool.getImageAbsolutePath(this, uri);
                Intent intent = new Intent(ShortCutsActivity.this, PlayerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("path", path);
                intent.putExtras(bundle);
                intent.setData(uri);
                startActivity(intent);
            }
        }
        /*else {
            Toast.makeText(getApplicationContext(),R.string.main_toast_chooseFile_fail,Toast.LENGTH_LONG).show();
        }*/
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showFloatBtn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, R.string.main_toast_unSupportFloatBtn, Toast.LENGTH_SHORT).show();
            return;
        }

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(this.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                IntentResultCodeMediaProjection);
    }
}
