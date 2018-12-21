package com.equationl.videoshotpro;

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
import android.os.Bundle;
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
import com.equationl.videoshotpro.utils.Share;
import com.equationl.videoshotpro.utils.Utils;
import com.tencent.bugly.crashreport.CrashReport;
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
    Bitmap final_bitmap;
    Utils utils = new Utils();
    boolean isAllFullPicture = false;

    private final MyHandler handler = new MyHandler(this);

    public static BuildPictureActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final String TAG = "EL,InBuildActivity";

    private static final int HandlerStatusOutOfMemory = 10086;
    private static final int HandlerStatusBuildPictureNext = 10087;
    private static final int HandlerStatusBuildPictureDone = 10088;
    private static final int HandlerStatusBuildPictureUpdateBitmap = 10089;
    private static final int HandlerStatusBuildPictureFail = 10090;
    private static final int HandlerGetBitmapFail = 10091;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_picture);

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

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置样式
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setMessage("正在处理...");
        dialog.setTitle("请稍等");
        dialog.setMax(fileList.length+1);

        try {
            bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);
        } catch (NullPointerException e) {
            CrashReport.postCatchedException(e);
            Toast.makeText(this, R.string.buildPicture_toast_copyPreview_fail, Toast.LENGTH_SHORT).show();
            finish();
        }
        bHeight = bm_test.getHeight();
        bWidth = bm_test.getWidth();
        startY = (float) (bHeight*0.8);
        stopY = startY;

        if (!isAllFullPicture){
            Toast.makeText(this,"请调整剪切字幕的位置", Toast.LENGTH_LONG).show();
            canvas = new Canvas(bm_test);
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth((float) 5);
            canvas.drawLine(0,startY,bWidth,stopY,paint);
        }
        else {
            t = new Thread(new MyThread());
            t.start();
            dialog.show();
            dialog.setProgress(0);
        }
        imageTest.setImageBitmap(bm_test);

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
                    utils.finishActivity(PlayerActivity.instance);
                    utils.finishActivity(MainActivity.instance);
                    utils.finishActivity(ChooseActivity.instance);
                    utils.finishActivity(MarkPictureActivity2.instance);
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
                if (isDone==1) {
                    Share.showSharePictureDialog(BuildPictureActivity.this, savePath, shareListener, BuildPictureActivity.this);
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
                utils.finishActivity(PlayerActivity.instance);
                utils.finishActivity(MainActivity.instance);
                utils.finishActivity(ChooseActivity.instance);
                utils.finishActivity(MarkPictureActivity2.instance);
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
            handler.sendEmptyMessage(HandlerGetBitmapFail);
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
        isAllFullPicture = true;
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
                String watermark = settings.getString("watermark_text","使用 隐云图解制作 生成");
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
                SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
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
        return tool.cutBitmap(bm, (int)startY, bWidth);
    }

    private Bitmap addBitmap(Bitmap first, Bitmap second) {
        return tool.jointBitmap(first, second);
    }

    private boolean saveMyBitmap(Bitmap bmp, String bitName, boolean isReduce) throws IOException {
        boolean flag;
        try {
            if (isReduce) {
                int quality = Integer.parseInt(settings.getString("reduce_value","100"));
                savePath = tool.saveBitmap2png(bmp,bitName, new File(tool.getSaveRootPath()), true, quality);
            }
            else {
                savePath = tool.saveBitmap2png(bmp,bitName, new File(tool.getSaveRootPath()));
            }
            flag = true;
        } catch (Exception e) {
            flag = false;
        }

        return flag;
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
                        String temp_path = activity.tool.getSaveRootPath()+"/"+msg.obj.toString();
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
                        try {   //避免非正常退出导致的闪退
                            activity.dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                            Log.i(TAG, "At HandlerStatusBuildPictureFail, dismiss dialog fail!");
                        }
                        activity.isDone = 1;
                        activity.btn_up.setText("退出");
                        activity.btn_down.setVisibility(View.GONE);
                        activity.btn_done.setVisibility(View.GONE);
                        break;

                    case HandlerGetBitmapFail:
                        Toast.makeText(activity, R.string.buildPicture_dialog_getBitmap_fail, Toast.LENGTH_LONG).show();
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
                                        activity.utils.finishActivity(PlayerActivity.instance);
                                        activity.utils.finishActivity(MainActivity.instance);
                                        activity.utils.finishActivity(MarkPictureActivity2.instance);
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

}
