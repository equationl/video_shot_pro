package com.equationl.videoshotpro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.Utils;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class PlayerActivity extends AppCompatActivity {
    VideoView videoview;
    View videoview_contianer;
    Button btn_status,btn_done,btn_shot;
    TextView text_count,video_time, play_controlBar_current_time, play_controlBar_totally_time;
    Uri uri;
    LinkedBlockingQueue<Long> mark_time = new LinkedBlockingQueue<Long>();
    int pic_num=0, isFirstPlay=1, shot_num=0;
    GestureDetector mGestureDetector;
    Thread thread = new Thread(new MyThread()), gif_thread = new Thread(new ThreadShotGif());
    Boolean isDone=false;
    SharedPreferences settings;
    Boolean isHideBtn = false;
    Boolean isORIENTATION_LANDSCAPE = false;
    Boolean isShotGif = false, isShotingGif = false;
    Tools tool = new Tools();
    RelativeLayout.LayoutParams params;
    FFmpeg ffmpeg;
    String path, duration_text;
    Boolean isShowingTime = false;
    Resources res;
    int gif_start_time=0, gif_end_time=0;
    boolean isShotFinish=false;
    int shotToGifMinTime;
    LinearLayout player_controlBar_layout;
    SeekBar play_seekbar;
    ImageView play_controlBar_play_pause_btn;
    Utils utils = new Utils();

    public static PlayerActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了


    private static final int HandlerStatusHideTime = 10010;
    private static final int HandlerStatusShowTime = 10011;
    private static final int HandlerStatusUpdateTime = 10012;
    private static final int HandlerShotGifFail = 10013;
    private static final int HandlerShotGifSuccess = 10014;
    private static final int HandlerShotGifRunning = 10015;
    private static final int HandlerUpdateControlBarCurrentTime = 10016;



    private static final String TAG = "el,In PlayerActivity";

    private final MyHandler handler = new MyHandler(this);

    @SuppressLint("ClickableViewAccessibility")   //忽略同时使用 OnClickListener 和 OnTouchListener 产生的冲突警告
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
        setContentView(R.layout.activity_player);


        instance = this;

        videoview = (VideoView) findViewById(R.id.videoView);
        btn_status = (Button) findViewById(R.id.data_button_left);
        btn_done   = (Button) findViewById(R.id.button_done);
        btn_shot   = (Button) findViewById(R.id.data_button_bottom);
        text_count = (TextView) findViewById(R.id.text_count);
        videoview_contianer = findViewById(R.id.main_videoview_contianer);
        video_time = (TextView) findViewById(R.id.video_time);
        player_controlBar_layout = (LinearLayout) findViewById(R.id.player_controlBar_layout);
        play_controlBar_current_time = (TextView) findViewById(R.id.play_controlBar_current_time) ;
        play_controlBar_totally_time = (TextView) findViewById(R.id.play_controlBar_totally_time) ;
        play_seekbar = (SeekBar) findViewById(R.id.player_controlBar_seekBar);
        play_controlBar_play_pause_btn = (ImageView) findViewById(R.id.play_controlBar_play_pause_btn);

        params = (RelativeLayout.LayoutParams) btn_shot.getLayoutParams();
        ffmpeg = FFmpeg.getInstance(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        isShotGif = settings.getBoolean("isShotGif", false);

        res = getResources();
        text_count.setText(String.format(res.getString(R.string.player_text_shotStatus),0, 0));

        tool.cleanExternalCache(this);    //清除上次产生的缓存图片

        uri = getIntent().getData();
        path = tool.getImageAbsolutePath(this, uri);

        //videoview.setMediaController(new MediaController(this));
        videoview.setVideoURI(uri);

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
        String meta_duration = rev.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = 0;
        try {
            duration = Long.parseLong(meta_duration);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.player_toast_getMetaDuration_fail, Toast.LENGTH_LONG).show();
            finish();
        }
        Bitmap bitmap = rev.getFrameAtTime(((duration/2)*1000),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        videoview.setBackground(new BitmapDrawable(bitmap));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date(duration);
        duration_text = simpleDateFormat.format(date);

        mGestureDetector = new GestureDetector(this, mGestureListener);
        videoview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });


        btn_done   .setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pic_num < 1) {
                    Toast.makeText(getApplicationContext(),R.string.player_toast_needMoreShot, Toast.LENGTH_LONG).show();
                }
                else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    if (!thread.isAlive()) {
                        thread = new Thread(new MyThread());
                        thread.start();
                    }
                    isDone = true;
                    btn_shot.setClickable(false);
                    btn_done.setClickable(false);
                }
            }
        });
        btn_status .setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isFirstPlay==1) {
                    videoview.setBackgroundResource(0);
                    videoview.start();
                    handler.sendEmptyMessage(HandlerUpdateControlBarCurrentTime);
                    btn_status.setText(R.string.player_text_rotationScreen);
                    isFirstPlay = 0;
                    play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_pause);
                }
                else {
                    if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        player_controlBar_layout.setVisibility(View.INVISIBLE);
                        isORIENTATION_LANDSCAPE = false;
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        player_controlBar_layout.setVisibility(View.VISIBLE);
                        isORIENTATION_LANDSCAPE = true;
                    }
                }
            }
        });
        btn_shot   .setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btn_shot.setBackground(res.getDrawable(R.drawable.button_radius));
                text_count.setText(String.format(res.getString(R.string.player_text_shotStatus),pic_num+1,shot_num));
                mark_time.offer((long)videoview.getCurrentPosition());
                pic_num++;
                if (!thread.isAlive()) {
                    thread = new Thread(new MyThread());
                    thread.start();
                }
            }
        });

        btn_shot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.i(TAG, "on btn_shot.onTouch");
                if (isShotingGif) {
                    Log.i(TAG, "on btn_shot.onTouch, and is shotting gif");
                    return false;
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                    Log.d(TAG, "shot button ---> up");
                    return false;
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d(TAG, "shot button ---> down");
                    if (isShotGif) {
                        btn_shot.setBackground(res.getDrawable(R.drawable.button_radius));
                        gif_start_time = videoview.getCurrentPosition();
                    }
                    return false;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    Log.d(TAG, "shot button ---> cacel");
                    if (isShotGif) {
                        gif_end_time = videoview.getCurrentPosition();
                        shotToGifMinTime = Integer.valueOf(settings.getString("shotToGifMinTime", "3"));
                        Log.i(TAG, "shotToGifMinTime="+shotToGifMinTime);
                        if (gif_end_time-gif_start_time > shotToGifMinTime*1000) {
                            btn_shot.setBackground(res.getDrawable(R.drawable.button_radius_up));
                            isShotingGif = true;
                            if (!gif_thread.isAlive()) {
                                gif_thread = new Thread(new ThreadShotGif());
                                gif_thread.start();
                            }
                            return true;
                        }
                    }
                    return false;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.d(TAG, "shot button ---> move");
                    return true;
                }
                return false;
            }
        });

        videoview.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if(what== MediaPlayer.MEDIA_ERROR_SERVER_DIED){
                    Toast.makeText(getApplicationContext(),"Media Error,Server Died"+extra, Toast.LENGTH_LONG).show();
                }else if(what== MediaPlayer.MEDIA_ERROR_UNKNOWN){
                    Toast.makeText(getApplicationContext(),"Media Error,Error Unknown "+extra, Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btn_status.setText(R.string.player_text_replay);
                isFirstPlay = 1;
            }
        });

        play_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimeFormat(play_controlBar_current_time, progress);
                if (videoview.getDuration() == progress) {
                    play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_play);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeMessages(HandlerUpdateControlBarCurrentTime);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int totall = seekBar.getProgress();
                videoview.seekTo(totall);
                handler.sendEmptyMessage(HandlerUpdateControlBarCurrentTime);
            }
        });

        play_controlBar_play_pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoview.isPlaying()) {
                    play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_play);
                    videoview.pause();
                    handler.removeMessages(HandlerUpdateControlBarCurrentTime);
                } else {
                    play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_pause);
                    videoview.start();
                    handler.sendEmptyMessage(HandlerUpdateControlBarCurrentTime);
                }
            }
        });

    }



    @Override
    protected void onRestart() {
        Log.i("el_test", "onRestart");
        super.onRestart();
        isDone = false;
        btn_shot.setClickable(true);
        btn_done.setClickable(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (videoview == null) {
            return;
        }
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){//横屏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().invalidate();
            float width = getWidthInPx(this);
            float height = getHeightInPx(this);
            videoview_contianer.getLayoutParams().height = (int) height;
            videoview_contianer.getLayoutParams().width = (int) width;
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            btn_shot.setLayoutParams(params);
            if (videoview.isPlaying()) {
                play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_pause);
            }
            Log.i("TEST","width="+width+" height="+height);
        } else {
            final WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            float width = getWidthInPx(this);
            float height = getHeightInPx(this);
            videoview_contianer.getLayoutParams().height = (int) height;
            videoview_contianer.getLayoutParams().width = (int) width;
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            btn_shot.setLayoutParams(params);
        }
    }

    public static float getHeightInPx(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
    public static float getWidthInPx(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }


    private class MyThread implements Runnable {
        @Override
        public void run() {
            Long time;
            while ((time = mark_time.peek()) != null) {
                if (!ffmpeg.isFFmpegCommandRunning()) {
                    isShotFinish = false;
                    Log.i("el_test", "time="+time);
                    Log.i("el_test", "shot_num="+shot_num);
                    String outPathName;
                    File externalCacheDir = getExternalCacheDir();
                    if (externalCacheDir == null) {
                        Message msg = Message.obtain();
                        msg.obj = res.getString(R.string.player_text_savePicture_fail)+res.getString(R.string.player_text_getCachePath_fail);
                        msg.what = 2;
                        handler.sendMessage(msg);
                        break;
                    }
                    if (settings.getBoolean("isShotToJpg",true)) {
                        outPathName = externalCacheDir.toString()+"/"+shot_num+".jpg";
                    }
                    else {
                        outPathName = externalCacheDir.toString()+"/"+shot_num+".png";
                    }

                    String cmd[] = {"-ss", ""+(time/1000.0), "-t", "0.001", "-i", path, "-update", "1", "-y", "-f", "image2", outPathName};
                    try {
                        Log.i(TAG, "cmd="+Arrays.toString(cmd));

                        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                           @Override
                            public void onFailure(String message) {
                                Log.i("el_test: onFailure", message);
                                Message msg = Message.obtain();
                                msg.obj = R.string.player_text_savePicture_fail+message;
                                msg.what = 2;
                                handler.sendMessage(msg);
                            }
                            @Override
                            public void onSuccess(String message) {
                                shot_num++;
                                mark_time.poll();
                                Log.i("TAG", "onSuccess:");
                                Log.i(TAG, message);
                                Message msg = Message.obtain();
                                msg.obj = String.format(res.getString(R.string.player_text_shotStatus),pic_num,shot_num);
                                msg.what = 1;
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
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                    //阻塞等待截取结果
                    while (!isShotFinish) {
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException e) {}
                    }
                }
                else {
                    Log.i("TAG", "ffmpeg is running");
                }
            }
            if (isDone) {
                Message msg = Message.obtain();
                msg.obj = "";
                msg.what = 3;
                handler.sendMessage(msg);
            }
        }
    }

    private class ThreadShotGif implements Runnable {
        @Override
        public void run() {
            String video_path = tool.getImageAbsolutePath(PlayerActivity.this,uri);
            String gif_RP = settings.getString("gifRP_value", "-1");
            gif_RP = tool.getVideo2GifRP(video_path, gif_RP);
            String gif_frameRate = settings.getString("gifFrameRate_value", "14");
            Log.i(TAG, "RP="+gif_RP+" fraerate="+gif_frameRate);
            SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String date    =    sDateFormat.format(new    java.util.Date());
            date += "-by_EL.gif";
            final String save_path =  tool.getSaveRootPath() + "/" + date;
            boolean isVideoPathHaveSpace = false;
            if (video_path.contains(" ")) {  //避免因为视频路径中包含空格而导致按照空格分割命令时出错
                video_path = video_path.replaceAll(" ", "_");
                isVideoPathHaveSpace = true;
            }
            String cmd = String.format(Locale.CHINA,
                    "-ss %f -t %f -i %s",
                    (gif_start_time/1000.0),
                    ((gif_end_time-gif_start_time)/1000.0),
                    video_path);
            //String cmd = "-ss "+(gif_start_time/1000.0)+" -t "+((gif_end_time-gif_start_time)/1000.0)+" -i "+video_path;
            Log.i(TAG, "gif start time(s)="+(gif_start_time/1000.0)+" time(ms)="+gif_start_time+" all="+((gif_end_time-gif_start_time)/1000.0));
            cmd += gif_RP.equals("-1")?"":" -s "+gif_RP;
            cmd += " -f gif";
            cmd += gif_frameRate.equals("-1")?"":" -r "+gif_frameRate;
            cmd += " "+save_path;
            String gif_cmd[] = cmd.split(" ");
            if (isVideoPathHaveSpace) {   //FIXME 现在的索引确定是5，小心以后变化啊
                gif_cmd[5] = gif_cmd[5].replaceAll("_", " ");
            }
            Log.i(TAG, "cmd = "+Arrays.toString(gif_cmd));

            while (ffmpeg.isFFmpegCommandRunning()) {
                //阻塞等待执行结束
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e){}
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
                        Message msg = Message.obtain();
                        msg.obj = save_path;
                        msg.what = HandlerShotGifSuccess;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onFinish() {}
                });
            } catch (FFmpegCommandAlreadyRunningException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                handler.sendEmptyMessage(HandlerShotGifFail);
            }
        }
    }

    private android.view.GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (videoview.isPlaying()) {
                videoview.pause();
                play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_play);
                handler.removeMessages(HandlerUpdateControlBarCurrentTime);
            }
            else {
                videoview.setBackgroundResource(0);
                videoview.start();
                play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_pause);
                handler.sendEmptyMessage(HandlerUpdateControlBarCurrentTime);
                btn_status.setText(R.string.player_text_rotationScreen);
            }

            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i("test", "单击屏幕");
            handler.sendEmptyMessage(HandlerStatusShowTime);

            if (settings.getBoolean("isHideButton", false) && isORIENTATION_LANDSCAPE) {
                if (isHideBtn) {
                    btn_status.setVisibility(View.VISIBLE);
                    btn_done.setVisibility(View.  VISIBLE);
                    btn_shot.setVisibility(View.  VISIBLE);
                    player_controlBar_layout.setVisibility(View.VISIBLE);
                    text_count.setVisibility(View.VISIBLE);
                    isHideBtn = false;
                }
                else {
                    btn_status.setVisibility(View.INVISIBLE);
                    btn_done.setVisibility(View.  INVISIBLE);
                    btn_shot.setVisibility(View.  INVISIBLE);
                    player_controlBar_layout.setVisibility(View.INVISIBLE);
                    text_count.setVisibility(View.INVISIBLE);
                    isHideBtn = true;
                }
            }

            return false;
        }


        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //videoview.pause();
            //play_controlBar_play_pause_btn.setImageResource(android.R.drawable.ic_media_play);
            video_time.setVisibility(View.VISIBLE);
            int px2ime = Integer.valueOf(settings.getString("gestureSensibility", "10"));
            videoview.seekTo(videoview.getCurrentPosition()-(int)distanceX*px2ime);
            String res;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            long lt = videoview.getCurrentPosition();
            Date date = new Date(lt);
            res = simpleDateFormat.format(date);
            res += "/"+duration_text;
            video_time.setText(res);
            autoHideTime();

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //video_time.setVisibility(View.GONE);
            return true;
        }
    };

    Timer tHide = null;
    private void autoHideTime() {
        if (tHide == null) {
            Log.i("test","call in autoHideTime with tHide is null");
            tHide = new Timer();
            tHide.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(HandlerStatusHideTime);
                    tHide = null;
                }
            }, 2000);
        }
    }


    private static class MyHandler extends Handler {
        private final WeakReference<PlayerActivity> mActivity;

        private MyHandler(PlayerActivity activity) {
            mActivity = new WeakReference<PlayerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final PlayerActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case 1:
                        activity.text_count.setText(msg.obj.toString());
                        break;
                    case 2:
                        //text_count.setText(msg.obj.toString());
                        Toast.makeText(activity, (String)msg.obj, Toast.LENGTH_LONG).show();
                        break;
                    case 3:
                        if (activity.settings.getBoolean("isSortPicture", true)) {
                            Intent intent = new Intent(activity, ChooseActivity.class);
                            activity.startActivity(intent);
                        }
                        else {
                            Intent intent = new Intent(activity, MarkPictureActivity2.class);
                            activity.startActivity(intent);
                        }
                        break;
                    case HandlerStatusHideTime:
                        activity.isShowingTime = false;
                        activity.video_time.setVisibility(View.GONE);
                        break;
                    case HandlerStatusShowTime:
                        activity.isShowingTime = true;
                        activity.video_time.setVisibility(View.VISIBLE);
                        String res;
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        long lt = activity.videoview.getCurrentPosition();
                        Date date = new Date(lt);
                        res = simpleDateFormat.format(date);
                        res += "/"+activity.duration_text;
                        activity.video_time.setText(res);
                        activity.autoHideTime();
                        if (activity.videoview.isPlaying() && activity.isShowingTime) {
                            activity.handler.sendEmptyMessageDelayed(HandlerStatusUpdateTime, 200);
                        }
                        Log.i("test", "res="+res);
                        break;
                    case HandlerStatusUpdateTime:
                        //video_time.setVisibility(View.VISIBLE);
                        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        lt = activity.videoview.getCurrentPosition();
                        date = new Date(lt);
                        res = simpleDateFormat.format(date);
                        res += "/"+activity.duration_text;
                        activity.video_time.setText(res);
                        if (activity.videoview.isPlaying() && activity.isShowingTime) {
                            activity.handler.sendEmptyMessageDelayed(HandlerStatusUpdateTime, 200);
                        }
                        break;
                    case HandlerShotGifSuccess:
                        activity.isShotingGif = false;
                        MediaScannerConnection.scanFile(activity, new String[]{msg.obj.toString()}, null, null);
                        Toast.makeText(activity, R.string.player_toast_shotGif_success, Toast.LENGTH_SHORT).show();
                        break;
                    case HandlerShotGifFail:
                        Toast.makeText(activity, R.string.player_toast_shotGif_fail, Toast.LENGTH_SHORT).show();
                        break;
                    case HandlerShotGifRunning:
                        Toast.makeText(activity, R.string.player_toast_shotGif_start, Toast.LENGTH_SHORT).show();
                        break;
                    case HandlerUpdateControlBarCurrentTime:
                        int currentTime = activity.videoview.getCurrentPosition();
                        int totally = activity.videoview.getDuration();
                        activity.updateTimeFormat(activity.play_controlBar_totally_time, totally);
                        activity.updateTimeFormat(activity.play_controlBar_current_time, currentTime);
                        activity.play_seekbar.setMax(totally);
                        activity.play_seekbar.setProgress(currentTime);
                        activity.handler.sendEmptyMessageDelayed(HandlerUpdateControlBarCurrentTime, 500);//500毫秒刷新
                        break;
                }
            }
        }
    }


    /**
     * 时间格式化
     *    作者：zzj丶
     *    链接：https://www.jianshu.com/p/564cffc2df87
     * @param textView    时间控件
     * @param millisecond 总时间 毫秒
     */
    private void updateTimeFormat(TextView textView, int millisecond) {
        //将毫秒转换为秒
        int second = millisecond / 1000;
        //计算小时
        int hh = second / 3600;
        //计算分钟
        int mm = second % 3600 / 60;
        //计算秒
        int ss = second % 60;
        //判断时间单位的位数
        String str = null;
        if (hh != 0) {//表示时间单位为三位
            str = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            str = String.format("%02d:%02d", mm, ss);
        }
        //将时间赋值给控件
        textView.setText(str);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (isORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                player_controlBar_layout.setVisibility(View.INVISIBLE);
                if (isHideBtn) {
                    btn_done.setVisibility(View.VISIBLE);
                    btn_shot.setVisibility(View.  VISIBLE);
                    btn_status.setVisibility(View.  VISIBLE);
                    text_count.setVisibility(View.VISIBLE);
                    isHideBtn = false;
                }
                isORIENTATION_LANDSCAPE = false;
                return true;
            }
            else {
                if (isShotGif) {
                    utils.finishActivity(MainActivity.instance);
                    startActivity(new Intent(this, MainActivity.class));   //被动刷新
                }
            }
            return super.onKeyDown(keyCode, event);
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }

}
