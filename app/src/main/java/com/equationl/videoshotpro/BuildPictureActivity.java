package com.equationl.videoshotpro;

import android.annotation.SuppressLint;
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
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.cropBox.CropImageView;
import com.equationl.videoshotpro.utils.Share;
import com.equationl.videoshotpro.utils.Utils;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;


import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.glide.ImageLoader;

public class BuildPictureActivity extends AppCompatActivity {
    CropImageView imageViewPreview;
    String[] fileList;
    Bitmap bm_test;
    float startY, stopY;
    int bWidth,bHeight;
    ProgressDialog dialog;
    boolean isBuildDone = false, isBuildFail = false;
    int SubtitleTop, SubtitleBottom;
    File savePath=null;
    SharedPreferences settings, sp_init;
    Tools tool = new Tools();
    Boolean isFromExtra, isAutoBuild, isCutAllPicture;
    Resources res;
    Thread t, t_2;
    Bitmap final_bitmap;
    boolean isAllFullPicture = false;
    boolean drawDone = false;
    boolean isShowingOOMDialog = false;

    private final MyHandler handler = new MyHandler(this);

    @SuppressLint("StaticFieldLeak")
    public static BuildPictureActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final String TAG = "EL,InBuildActivity";

    private static final int HandlerStatusOutOfMemory = 10086;
    private static final int HandlerStatusBuildPictureNext = 10087;
    private static final int HandlerStatusBuildPictureDone = 10088;
    private static final int HandlerStatusBuildPictureUpdateBitmap = 10089;
    private static final int HandlerStatusBuildPictureFail = 10090;
    private static final int HandlerStatusBuildPictureAddFail = 10091;
    private static final int HandlerGetBitmapFail = 10092;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_picture);

        instance = this;

        init();
    }

    void init() {
        imageViewPreview = findViewById(R.id.buildPicture_image_preview);

        res = getResources();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        isCutAllPicture = settings.getBoolean("isCutAllPicture", false);

        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            fileList = bundle.getStringArray("fileList");
            isFromExtra = bundle.getBoolean("isFromExtra", false);
            isAutoBuild = bundle.getBoolean("isAutoBuild", false);
            SubtitleTop = bundle.getInt("SubtitleTop", 0);
            SubtitleBottom = bundle.getInt("SubtitleBottom", 0);
        }
        else {
            Toast.makeText(this, R.string.buildPicture_toast_getBundle_fail, Toast.LENGTH_SHORT).show();
            finish();
        }

        t_2 = new Thread(new BuildThread());

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
        } catch (RuntimeException e) {
            CrashReport.postCatchedException(e);
            Toast.makeText(this, R.string.buildPicture_toast_copyPreview_fail, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (bm_test == null) {
            Log.e(TAG, "onCreate: bm is null!");
            Toast.makeText(this, R.string.buildPicture_toast_copyPreview_fail, Toast.LENGTH_SHORT).show();
            finish();
        }
        //see: https://bugly.qq.com/v2/crash-reporting/crashes/41a66442fd/33904?pid=1
        try {
            bHeight = bm_test.getHeight();
            bWidth = bm_test.getWidth();
        } catch (NullPointerException e) {
            Log.e(TAG, "init: get bitmap height and width:", e);
            Toast.makeText(this, R.string.buildPicture_toast_getBitmapSize_fail, Toast.LENGTH_LONG).show();
            finish();
        }
        startY = (float) (bHeight*0.8);
        stopY = startY;


        imageViewPreview.setImageBitmap(bm_test);

        imageViewPreview.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "imageViewPreview.post: draw done!");
                drawDone = true;
            }
        });

        if (isAutoBuild || isAllFullPicture) {
            t = new Thread(new BuildThread());
            t.start();
            dialog.show();
            dialog.setProgress(0);
        }
        else {
            setTitle(R.string.title_activity_markSubtitle);
            Toast.makeText(this,"请调整剪切字幕的位置", Toast.LENGTH_LONG).show();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        else {
            Log.i(TAG, "init: actionBar = null");
        }

        imageViewPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "imageViewPreview onClick");
                if (isBuildDone && !isBuildFail) {
                    ImageLoader.cleanDiskCache(getApplicationContext());
                    ImagePreview.getInstance()
                            .setContext(BuildPictureActivity.this)
                            .setEnableDragClose(true)
                            .setShowDownButton(false)
                            .setIndex(0)
                            .setImage(savePath.toString())
                            .start();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.acticity_build_picture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == R.id.build_menu_next) {
            if (isBuildDone) {
                if (!isBuildFail) {
                    Share.showSharePictureDialog(BuildPictureActivity.this, savePath, shareListener, BuildPictureActivity.this);
                }
            }
            else {
                t = new Thread(new BuildThread());
                t.start();
                dialog.show();
                dialog.setProgress(0);
            }
        }
        else if (item.getItemId() == android.R.id.home) {
            if (isBuildDone) {
                Utils.finishActivity(PlayerActivity.instance);
                Utils.finishActivity(ChooseActivity.instance);
                Utils.finishActivity(MarkPictureActivity.instance);
                if (isFromExtra) {
                    Utils.finishActivity(MainActivity.instance);
                    Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish();
            }
            else {
                if (isFromExtra) {
                    Utils.finishActivity(MainActivity.instance);
                    Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isBuildDone) {
            if (isBuildFail) {
                imageViewPreview.setDrawBox(false);
                setTitle(R.string.title_activity_buildFail);
            }
            else {
                menu.findItem(R.id.build_menu_next).setIcon(
                        R.drawable.share_variant);
                setTitle(R.string.title_activity_buildDone);
            }
        }
        else {
            menu.findItem(R.id.build_menu_next).setIcon(R.drawable.check);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (isBuildDone) {
                Utils.finishActivity(PlayerActivity.instance);
                Utils.finishActivity(ChooseActivity.instance);
                Utils.finishActivity(MarkPictureActivity.instance);
                if (isFromExtra) {
                    Utils.finishActivity(MainActivity.instance);
                    Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                    startActivity(intent);
                }
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
                Log.e("EL", "获取截图失败：", e);
                CrashReport.postCatchedException(e);
            }
        }

        else {
            try {
                bm = tool.getBitmapFromFile(no, getExternalCacheDir(),extension);
            } catch (Exception e) {
                Log.e("EL", "获取截图失败：", e);
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


    private class BuildThread implements Runnable {
        int delete_nums=0;
        Boolean isRunning = true;
        boolean isFirstAllPic = true;
        @Override
        public void run() {
            //当全部保留全图时，堵塞等待view绘制完成
            while (!drawDone && isCutAllPicture) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            }
            Message msg;
            int len = fileList.length;
            final_bitmap = Bitmap.createBitmap(bWidth,1, getColorConfig());
            for (int i = 0; i < len; i++) {
                Log.i(TAG, "BuildThred run: " + i + " fileStatus:" + fileList[i]);
                msg = Message.obtain();
                msg.obj = "处理第" + i + "张图片";
                msg.what = HandlerStatusBuildPictureNext;
                handler.sendMessage(msg);
                switch (fileList[i]) {
                    case "cut": {
                        try {
                            final_bitmap = addBitmap(final_bitmap, cutBitmap(getBitmap(i + "")));
                        } catch (OutOfMemoryError e) {
                            showDialogOutOfMemory();
                            isRunning = false;
                            stopThread1(t);
                            break;
                        }
                        boolean isShow = settings.getBoolean("isMonitoredShow", false);
                        if (isShow) {
                            msg = Message.obtain();
                            msg.obj = final_bitmap;
                            msg.what = HandlerStatusBuildPictureUpdateBitmap;
                            handler.sendMessage(msg);
                        }
                        break;
                    }
                    case "all": {
                        try {
                            if (isCutAllPicture) {
                                final_bitmap = addBitmap(final_bitmap, cutBottomBitmap(getBitmap(i + "")));
                            }
                            else {
                                if (isFirstAllPic) {
                                    //第一张全图一定是要切掉的
                                    final_bitmap = addBitmap(final_bitmap, cutBottomBitmap(getBitmap(i + "")));
                                    isFirstAllPic = false;
                                }
                                else {
                                    final_bitmap = addBitmap(final_bitmap, getBitmap(i + ""));
                                }
                            }
                        } catch (OutOfMemoryError e) {
                            showDialogOutOfMemory();
                            isRunning = false;
                            stopThread1(t);
                            break;
                        }
                        boolean isShow = settings.getBoolean("isMonitoredShow", false);
                        if (isShow) {
                            msg = Message.obtain();
                            msg.obj = final_bitmap;
                            msg.what = HandlerStatusBuildPictureUpdateBitmap;
                            handler.sendMessage(msg);
                        }
                        break;
                    }
                    case "text": {
                        try {
                            final_bitmap = addBitmap(final_bitmap, getBitmap(i + "_t"));
                        } catch (OutOfMemoryError e) {
                            showDialogOutOfMemory();
                            isRunning = false;
                            stopThread1(t);
                            break;
                        }
                        boolean isShow = settings.getBoolean("isMonitoredShow", false);
                        if (isShow) {
                            msg = Message.obtain();
                            msg.obj = final_bitmap;
                            msg.what = HandlerStatusBuildPictureUpdateBitmap;
                            handler.sendMessage(msg);
                        }
                        break;
                    }
                    default:
                        delete_nums++;
                        break;
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
                SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
                String date    =    sDateFormat.format(new    java.util.Date());
                try {
                    boolean isReduce = settings.getBoolean("isReduce_switch", false);
                    if(saveMyBitmap(final_bitmap,date+"-by_EL", isReduce)) {
                        msg = Message.obtain();
                        msg.obj = date+"-by_EL";
                        msg.what = HandlerStatusBuildPictureDone;
                        handler.sendMessage(msg);

                        msg = Message.obtain();
                        msg.obj = final_bitmap;
                        msg.what = HandlerStatusBuildPictureUpdateBitmap;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    //Toast.makeText(getApplicationContext(),"保存截图失败"+e,Toast.LENGTH_LONG).show();
                    msg = Message.obtain();
                    msg.obj = "保存截图失败"+e;
                    msg.what = HandlerStatusBuildPictureFail;
                    handler.sendMessage(msg);
                }
                catch (OutOfMemoryError e) {
                    showDialogOutOfMemory();
                    stopThread1(t);
                }
            }
        }
    }

    @Nullable
    private Bitmap cutBitmap(Bitmap bm) {
        //return Bitmap.createBitmap(bm, 0, (int)startY, bWidth, (int)(bm.getHeight()-startY));
        if (isAutoBuild) {
            if (SubtitleTop != 0 && SubtitleBottom != 0) {
                return tool.cutBitmap(bm, 0, SubtitleTop, bm.getWidth(), SubtitleBottom-SubtitleTop);
            }
            return tool.cutBitmap(bm, (int)startY, bWidth);
        }
        else {
            int[] cropBox = imageViewPreview.getCropBox();
            if (cropBox == null) {
                return null;
            }
            else {
                Log.i(TAG, "cutBitmap: "+String.format("%d %d %d %d %d %d", cropBox[0], cropBox[1], cropBox[2], cropBox[3], cropBox[4], cropBox[5]));
                if (isFromExtra) {
                    //当图片来自不可确定来源时，长度可能不一致，因此需要重新确定裁剪高度
                    int y = (int)((float)cropBox[1]/cropBox[5]*bm.getHeight());
                    int h = (int)((float)cropBox[3]/cropBox[5]*bm.getHeight());
                    Log.i(TAG, "cutBitmap: "+String.format("y=%d h=%d", y, h));
                    //return tool.cutBitmap(bm, cropBox[0], y, cropBox[2], h);
                    //避免因为内存不足修改图片尺寸后宽度不一致导致闪退
                    return tool.cutBitmap(bm, 0, y, bm.getWidth(), h);
                }
                else {
                    //return tool.cutBitmap(bm, cropBox[0], cropBox[1], cropBox[2], cropBox[3]);
                    //避免因为内存不足修改图片尺寸后宽度不一致导致闪退
                    return tool.cutBitmap(bm, 0, cropBox[1], bm.getWidth(), cropBox[3]);
                }
            }
        }
        //return tool.cutBitmap(bm, (int)startY, bWidth);
    }

    @Nullable
    private Bitmap cutBottomBitmap(Bitmap bm) {
        if (bm == null) {
            Log.e(TAG, "cutBottomBitmap: bitmap is null");
            return null;
        }

        if (isAutoBuild) {
            if (SubtitleTop != 0 && SubtitleBottom != 0) {
                Log.i(TAG, "cutBottomBitmap: subtitleBottom= " + SubtitleBottom);
                return tool.cutBitmap(bm, 0, 0, bm.getWidth(), SubtitleBottom);
            }
            return bm;
        }
        else {
            int[] cropBox = imageViewPreview.getCropBox();
            if (cropBox == null) {
                Log.e(TAG, "cutBottomBitmap: cropBox is null!");
                return null;
            }
            else {
                Log.i(TAG, "cutBottomBitmap: "+String.format("%d %d %d %d %d %d", cropBox[0], cropBox[1], cropBox[2], cropBox[3], cropBox[4], cropBox[5]));
                if (isFromExtra) {
                    //当图片来自不可确定来源时，长度可能不一致，因此需要重新确定裁剪高度
                    int y = (int)((float)cropBox[1]/cropBox[5]*bm.getHeight());
                    int h = (int)((float)cropBox[3]/cropBox[5]*bm.getHeight());
                    Log.i(TAG, "cutBottomBitmap: "+String.format("y=%d h=%d", y, h));
                    return tool.cutBitmap(bm, 0, 0, bm.getWidth(), y+h);
                }
                else {
                    return tool.cutBitmap(bm, 0, 0, bm.getWidth(), cropBox[1]+cropBox[3]);
                }
            }
        }
        //return tool.cutBitmap(bm, (int)startY, bWidth);
    }

    private Bitmap addBitmap(Bitmap first, Bitmap second) {
        Bitmap bitmap = tool.jointBitmap(first, second);
        if (bitmap != null) {
            return bitmap;
        }
         else {
            //避免因为拼接失败直接返回错误，如果拼接失败就只返回第一个bitmap
            Log.e(TAG, "joint bitmap fail, just return first bitmap");
            //Toast.makeText(this, "joint bitmap fail, just return first bitmap", Toast.LENGTH_LONG).show();
            handler.sendEmptyMessage(HandlerStatusBuildPictureAddFail);
            return first;
        }
    }

    private boolean saveMyBitmap(Bitmap bmp, String bitName, boolean isReduce) throws Exception {
            if (isReduce) {
                int quality = Integer.parseInt(settings.getString("reduce_value","100"));
                savePath = tool.saveBitmap2File(bmp,bitName, new File(tool.getSaveRootPath()), true, quality);
            }
            else {
                savePath = tool.saveBitmap2File(bmp,bitName, new File(tool.getSaveRootPath()));
            }
            return true;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<BuildPictureActivity> mActivity;

        private MyHandler(BuildPictureActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final BuildPictureActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerStatusBuildPictureAddFail:
                        Toast.makeText(activity, R.string.buildPicture_toast_addPic_fail, Toast.LENGTH_SHORT).show();
                        break;
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
                            activity.dialog.setProgress(activity.dialog.getProgress()+1);
                            activity.dialog.setMessage(msg.obj.toString());
                            System.gc();
                        }
                        break;

                    case HandlerStatusBuildPictureDone:
                        activity.dialog.dismiss();
                        activity.isBuildDone = true;
                        activity.imageViewPreview.setDrawBox(false);
                        activity.invalidateOptionsMenu();
                        String temp_path = activity.tool.getSaveRootPath()+"/"+msg.obj.toString();
                        temp_path += activity.settings.getBoolean("isReduce_switch", false) ? ".jpg":".png";
                        MediaScannerConnection.scanFile(activity, new String[]{temp_path}, null, null);
                        //Toast.makeText(activity,R.string.buildPicture_toast_buildPicture_done, Toast.LENGTH_LONG).show();
                        break;

                    case HandlerStatusBuildPictureUpdateBitmap:
                        boolean isShow = activity.settings.getBoolean("isMonitoredShow", false);
                        if (isShow) {
                            activity.imageViewPreview.setImageBitmap((Bitmap) msg.obj);
                        }
                        else {
                            DisplayMetrics dm;
                            dm = activity.getResources().getDisplayMetrics();
                            int screenHeight = dm.heightPixels;

                            Bitmap bm = (Bitmap) msg.obj;

                            Log.i(TAG, "buildPictureUpdateBitmap: bitmap.height="+bm.getHeight()+" screenHeight="+screenHeight);
                            if (bm.getHeight() > screenHeight) {
                                if (screenHeight*2 < bm.getHeight()) {
                                    screenHeight = screenHeight * 2;
                                }
                                Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), screenHeight);
                                Log.i(TAG, "buildPictureUpdateBitmap: newbitmap.height="+newbm.getHeight()+" newbitmap.width="+newbm.getWidth());
                                //activity.imageViewPreview.setScaleType(ImageView.ScaleType.FIT_START);
                                activity.imageViewPreview.setImageBitmap(newbm);
                            }
                            else {
                                activity.imageViewPreview.setImageBitmap(bm);
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
                        activity.isBuildDone = true;
                        activity.isBuildFail = true;
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
                                        if (activity.t != null && activity.t.isAlive()) {
                                            activity.t.interrupt();
                                            activity.t = null;
                                        }
                                        activity.t_2.start();
                                        activity.dialog.show();
                                        activity.dialog.setProgress(0);
                                        activity.isShowingOOMDialog = false;
                                    }
                                })
                                .setNegativeButton(R.string.buildPicture_btn_oom_exit, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Utils.finishActivity(PlayerActivity.instance);
                                        Utils.finishActivity(MarkPictureActivity.instance);
                                        Utils.finishActivity(ChooseActivity.instance);
                                        if (activity.isFromExtra) {
                                            Utils.finishActivity(MainActivity.instance);
                                            Intent intent = new Intent(activity, MainActivity.class);
                                            activity.startActivity(intent);
                                        }
                                        activity.finish();
                                    }
                                }).setCancelable(false).show();
                        break;
                }
            }
        }
    }

    private void showDialogOutOfMemory() {
        if (!isShowingOOMDialog) {
            handler.sendEmptyMessage(HandlerStatusOutOfMemory);
            isShowingOOMDialog = true;
        }
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

    @Override
    protected void onStop() {
        super.onStop();
        if(dialog.isShowing()){
            dialog.dismiss();
        }
    }

    private void stopThread1(Thread t) {
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }
}
