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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
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
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by branch on 2016-5-25.
 * Edited by equationl
 * <p>
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
    GestureDetector mGestureDetector;
    ImageView mFloatView;
    int mScreenWidth;
    int mScreenHeight;
    int mScreenDensity;
    SharedPreferences settings;
    Tools tool;
    Resources res;
    int shot_num = 0;

    private final MyHandler handler = new MyHandler(this);
    private static final String TAG = "el,In FWService";
    private static final int HandlerScreenShotFinish = 10000;

    @Override
    public void onCreate() {
        super.onCreate();

        res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        tool = new Tools();

        tool.cleanExternalCache(this);

        checkPermission();
        createFloatView();
        createImageReader();
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
            appIntent = new Intent(this,MarkPictureActivity2.class);
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
        mGestureDetector = new GestureDetector(getApplicationContext(), new FloatGestrueTouchListener());
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

        mFloatView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }


    private class FloatGestrueTouchListener implements GestureDetector.OnGestureListener {
        int lastX, lastY;
        int paramX, paramY;

        @Override
        public boolean onDown(MotionEvent event) {
            lastX = (int) event.getRawX();
            lastY = (int) event.getRawY();
            paramX = mLayoutParams.x;
            paramY = mLayoutParams.y;
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            startScreenShot();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int dx = (int) e2.getRawX() - lastX;
            int dy = (int) e2.getRawY() - lastY;
            mLayoutParams.x = paramX + dx;
            mLayoutParams.y = paramY + dy;
            // 更新悬浮窗位置
            mWindowManager.updateViewLayout(mFloatView, mLayoutParams);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
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
                }
            }
        }
    }
}
