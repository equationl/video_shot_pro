package com.equationl.videoshotpro;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.player.SystemPlayerManager;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;


public class PlayerActivity extends AppCompatActivity {
    SharedPreferences settings;
    StandardGSYVideoPlayer videoPlayer;
    OrientationUtils orientationUtils;
    Thread shotThread = new Thread(new ShotThread()), gifThread = new Thread(new ShotGifThread());
    Resources res;
    Tools tool;
    FFmpeg ffmpeg;

    ImageView btn_shot, btn_done;
    TextView text_play_status;

    LinkedBlockingQueue<Long> mark_time = new LinkedBlockingQueue<>();
    String video_path;
    Boolean isShotFinish = false;
    Boolean isDone = false;
    Boolean isShotGif = false, isShotingGif = false;
    Boolean isOnBuildGifPalettePic = false;
    int mark_count=0, shot_count=0;
    int gif_start_time=0, gif_end_time=0;

    private static final String TAG = "EL, in PlayerActivity";
    private final MyHandler handler = new MyHandler(this);
    @SuppressLint("StaticFieldLeak")
    public static PlayerActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    private static final int HandlerShotFail = 10001;
    private static final int HandlerShotSuccess = 10002;
    private static final int HandlerShotDone = 10003;
    private static final int HandlerShotGifFail = 10013;
    private static final int HandlerShotGifSuccess = 10014;
    private static final int HandlerShotGifRunning = 10015;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        instance = this;
        init();
        initVideoPlayer();
    }

    private void initVideoPlayer() {
        PlayerFactory.setPlayManager(SystemPlayerManager.class);
        videoPlayer.setUp(video_path, true, "");
        /*ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.error_picture);
        videoPlayer.setThumbImageView(imageView);  */
        videoPlayer.setLockLand(true);
        videoPlayer.setShowFullAnimation(false);
        videoPlayer.getTitleTextView().setVisibility(View.GONE);
        orientationUtils = new OrientationUtils(this, videoPlayer);
        //设置全屏按键功能,这是使用的是选择屏幕，而不是全屏
        videoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orientationUtils.resolveByClick();
                //videoPlayer.startWindowFullscreen(PlayerActivity.this, false, false);
            }
        });
        videoPlayer.setIsTouchWiget(true);
        //设置返回按键功能
        videoPlayer.getBackButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        videoPlayer.startPlayLogic();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        btn_shot = findViewById(R.id.player_btn_shot);
        btn_done = findViewById(R.id.player_btn_done);
        text_play_status = findViewById(R.id.player_text_status);
        videoPlayer =  findViewById(R.id.video_player);

        tool = new Tools();
        ffmpeg = FFmpeg.getInstance(this);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();

        Uri uri = getIntent().getData();
        checkUri(uri);
        video_path = tool.getImageAbsolutePath(this, uri);
        if (video_path == null) {
            Toast.makeText(this, R.string.player_toast_getVideoPath_fail, Toast.LENGTH_LONG).show();
            finish();
        }

        isShotGif = settings.getBoolean("isShotGif", false);

        text_play_status.setText(String.format(res.getString(R.string.player_text_shotStatus),0, 0));

        tool.cleanExternalCache(this);

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                click_btn_done();
            }
        });

        btn_shot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                click_btn_shot();
            }
        });

        btn_shot.setOnTouchListener(touch_btn_shot);
    }

    @SuppressLint("ClickableViewAccessibility")
    View.OnTouchListener touch_btn_shot = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.i(TAG, "call onTouch()");
            if (isShotingGif) {
                return true;
            }
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "call onTouch() -> up");
                    if (isShotGif) {
                        Log.i(TAG, "call onTouch() -> up -> isShotGif");
                        gif_end_time = videoPlayer.getCurrentPositionWhenPlaying();
                        int shotToGifMinTime = Integer.valueOf(settings.getString("shotToGifMinTime", "3"));
                        if (gif_end_time-gif_start_time > shotToGifMinTime*1000) {
                            Log.i(TAG, "call onTouch() -> up -> isShotGif -> couldShot");
                            isShotingGif = true;
                            if (!gifThread.isAlive()) {
                                gifThread = new Thread(new ShotGifThread());
                                gifThread.start();
                            }
                            //return true;
                        }
                    }
                    return false;
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "call onTouch() -> down");
                    if (isShotGif) {
                        Log.i(TAG, "call onTouch() -> down -> isShotGif");
                        gif_start_time = videoPlayer.getCurrentPositionWhenPlaying();
                    }
                    return false;
            }
            return false;
        }
    };

    // FIXME 文档中没找到检查URI的方法，这样曲线检查吧
    private void checkUri(Uri uri) {
        MediaMetadataRetriever rev = new MediaMetadataRetriever();
        try {
            rev.setDataSource(this, uri);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, R.string.player_toast_loadUriFail_invalid, Toast.LENGTH_LONG).show();
            finish();
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.player_toast_loadUriFail_lackPermission, Toast.LENGTH_LONG).show();
            finish();
        }   catch (Exception e) {
            Toast.makeText(this, R.string.player_toast_loadUriFail_other, Toast.LENGTH_LONG).show();
            finish();
        }
        rev.release();
    }

    private void click_btn_shot() {
        if (isShotingGif) {
            return;
        }
        mark_count++;
        text_play_status.setText(String.format(res.getString(R.string.player_text_shotStatus),mark_count,shot_count));
        mark_time.offer((long)videoPlayer.getCurrentPositionWhenPlaying());
        if (!shotThread.isAlive()) {
            shotThread = new Thread(new ShotThread());
            shotThread.start();
        }
    }

    private void click_btn_done() {
        if (mark_count < 1) {
            Toast.makeText(getApplicationContext(),R.string.player_toast_needMoreShot, Toast.LENGTH_LONG).show();
        }
        else {
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (!shotThread.isAlive()) {
                shotThread = new Thread(new ShotThread());
                shotThread.start();
            }
            isDone = true;
            btn_shot.setClickable(false);
            btn_done.setClickable(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPlayer.onVideoPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isDone = false;
        btn_shot.setClickable(true);
        btn_done.setClickable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoPlayer.onVideoResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

    @Override
    public void onBackPressed() {
        if (orientationUtils.getScreenType() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            videoPlayer.getFullscreenButton().performClick();
            return;
        }
        videoPlayer.setVideoAllCallBack(null);
        super.onBackPressed();
    }

    private class ShotThread implements Runnable {
        @Override
        public void run() {
            Long time;
            while ((time = mark_time.peek()) != null) {
                if (!ffmpeg.isFFmpegCommandRunning()) {
                    isShotFinish = false;
                    String outPathName;
                    File externalCacheDir = getExternalCacheDir();
                    if (externalCacheDir == null) {
                        Message msg = Message.obtain();
                        msg.obj = res.getString(R.string.player_text_savePicture_fail)+res.getString(R.string.player_text_getCachePath_fail);
                        msg.what = HandlerShotFail;
                        handler.sendMessage(msg);
                        break;
                    }
                    if (settings.getBoolean("isShotToJpg",true)) {
                        outPathName = externalCacheDir.toString()+"/"+shot_count+".jpg";
                    }
                    else {
                        outPathName = externalCacheDir.toString()+"/"+shot_count+".png";
                    }

                    String cmd[] = {"-ss", ""+(time/1000.0), "-t", "0.001", "-i", video_path, "-update", "1", "-y", "-f", "image2", outPathName};
                    try {
                        Log.i(TAG, "cmd="+Arrays.toString(cmd));
                        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                            @Override
                            public void onFailure(String message) {
                                Log.i("el_test: onFailure", message);
                                Message msg = Message.obtain();
                                msg.obj = R.string.player_text_savePicture_fail+message;
                                msg.what = HandlerShotFail;
                                handler.sendMessage(msg);
                            }
                            @Override
                            public void onSuccess(String message) {
                                shot_count++;
                                mark_time.poll();
                                Log.i("TAG", "onSuccess:");
                                Log.i(TAG, message);
                                Message msg = Message.obtain();
                                msg.obj = String.format(res.getString(R.string.player_text_shotStatus),mark_count, shot_count);
                                msg.what = HandlerShotSuccess;
                                handler.sendMessage(msg);
                            }
                            @Override
                            public void onFinish() {
                                isShotFinish = true;
                            }
                        });
                    } catch (FFmpegCommandAlreadyRunningException e) {
                        Message msg = Message.obtain();
                        msg.obj = res.getString(R.string.player_text_savePicture_fail)+e;
                        msg.what = HandlerShotFail;
                        handler.sendMessage(msg);
                    }
                    //阻塞等待截取结果
                    while (!isShotFinish) {
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                }
                else {
                    Log.i(TAG, "ffmpeg is running");
                }
            }
            if (isDone) {
                Message msg = Message.obtain();
                msg.obj = "";
                msg.what = HandlerShotDone;
                handler.sendMessage(msg);
            }
        }
    }

    private class ShotGifThread implements Runnable {
        @Override
        public void run(){
            String palettePicPath = new File(getExternalCacheDir(), "PalettePic.png").getAbsolutePath();
            SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
            String date    =    sDateFormat.format(new    java.util.Date());
            date += "-by_EL.gif";
            String save_path =  tool.getSaveRootPath() + "/" + date;
            String gif_RP = settings.getString("gifRP_value", "-1");
            gif_RP = tool.getVideo2GifRP(video_path, gif_RP);
            gif_RP = gif_RP.equals("-1")?"-1:-1":gif_RP.replace("x", ":");
            String[] cmd = {"-ss", String.valueOf(gif_start_time/1000.0), "-t",
                    String.valueOf((gif_end_time-gif_start_time)/1000.0),"-i",
                    video_path, "-vf",
                    "scale="+gif_RP+":flags=lanczos,palettegen", "-y",
                    palettePicPath};
            isOnBuildGifPalettePic = true;
            executeBuildGif(cmd, save_path);
        }
    }

    private String[] getShotGifCmd(String save_path) {
        String gif_RP = settings.getString("gifRP_value", "-1");
        gif_RP = tool.getVideo2GifRP(video_path, gif_RP);
        String gif_frameRate = settings.getString("gifFrameRate_value", "14");

        boolean isVideoPathHaveSpace = false;
        if (video_path.contains(" ")) {  //避免因为视频路径中包含空格而导致按照空格分割命令时出错
            video_path = video_path.replaceAll(" ", "_");
            isVideoPathHaveSpace = true;
        }
        String palettePicPath = new File(getExternalCacheDir(), "PalettePic.png").getAbsolutePath();
        String cmd = String.format(Locale.CHINA,
                "-ss %f -t %f -i %s -i %s -r %s -b %s -lavfi scale=%s:flags=lanczos[x];[x][1:v]paletteuse -y %s",
                (gif_start_time/1000.0),
                ((gif_end_time-gif_start_time)/1000.0),
                video_path,
                palettePicPath,
                gif_frameRate.equals("-1")? "24":gif_frameRate,
                "100k",
                gif_RP.equals("-1")?"-1:-1":gif_RP.replace("x", ":"),
                save_path);
        String gif_cmd[] = cmd.split(" ");
        if (isVideoPathHaveSpace) {   //FIXME 现在的索引确定是5，小心以后变化啊
            gif_cmd[5] = gif_cmd[5].replaceAll("_", " ");
        }
        Log.i(TAG, "cmd = "+Arrays.toString(gif_cmd));
        return gif_cmd;
    }

    private void executeBuildGif(String[] gif_cmd, final String save_path) {
        while (ffmpeg.isFFmpegCommandRunning()) {
            //阻塞等待执行结束
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        try {
            ffmpeg.execute(gif_cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    handler.sendEmptyMessage(HandlerShotGifRunning);
                }

                @Override
                public void onProgress(String message) {}

                @Override
                public void onFailure(String message) {
                    Log.e(TAG, "截取GIF失败："+message);
                    handler.sendEmptyMessage(HandlerShotGifFail);
                }

                @Override
                public void onSuccess(String message) {
                    if (isOnBuildGifPalettePic) {
                        isOnBuildGifPalettePic = false;
                        String[] cmd = getShotGifCmd(save_path);
                        executeBuildGif(cmd, save_path);
                    }
                    else {
                        Message msg = Message.obtain();
                        msg.what = HandlerShotGifSuccess;
                        msg.obj = save_path;
                        handler.sendMessage(msg);
                    }
                }

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            handler.sendEmptyMessage(HandlerShotGifFail);
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<PlayerActivity> mActivity;

        private MyHandler(PlayerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final PlayerActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerShotSuccess:
                        activity.text_play_status.setText(msg.obj.toString());
                        break;
                    case HandlerShotFail:
                        Toast.makeText(activity, (String)msg.obj, Toast.LENGTH_LONG).show();
                        break;
                    case HandlerShotDone:
                        if (activity.settings.getBoolean("isSortPicture", true)) {
                            Intent intent = new Intent(activity, ChooseActivity.class);
                            activity.startActivity(intent);
                        }
                        else {
                            Intent intent = new Intent(activity, MarkPictureActivity.class);
                            activity.startActivity(intent);
                        }
                        break;
                    case HandlerShotGifSuccess:
                        activity.btn_shot.clearAnimation();
                        activity.isShotingGif = false;
                        MediaScannerConnection.scanFile(activity, new String[]{msg.obj.toString()}, null, null);
                        Toast.makeText(activity, R.string.player_toast_shotGif_success, Toast.LENGTH_SHORT).show();
                        File palettePicFile = new File(activity.getExternalCacheDir(), "PalettePic.png");
                        if (!palettePicFile.delete()) {
                            Log.e(TAG, "delete palettePicFile fail");
                        }
                        break;
                    case HandlerShotGifFail:
                        Toast.makeText(activity, R.string.player_toast_shotGif_fail, Toast.LENGTH_SHORT).show();
                        break;
                    case HandlerShotGifRunning:
                        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.rotate);
                        activity.btn_shot.startAnimation(animation);
                        Toast.makeText(activity, R.string.player_toast_shotGif_start, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
}
