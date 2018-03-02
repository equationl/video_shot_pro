package com.equationl.videoshotpro;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.qq.e.ads.interstitial.AbstractInterstitialADListener;
import com.qq.e.ads.interstitial.InterstitialAD;
import com.qq.e.comm.util.AdError;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;


import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;

public class BuildPictureActivity extends AppCompatActivity {
    Button btn_up, btn_down, btn_done;
    TextView text_memory;
    ImageView imageTest;
    String[] fileList;
    Canvas canvas;
    Paint paint;
    Bitmap bm_test;
    float startY, stopY;
    int bWidth,bHeight;
    ProgressDialog dialog;
    int isDone=0;
    File savePath=null;
    SharedPreferences settings, sp_init;
    Tools tool = new Tools();
    Boolean isFromExtra;
    Resources res;
    Thread t, t_2;
    Tencent mTencent;
    InterstitialAD iad;
    Bitmap final_bitmap;
    IWXAPI wxApi;

    private final MyHandler handler = new MyHandler(this);

    public static BuildPictureActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final String TAG = "EL,InBuildActivity";

    private static final int HandlerStatusOutOfMemory = 10086;
    private static final int HandlerStatusBuildPictureNext = 10087;
    private static final int HandlerStatusBuildPictureDone = 10088;
    private static final int HandlerStatusBuildPictureUpdateBitmap = 10089;
    private static final int HandlerStatusBuildPictureFail = 10090;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_picture);

        Log.i("cao", "In BuildPictureActivity onCreate");

        instance = this;

        btn_up    = (Button)   findViewById(R.id.button_up);
        btn_down  = (Button)    findViewById(R.id.button_down);
        btn_done  = (Button)    findViewById(R.id.button_final_done);
        imageTest = (ImageView) findViewById(R.id.imageTest);
        text_memory = (TextView) findViewById(R.id.buildPicture_text_memory);

        res = getResources();

        updateMemoryText();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        Bundle bundle = this.getIntent().getExtras();
        fileList = bundle.getStringArray("fileList");
        isFromExtra = bundle.getBoolean("isFromExtra");

        t_2 = new Thread(new MyThread());

        mTencent = Tencent.createInstance("1106257597", this);

        //Log.i("filelist", fileList.toString());

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置样式
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setMessage("正在处理...");
        dialog.setTitle("请稍等");
        dialog.setMax(fileList.length+1);

        Toast.makeText(this,"请调整剪切字幕的位置", Toast.LENGTH_LONG).show();

        bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);

        canvas = new Canvas(bm_test);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth((float) 5);
        bHeight = bm_test.getHeight();
        bWidth = bm_test.getWidth();

        Log.i(TAG, "TEST IMAGE WIDTH="+bWidth+" HEIGHT="+bHeight);

        startY = (float) (bHeight*0.8);
        stopY = startY;
        canvas.drawLine(0,startY,bWidth,stopY,paint);
        imageTest.setImageBitmap(bm_test);

        int test[] = tool.getImageRealSize(imageTest);
        Log.i(TAG, "imageview width="+imageTest.getHeight()+" height="+imageTest.getWidth());


        btn_up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isDone == 0) {
                    bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);
                    canvas = new Canvas(bm_test);
                    startY = startY-8;
                    if (startY < 0) {
                        startY = 0;
                    }
                    stopY = startY;
                    canvas.drawLine(0,startY,bm_test.getWidth(),stopY,paint);
                    imageTest.setImageBitmap(bm_test);
                    updateMemoryText();
                }
                else {
                    try {
                        PlayerActivity.instance.finish();
                        MarkPictureActivity.instance.finish();
                        MainActivity.instance.finish();
                        ChooseActivity.instance.finish();
                    } catch (NullPointerException e) {Log.e("el", e.toString());}
                    Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        btn_down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isDone == 0) {
                    bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);
                    canvas = new Canvas(bm_test);
                    startY = startY+8;
                    if (startY > bHeight) {
                        startY = bHeight;
                    }
                    stopY = startY;
                    canvas.drawLine(0,startY,bm_test.getWidth(),stopY,paint);
                    imageTest.setImageBitmap(bm_test);
                    updateMemoryText();
                }
            }
        });

        btn_done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int test[] = tool.getImageRealSize(imageTest);
                Log.i(TAG, "imageview width="+test[0]+" height="+test[1]);
                if (isDone==1) {
                    /*Uri imageUri = Uri.fromFile(savePath);
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    shareIntent.setType("image/*");
                    startActivity(Intent.createChooser(shareIntent, "分享到"));  */
                    showShareDialog(v);
                }
                else {
                    t = new Thread(new MyThread());
                    t.start();
                    dialog.show();
                    dialog.setProgress(0);
                }
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (isDone == 1) {
                try {
                    PlayerActivity.instance.finish();
                    MarkPictureActivity.instance.finish();
                    MainActivity.instance.finish();
                } catch (NullPointerException e){}
                Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            return false;
        }else {
            return super.onKeyDown(keyCode, event);
        }

    }

    private Bitmap getBitmap(String no) {
        Bitmap bm = null;
        String extension;
        if (settings.getBoolean("isShotToJpg", true)) {
            extension = "jpg";
        }
        else {
            extension = "png";
        }

        Boolean isSetColorMode = settings.getBoolean("isSetColorMode", false);
        Boolean isSetResolution = settings.getBoolean("isSetResolution", false);
        if (isSetColorMode || isSetResolution) {
            String resolution = isSetResolution ? settings.getString("resolution_value", "2") : "1";
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Integer.parseInt(resolution);
            options.inPreferredConfig = getColorConfig();
            try {
                bm = tool.getBitmapFromFile(no, getExternalCacheDir(),extension, options);
            } catch (Exception e) {
                Log.e("EL", "获取截图失败："+e.toString());
                CrashReport.postCatchedException(e);
            }
        }

        else {
            try {
                bm = tool.getBitmapFromFile(no, getExternalCacheDir(),extension);
            } catch (Exception e) {
                Log.e("EL", "获取截图失败："+e.toString());
                CrashReport.postCatchedException(e);
            }
        }

        if (bm == null) {
            Toast.makeText(this, R.string.buildPicture_dialog_getBitmap_fail, Toast.LENGTH_LONG).show();
            finish();
        }
        return bm;
    }

    private Bitmap getCutImg() {
        for (int i=0;i<fileList.length;i++) {
            if (fileList[i].equals("cut")) {
                return getBitmap(i+"");
            }
        }
        return getBitmap(0+"");
    }


    private class MyThread implements Runnable {
        int delete_nums=0;
        Boolean isRunning = true;
        @Override
        public void run() {
            Message msg;
            int len = fileList.length;
            final_bitmap = Bitmap.createBitmap(bWidth,1, getColorConfig());
            for (int i=0;i<len;i++) {
                msg = Message.obtain();
                msg.obj = "处理第"+i+"张图片";
                msg.what = HandlerStatusBuildPictureNext;
                handler.sendMessage(msg);
                if (fileList[i].equals("cut")) {
                    try {
                        final_bitmap = addBitmap(final_bitmap,cutBitmap(getBitmap(i+"")));
                    }
                    catch (OutOfMemoryError e) {
                        showDialogOutOfMemory();
                        isRunning = false;
                        break;
                    }
                    Boolean isShow = settings.getBoolean("isMonitoredShow", false);
                    if (isShow) {
                        msg = Message.obtain();
                        msg.obj = final_bitmap;
                        msg.what = HandlerStatusBuildPictureUpdateBitmap;
                        handler.sendMessage(msg);
                    }
                }
                else if (fileList[i].equals("all")) {
                    try {
                        final_bitmap = addBitmap(final_bitmap,getBitmap(i+""));
                    }
                    catch (OutOfMemoryError e) {
                        showDialogOutOfMemory();
                        isRunning = false;
                        break;
                    }
                    Boolean isShow = settings.getBoolean("isMonitoredShow", false);
                    if (isShow) {
                        msg = Message.obtain();
                        msg.obj = final_bitmap;
                        msg.what = HandlerStatusBuildPictureUpdateBitmap;
                        handler.sendMessage(msg);
                    }
                }
                else if (fileList[i].equals("text")) {
                    try {
                        final_bitmap = addBitmap(final_bitmap,getBitmap(i+"_t"));
                    }
                    catch (OutOfMemoryError e) {
                        showDialogOutOfMemory();
                        isRunning = false;
                        break;
                    }
                    Boolean isShow = settings.getBoolean("isMonitoredShow", false);
                    if (isShow) {
                        msg = Message.obtain();
                        msg.obj = final_bitmap;
                        msg.what = HandlerStatusBuildPictureUpdateBitmap;
                        handler.sendMessage(msg);
                    }
                }
                else {
                    delete_nums++;
                }
            }
            Boolean isAddWatermark = settings.getBoolean("isAddWatermark_switch",true);
            if (isAddWatermark && isRunning) {
                Canvas canvas = new Canvas(final_bitmap);
                String watermark = settings.getString("watermark_text","Made by videoshot");
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.argb(80,150,150,150));
                textPaint.setTextSize(40);

                Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt();
                int char_height = fmi.bottom-fmi.top;

                int watermarkPosition = Integer.parseInt(settings.getString("watermark_position_value", "1"));
                Layout.Alignment align = Layout.Alignment.ALIGN_NORMAL;
                switch (watermarkPosition) {
                    case 1:
                        break;
                    case 2:
                        canvas.translate(0,final_bitmap.getHeight()/2);
                        align = Layout.Alignment.ALIGN_CENTER;
                        break;
                    case 3:
                        canvas.translate(0, final_bitmap.getHeight()-char_height);
                        break;
                }

                StaticLayout layout = new StaticLayout(watermark,textPaint,canvas.getWidth(), align,1.0F,0.0F,true);
                canvas.translate(5,0);
                layout.draw(canvas);
            }

            if (delete_nums >= len) {
                msg = Message.obtain();
                msg.obj = "你全部删除了我合成什么啊？？？";
                msg.what = HandlerStatusBuildPictureFail;
                handler.sendMessage(msg);
            }
            else if (isRunning){
                msg = Message.obtain();
                msg.obj = res.getString(R.string.buildPicture_ProgressDialog_msg_export);
                msg.what = HandlerStatusBuildPictureNext;
                handler.sendMessage(msg);
                final_bitmap = tool.addRight(final_bitmap);
                SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
                String date    =    sDateFormat.format(new    java.util.Date());
                try {
                    if(saveMyBitmap(final_bitmap,date+"-by_EL", settings.getBoolean("isReduce_switch", false))) {
                        msg = Message.obtain();
                        msg.obj = date+"-by_EL";
                        msg.what = HandlerStatusBuildPictureDone;
                        handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    //Toast.makeText(getApplicationContext(),"保存截图失败"+e,Toast.LENGTH_LONG).show();
                    msg = Message.obtain();
                    msg.obj = "保存截图失败"+e;
                    msg.what = HandlerStatusBuildPictureFail;
                    handler.sendMessage(msg);
                }
                catch (OutOfMemoryError e) {
                    showDialogOutOfMemory();
                }
            }
        }
    }

    private Bitmap cutBitmap(Bitmap bm) {
        //return Bitmap.createBitmap(bm, 0, (int)startY, bWidth, (int)(bm.getHeight()-startY));
        return tool.cutBimap(bm, (int)startY, bWidth);
    }

    private Bitmap addBitmap(Bitmap first, Bitmap second) {
        return tool.jointBitmap(first, second);
    }

    private boolean saveMyBitmap(Bitmap bmp, String bitName, boolean isReduce) throws IOException {
        boolean flag;
        try {
            if (isReduce) {
                int quality = Integer.parseInt(settings.getString("reduce_value","100"));
                savePath = tool.saveBitmap2png(bmp,bitName, Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), true, quality);
            }
            else {
                savePath = tool.saveBitmap2png(bmp,bitName, Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES));
            }
            flag = true;
        } catch (Exception e) {
            flag = false;
        }

        return flag;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i("cao", "In BuildPictureActivity onDestroy");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("cao", "In BuildPictureActivity onStop");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("cao", "In BuildPictureActivity onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("cao", "In BuildPictureActivity onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("cao", "In BuildPictureActivity onStart");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.i("cao", "In BuildPictureActivity onRestart");
    }

    private void updateMemoryText() {
        int maxMemory = ((int) Runtime.getRuntime().maxMemory())/1024/1024;
        long totalMemory = ((int) Runtime.getRuntime().totalMemory())/1024/1024;
        long freeMemory = ((int) Runtime.getRuntime().freeMemory())/1024/1024;
        Log.i(TAG,"---> maxMemory="+maxMemory+"M,totalMemory="+totalMemory+"M,freeMemory="+freeMemory+"M");
        text_memory.setText(String.format(res.getString(R.string.buildPicture_text_memory), totalMemory+"M", maxMemory+"M"));
    }

    private static class MyHandler extends Handler {
        private final WeakReference<BuildPictureActivity> mActivity;

        private MyHandler(BuildPictureActivity activity) {
            mActivity = new WeakReference<BuildPictureActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final BuildPictureActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerStatusBuildPictureNext:
                        if (msg.obj.toString().equals(activity.res.getString(R.string.buildPicture_ProgressDialog_msg_export))) {
                            activity.dialog.dismiss();
                            activity.dialog = new ProgressDialog(activity);
                            activity.dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            activity.dialog.setIndeterminate(false);
                            activity.dialog.setCancelable(false);
                            activity.dialog.setMessage(msg.obj.toString());
                            activity.dialog.setTitle("请稍等");
                            activity.dialog.show();
                        }
                        else {
                            activity.updateMemoryText();
                            activity.dialog.setProgress(activity.dialog.getProgress()+1);
                            activity.dialog.setMessage(msg.obj.toString());
                            System.gc();
                        }
                        break;

                    case HandlerStatusBuildPictureDone:
                        activity.updateMemoryText();
                        activity.dialog.dismiss();
                        activity.btn_up.setText("返回");
                        activity.btn_done.setText("分享");
                        activity.btn_down.setVisibility(View.INVISIBLE);
                        activity.isDone=1;
                        if (!activity.sp_init.getBoolean("isCloseAd", false)) {
                            activity.showAD();
                        }
                        String temp_path = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)+"/"+msg.obj.toString();
                        temp_path += activity.settings.getBoolean("isReduce_switch", false) ? ".jpg":".png";
                        MediaScannerConnection.scanFile(activity, new String[]{temp_path}, null, null);
                        Toast.makeText(activity,"处理完成！图片已保存至 "+ temp_path +" 请进入图库查看", Toast.LENGTH_LONG).show();
                        break;

                    case HandlerStatusBuildPictureUpdateBitmap:
                        Boolean isShow = activity.settings.getBoolean("isMonitoredShow", false);
                        if (isShow) {
                            activity.imageTest.setImageBitmap((Bitmap) msg.obj);
                        }
                        else {
                            DisplayMetrics dm = new DisplayMetrics();
                            dm = activity.getResources().getDisplayMetrics();
                            int screenHeight = dm.heightPixels;

                            Bitmap bm = (Bitmap) msg.obj;
                            if (bm.getHeight() > screenHeight) {
                                Bitmap newbm = Bitmap.createBitmap(bm, 0, bm.getHeight()-screenHeight, bm.getWidth(), screenHeight);
                                activity.imageTest.setImageBitmap(newbm);
                            }
                            else {
                                activity.imageTest.setImageBitmap(bm);
                            }
                        }
                        break;

                    case HandlerStatusBuildPictureFail:
                        Toast.makeText(activity,msg.obj.toString(), Toast.LENGTH_LONG).show();
                        activity.dialog.dismiss();
                        activity.isDone = 1;
                        activity.btn_up.setText("退出");
                        activity.btn_down.setVisibility(View.GONE);
                        activity.btn_done.setVisibility(View.GONE);
                        break;

                    case HandlerStatusOutOfMemory:
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.buildPicture_dialog_oom_tittle)
                                .setMessage(R.string.buildPicture_dialog_oom_content)
                                .setPositiveButton(R.string.buildPicture_btn_oom_continue, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        activity.dialog.dismiss();
                                        activity.reduceQuality();
                                        Bitmap bm_temp = activity.getCutImg();
                                        activity.bWidth = bm_temp.getWidth();
                                        bm_temp = null;
                                        if (activity.t.isAlive()) {
                                            activity.t.interrupt();
                                            activity.t = null;
                                        }
                                        activity.t_2.start();
                                        activity.dialog.show();
                                        activity.dialog.setProgress(0);
                                    }
                                })
                                .setNegativeButton(R.string.buildPicture_btn_oom_exit, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            PlayerActivity.instance.finish();
                                            MarkPictureActivity.instance.finish();
                                            MainActivity.instance.finish();
                                        } catch (NullPointerException e) {Log.e("el", e.toString());}
                                        Intent intent = new Intent(activity, MainActivity.class);
                                        activity.startActivity(intent);
                                        activity.finish();
                                    }
                                }).setCancelable(false).show();
                        break;
                }
            }
        }
    }

    private void showDialogOutOfMemory() {
        handler.sendEmptyMessage(HandlerStatusOutOfMemory);
    }

    private void reduceQuality() {
        String[] colorModes, resolutions;
        String colorMode, resolution;

        colorModes = res.getStringArray(R.array.pref_colorMode_list_values);
        resolutions = res.getStringArray(R.array.pref_resolution_list_values);
        colorMode = settings.getString("colorMode_value", "1");
        resolution = settings.getString("resolution_value", "2");

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("isMonitoredShow", false);  //关闭实时预览
        editor.putBoolean("isSetColorMode", true);    //允许更改颜色模式
        editor.putBoolean("isSetResolution", true);    //允许更改图片分辨率
        editor.putString("colorMode_value", colorModes[getArrayIndexNext(colorModes, colorMode)]);   //将颜色模式降低一个档次
        editor.putString("resolution_value", resolutions[getArrayIndexNext(resolutions, resolution)]);   //将分辨率降低一个档次

        editor.apply();
    }

    private int getArrayIndexNext(String[] s, String value) {
        int size = s.length;
        for (int i=0; i<size; i++) {
            if (s[i].equals(value)) {
                return i+1<size ? i+1:i;
            }
        }
        return size-1;
    }

    private Bitmap.Config getColorConfig() {
        Bitmap.Config mode = Bitmap.Config.ARGB_8888;
        if (settings.getBoolean("isSetColorMode", false)) {
            Log.i(TAG, "is set color mode");
            int colorMode = Integer.parseInt(settings.getString("colorMode_value", "1"));
            switch (colorMode) {
                case 1:
                    mode = Bitmap.Config.ARGB_8888;
                    break;
                case 2:
                    mode = Bitmap.Config.ARGB_4444;
                    break;
                case 3:
                    mode = Bitmap.Config.RGB_565;
                    break;
                case 4:
                    mode = Bitmap.Config.ALPHA_8;
                    break;
            }
        }
        return mode;
    }

    private void showShareDialog(View view){
        final String[] items = res.getStringArray(R.array.buildPicture_dialog_share_items);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.buildPicture_dialog_share_title);
        alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int index) {
                final Bundle params = new Bundle();
                if (index == 0 || index == 1) {
                    params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, savePath.toString());
                    params.putString(QQShare.SHARE_TO_QQ_APP_NAME, res.getString(R.string.app_name));
                    params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
                }
                switch (index) {
                    case 0:
                        //qq好友
                        params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE);
                        mTencent.shareToQQ(BuildPictureActivity.this, params, shareListener);
                        break;
                    case 1:
                        //qq空间
                        params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
                        mTencent.shareToQQ(BuildPictureActivity.this, params, shareListener);
                        break;
                    case 2:
                        //微信好友
                        shareToWX(SendMessageToWX.Req.WXSceneSession);
                        break;
                    case 3:
                        //微信朋友圈
                        shareToWX(SendMessageToWX.Req.WXSceneTimeline);
                        break;
                    case 4:
                        //更多
                        Uri imageUri = //Uri.fromFile(savePath);
                                tool.getUriFromFile(savePath, BuildPictureActivity.this);
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        shareIntent.setType("image/*");
                        startActivity(Intent.createChooser(shareIntent, "分享到"));
                        break;
                }
            }
        });
        alertBuilder.create().show();
    }


    IUiListener shareListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            Toast.makeText(BuildPictureActivity.this, "分享成功！", Toast.LENGTH_SHORT).show();
        }
    };

    private class BaseUiListener implements IUiListener {

        @Override
        public void onComplete(Object response) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Tencent.onActivityResultData(requestCode, resultCode, data, shareListener);
    }


    private InterstitialAD getIAD() {
        if (iad == null) {
            iad = new InterstitialAD(this, "1105671977", "2090734016933086");
        }
        return iad;
    }

    private void showAD() {
        getIAD().setADListener(new AbstractInterstitialADListener() {
            @Override
            public void onNoAD(AdError error) {
                Log.i(
                        "AD_DEMO",
                        String.format("LoadInterstitialAd Fail, error code: %d, error msg: %s",
                                error.getErrorCode(), error.getErrorMsg()));
            }
            @Override
            public void onADReceive() {
                Log.i("AD_DEMO", "onADReceive");
                iad.show();
            }
            @Override
            public void onADClosed() {
                showCloseAdDialog();
            }
        });
        iad.loadAD();
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

    private void shareToWX(int shareTo) {
        wxApi = WXAPIFactory.createWXAPI(this, "wx45ceac6c6d2f1aff", true);
        wxApi.registerApp("wx45ceac6c6d2f1aff");

        WXImageObject imgObj = new WXImageObject();
        imgObj.setImagePath(savePath.toString());
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        try
        {
            Bitmap thumbBmp = Bitmap.createScaledBitmap(tool.getBitmapFromFile(savePath.toString()), 128, 160, true);
            msg.setThumbImage(thumbBmp);
        }
        catch (Exception e)
        {
            Toast.makeText(this, R.string.buildPicture_toast_createThumb_fail, Toast.LENGTH_SHORT).show();
            CrashReport.postCatchedException(e);
            return;
        }

        msg.title = res.getString(R.string.main_SHARE_TO_QQ_TITLE);
        msg.description = res.getString(R.string.main_SHARE_TO_QQ_SUMMARY);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = "sharePicture";
        req.message = msg;
        req.scene = shareTo;

        // 调用api接口发送数据到微信
        if (!wxApi.sendReq(req)) {
            Toast.makeText(this, R.string.buildPicture_toast_sharePicture_fail, Toast.LENGTH_LONG).show();
        }
    }
}
