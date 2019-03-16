package com.equationl.videoshotpro;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.ScreenRecorder;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by branch on 2016-5-25.
 * Edited by equationl
 *
 * 启动悬浮窗界面
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FloatWindowsService extends Service {
    MediaProjection mMediaProjection;
    VirtualDisplay mVirtualDisplay;
    static Intent mResultData = null;
    ImageReader mImageReader;
    WindowManager mWindowManager;
    WindowManager.LayoutParams mLayoutParams;
    ScreenRecorder mRecorder;
    FFmpeg ffmpeg;
    Thread video2gifThread = new Thread(new Video2GifThread());
    ImageView mFloatView;
    int mScreenWidth;
    int mScreenHeight;
    int mScreenDensity;
    SharedPreferences settings;
    Tools tool;
    Resources res;
    int shot_num = 0;
    boolean isOnScreenRecorder = false;
    boolean isOnBuildGif = false;
    boolean isOnBuildGifPalettePic = false;

    private final MyHandler handler = new MyHandler(this);
    private static final String TAG = "el,In FWService";
    private static final int HandlerScreenShotFinish = 10000;
    private static final int HandlerVideo2GifStart = 10001;
    private static final int HandlerVideo2GifFail = 10002;
    private static final int HandlerVideo2GifSuccess = 10003;
    private static final int HandlerVideo2GifFinish = 10004;

    @Override
    public void onCreate() {
        super.onCreate();

        res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        tool = new Tools();
        ffmpeg = FFmpeg.getInstance(this);

        tool.cleanExternalCache(this);

        checkPermission();
        createFloatView();
        //createImageReader();
        initNotification();
    }

    private void initNotification(){
        NotificationManager barmanager=(NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notice;
        Notification.Builder builder = new Notification.Builder(this).setTicker(res.getString(R.string.floatWindowsService_notice_ticker_text))
                .setSmallIcon(R.mipmap.ic_launcher).setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            builder.setChannelId("float_done");
        }
        Intent appIntent;
        if (settings.getBoolean("isSortPicture", true)) {
            appIntent = new Intent(this,ChooseActivity.class);
        }
        else {
            appIntent = new Intent(this,MarkPictureActivity.class);
        }
        appIntent.setAction(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//关键的一步，设置启动模式
        appIntent.putExtra("isFromExtra", true);
        PendingIntent contentIntent =PendingIntent.getActivity(this, 0,appIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        notice = builder.setContentIntent(contentIntent).setContentTitle(res.getString(R.string.floatWindowsService_notice_title))
                .setContentText(res.getString(R.string.floatWindowsService_notice_content)).build();
        notice.flags=Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
        if (barmanager != null) {
            barmanager.notify(10, notice);
        }
    }

    public static void setResultData(Intent mResultData) {
        FloatWindowsService.mResultData = mResultData;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("EL", "in onBind)");
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createFloatView() {
        mLayoutParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else {
            mLayoutParams.type =  WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mLayoutParams.format = PixelFormat.RGBA_8888;
        // 设置Window flag
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.START | Gravity.TOP;
        mLayoutParams.x = mScreenWidth;
        mLayoutParams.y = 100;
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;


        mFloatView = new ImageView(getApplicationContext());
        mFloatView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.float_button));
        mWindowManager.addView(mFloatView, mLayoutParams);

        mFloatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "click mFloatView");
                if (!isOnBuildGif) {
                    startScreenShot();
                }
            }
        });

        mFloatView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.i(TAG, "long click mFloatView");
                if (!isOnBuildGif &&
                        settings.getBoolean("isShotGif", false)) {
                    mFloatView.setVisibility(View.INVISIBLE);
                    startScreenRecorder();
                }
                return false;
            }
        });

        mFloatView.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //return mGestureDetector.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "mFloatView -> down");
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = mLayoutParams.x;
                        paramY = mLayoutParams.y;
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG, "mFloatView -> up");
                        if (isOnScreenRecorder) {
                            if (mRecorder != null) {
                                mRecorder.quit();
                                mRecorder = null;
                                mFloatView.setVisibility(View.VISIBLE);
                                tearDownMediaProjection();
                                video2Gif();
                            }
                            isOnScreenRecorder = false;
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        mLayoutParams.x = paramX + dx;
                        mLayoutParams.y = paramY + dy;
                        mWindowManager.updateViewLayout(mFloatView, mLayoutParams);
                        break;
                }
                return false;
            }
        });
    }

    private void video2Gif() {
        Toast.makeText(FloatWindowsService.this, R.string.floatWindowsService_toast_saveVideo2Gif_start, Toast.LENGTH_SHORT).show();
        if (!video2gifThread.isAlive()) {
            video2gifThread= new Thread(new Video2GifThread());
            video2gifThread.start();
        }
    }

    private void startScreenRecorder() {
        Log.i(TAG, "call startScreenRecorder()");
        isOnScreenRecorder = true;

        setUpMediaProjection();

        if (mMediaProjection == null) {
            Log.e(TAG, "mMediaProjection == null");
            Toast.makeText(this, R.string.floatWindowsService_toast_setMediaProjection_fail, Toast.LENGTH_SHORT).show();
            return;
        }

        String video_RP = settings.getString("gifRP_value", "-1");
        video_RP = tool.getRP(mScreenWidth, mScreenHeight, video_RP);
        String video_frameRate = settings.getString("gifFrameRate_value", "30");
        video_frameRate = video_frameRate.equals("-1")? "30" : video_frameRate;
        int width, height;
        if (video_RP.equals("-1")) {
            width = mScreenWidth;
            height = mScreenHeight;
        }
        else {
            String[] videoSize = video_RP.split("x");
            width = Integer.valueOf(videoSize[0]);
            height = Integer.valueOf(videoSize[1]);
        }

        Configuration mConfiguration = res.getConfiguration();
        int ori = mConfiguration.orientation ;
        if(ori == Configuration.ORIENTATION_LANDSCAPE){ //横屏
            int temp = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = temp;
        }

        Log.i(TAG, "width: "+width+" height: "+height);
        File file = new File(getExternalCacheDir(), "temp.mp4");
        if (file.exists()) {
            if (!file.delete()) {
                Log.e(TAG, "delete cache file fail");
                Toast.makeText(this, R.string.floatWindowsService_toast_deleteCacheFile_fail, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Log.i(TAG, "video cache path: "+file.getAbsolutePath());
        //Log.i(TAG, "video_frameRate= "+video_frameRate);
        mRecorder = new ScreenRecorder(width, height, Integer.valueOf(video_frameRate), 1, mMediaProjection, file.getAbsolutePath());
        mRecorder.start();
    }

    private void startScreenShot() {
        mFloatView.setVisibility(View.GONE);

        createImageReader();

        Handler handler1 = new Handler();
        handler1.post(new Runnable() {
            public void run() {
                //start virtual
                startVirtual();
            }
        });

        handler1.post(new Runnable() {
            public void run() {
                //capture the screen
                startCapture();
            }
        });
    }


    private void createImageReader() {
        mImageReader = null;
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
    }

    public void startVirtual() {
        if (mMediaProjection != null) {
            virtualDisplay();
        } else {
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    public void setUpMediaProjection() {
        if (mResultData == null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        } else {
            mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData);
        }
    }

    private MediaProjectionManager getMediaProjectionManager() {

        return (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void virtualDisplay() {
        try {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        } catch (NullPointerException e) {  //见 https://bbs.csdn.net/topics/391879153
            CrashReport.postCatchedException(e);
            setUpMediaProjection();   //FIXME 未测试
            virtualDisplay();
        }
    }

    private void startCapture() {
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            Image image = null;
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Log.i(TAG, "call onImageAvailable");
                image = imageReader.acquireLatestImage();
                ImageToBitmap(image);
                mImageReader.close();
                stopVirtual();
                tearDownMediaProjection();
                image.close();
                handler.sendEmptyMessage(HandlerScreenShotFinish);
            }
        }, getBackgroundHandler());
    }

    Handler backgroundHandler;

    private Handler getBackgroundHandler() {
        if (backgroundHandler == null) {
            HandlerThread backgroundThread =
                    new HandlerThread("FloatWindowsService", android.os.Process
                            .THREAD_PRIORITY_BACKGROUND);
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
        return backgroundHandler;
    }

    private void ImageToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        image.close();
        if (bitmap != null) {
            saveBitmapToFile(bitmap);
        }
        else {
            Toast.makeText(this, R.string.floatWindowsService_toast_getBitmap_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmapToFile(Bitmap bitmap) {
        File savePath = getExternalCacheDir();
        String fileName = ""+shot_num;
        Boolean isReduce = settings.getBoolean("isShotToJpg", true);
        int quality = settings.getBoolean("isReduce_switch", false) ?
                Integer.parseInt(settings.getString("reduce_value","100")): 100;
        try {
            tool.saveBitmap2File(bitmap, fileName, savePath, isReduce, quality);
            shot_num++;
        } catch (Exception e){
            Toast.makeText(this, R.string.floatWindowsService_toast_saveBitmap_fail, Toast.LENGTH_SHORT).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    @Override
    public void onDestroy() {
        // to remove mFloatLayout from windowManager
        super.onDestroy();
        if (mFloatView != null) {
            mWindowManager.removeView(mFloatView);
        }
        if(mRecorder != null){
            mRecorder.quit();
            mRecorder = null;
        }
        stopVirtual();
        tearDownMediaProjection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)  {
        try {
            MainActivity.instance.finish();
            BuildPictureActivity.instance.finish();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        new Thread () {
            public void run () {
                try{
                    Thread.sleep(1000);
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
                try {
                    Instrumentation inst= new Instrumentation();
                    inst.sendKeyDownUpSync(KeyEvent. KEYCODE_BACK);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();    //FIXME 临时解决方案，不靠谱
        //以上代码用来避免使用外部程序截图时确定后部分Acticity未正常关闭的问题
        //2018.11.10注

        return super.onStartCommand(intent, flags, startId);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT  >= 23 ) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.floatWindowsService_toast_floatPermissionDenied, Toast.LENGTH_LONG).show();
                stopSelf();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String id = "float_done";
        CharSequence name = getString(R.string.float_done_channel_name);
        String description = getString(R.string.float_done_channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<FloatWindowsService> mActivity;

        private MyHandler(FloatWindowsService activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final FloatWindowsService activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerScreenShotFinish:
                        activity.mFloatView.setVisibility(View.VISIBLE);
                        break;
                    case HandlerVideo2GifFail:
                        Log.e(TAG, msg.obj.toString());
                        Toast.makeText(activity, R.string.floatWindowsService_toast_saveVideo2Gif_fail, Toast.LENGTH_SHORT).show();
                        //activity.mFloatView.setClickable(true);
                        break;
                    case HandlerVideo2GifStart:
                        //activity.mFloatView.setClickable(false);
                        activity.isOnBuildGif = true;
                        break;
                    case HandlerVideo2GifSuccess:
                        if (!activity.isOnBuildGifPalettePic) {
                            MediaScannerConnection.scanFile(activity, new String[]{msg.obj.toString()}, null, null);
                            Toast.makeText(activity, R.string.floatWindowsService_toast_saveVideo2Gif_success, Toast.LENGTH_SHORT).show();
                        }
                        //activity.mFloatView.setClickable(true);
                        break;
                    case HandlerVideo2GifFinish:
                        File f = (File) msg.obj;
                        if (!f.delete()) {
                            Log.i(TAG, "delete cache file fail");
                        }
                        File palettePicFile = new File(activity.getExternalCacheDir(), "PalettePic.png");
                        if (!palettePicFile.delete()) {
                            Log.e(TAG, "delete palettePicFile fail");
                        }
                        activity.isOnBuildGif = false;
                        break;
                }
            }
        }
    }

    private class Video2GifThread implements Runnable {
        @Override
        public void run(){
            SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
            String date    =    sDateFormat.format(new    java.util.Date());
            date += "-by_EL.gif";
            String palettePicPath = new File(getExternalCacheDir(), "PalettePic.png").getAbsolutePath();
            final String save_path =  tool.getSaveRootPath() + "/" + date;
            final File video_path = new File(getExternalCacheDir(), "temp.mp4");
            String cmd[];
            if (!isOnBuildGifPalettePic) {
                isOnBuildGifPalettePic = true;
                cmd = new String[]{"-i", video_path.getAbsolutePath(), "-vf",
                        "scale=-1:-1:flags=lanczos,palettegen", "-y",
                        palettePicPath};
                executeShotGif(cmd, save_path, video_path);
            }
        }
    }

    private void executeShotGif(String[] cmd, final String save_path, final File video_path) {
        while (ffmpeg.isFFmpegCommandRunning()) {
            //阻塞等待执行结束
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    handler.sendEmptyMessage(HandlerVideo2GifStart);
                }

                @Override
                public void onProgress(String message) {}

                @Override
                public void onFailure(String message) {
                    Message msg = Message.obtain();
                    msg.obj = message;
                    msg.what = HandlerVideo2GifFail;
                    handler.sendMessage(msg);
                }

                @Override
                public void onSuccess(String message) {
                    if (isOnBuildGifPalettePic) {
                        isOnBuildGifPalettePic = false;
                        String palettePicPath = new File(getExternalCacheDir(), "PalettePic.png").getAbsolutePath();
                        String gif_frameRate = settings.getString("gifFrameRate_value", "14");
                        gif_frameRate = gif_frameRate.equals("-1") ? "24":gif_frameRate;
                        String[] cmd = new String[]{"-i", video_path.getAbsolutePath(), "-i",
                                palettePicPath,"-r", gif_frameRate,
                                "-lavfi", "scale=-1:-1:flags=lanczos[x];[x][1:v]paletteuse",
                                "-y",  save_path};
                        executeShotGif(cmd, save_path, video_path);
                    }
                    else {
                        Message msg = Message.obtain();
                        msg.obj = save_path;
                        msg.what = HandlerVideo2GifSuccess;
                        handler.sendMessage(msg);
                    }
                }

                @Override
                public void onFinish() {
                    if (isOnBuildGifPalettePic) {
                        Message msg = Message.obtain();
                        msg.obj = video_path;
                        msg.what = HandlerVideo2GifFinish;
                        handler.sendMessage(msg);
                    }
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
