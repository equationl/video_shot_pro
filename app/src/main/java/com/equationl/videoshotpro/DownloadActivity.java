package com.equationl.videoshotpro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.ffmpeg.FFmpeg;
import com.equationl.videoshotpro.utils.Utils;
import com.gastudio.downloadloadding.library.GADownloadingView;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.SpeedCalculator;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.listener.DownloadListener4WithSpeed;
import com.liulishuo.okdownload.core.listener.assist.Listener4SpeedAssistExtend;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadActivity extends Activity {
    AlertDialog downloadDialog;
    GADownloadingView gaDownloadingView;
    TextView downloadViewText;
    Resources res;
    String trueMD5;

    long dataTotalLength;

    private static final int HandlerDownLoadStatusOnTaskStart = 30001;
    private static final int HandlerDownLoadStatusOnProgress = 30002;
    private static final int HandlerDownLoadStatusOnCompleted = 30003;
    private static final int HandlerDownLoadStatusOnError = 30004;

    private static final String DownloadFFmpegUrl = "http://121.199.60.141:1998/ffmpeg/";
    private static final String TAG = "EL, in DownloadActivity";

    private final MyHandler handler = new MyHandler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        String prefix = getIntent().getStringExtra("prefix");
        Log.i(TAG, "onCreate: prefix="+prefix);

        res = getResources();

        getFileMd5(prefix);
        showDownloadDialog();
        File data = new File(getExternalCacheDir(), "ffmpeg");
        downloadFile(data, prefix);
    }

    private void showDownloadDialog() {
        View view = View.inflate(this, R.layout.dialog_play_for_data_download, null);
        downloadViewText = view.findViewById(R.id.player_progress_download_text);
        gaDownloadingView = view.findViewById(R.id.player_progress_download);
        gaDownloadingView.performAnimation();
        downloadDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .show();
        Window window = downloadDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        else {
            Log.e(TAG, "showDownloadDialog: get window fail");
        }
    }

    private void downloadFile(File file, final String prefix) {
        DownloadListener4WithSpeed listener = new DownloadListener4WithSpeed() {
            @Override
            public void taskStart(@NonNull DownloadTask task) {
                handler.sendEmptyMessage(HandlerDownLoadStatusOnTaskStart);
            }

            @Override
            public void connectStart(@NonNull DownloadTask task, int blockIndex, @NonNull Map<String, List<String>> requestHeaderFields) {

            }

            @Override
            public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {

            }

            @Override
            public void infoReady(@NonNull DownloadTask task, @NonNull BreakpointInfo info, boolean fromBreakpoint, @NonNull Listener4SpeedAssistExtend.Listener4SpeedModel model) {
                dataTotalLength = info.getTotalLength();
            }

            @Override
            public void progressBlock(@NonNull DownloadTask task, int blockIndex, long currentBlockOffset, @NonNull SpeedCalculator blockSpeed) {

            }

            @Override
            public void progress(@NonNull DownloadTask task, long currentOffset, @NonNull SpeedCalculator taskSpeed) {
                //Log.i(TAG, "total="+dataTotalLength+" current="+currentOffset);
                Bundle bundle = new Bundle();
                bundle.putLong("dataTotalLength", dataTotalLength);
                bundle.putLong("currentOffset", currentOffset);
                Message msg = Message.obtain();
                msg.what = HandlerDownLoadStatusOnProgress;
                msg.obj = bundle;
                handler.sendMessage(msg);
            }

            @Override
            public void blockEnd(@NonNull DownloadTask task, int blockIndex, BlockInfo info, @NonNull SpeedCalculator blockSpeed) {

            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause, @NonNull SpeedCalculator taskSpeed) {
                Log.i(TAG, "taskEnd: ???????????");
                File data = new File(getExternalCacheDir(), "ffmpeg");
                String fileMD5 = Utils.fileToMD5(data.getPath());
                Log.i(TAG, "taskEnd: downloadFile md5="+fileMD5);
                Log.i(TAG, "taskEnd:     trueFile md5="+trueMD5);

                if (fileMD5 == null || trueMD5 == null) {
                    Log.e(TAG, "taskEnd: get fileMD5 fail!");
                    handler.sendEmptyMessage(HandlerDownLoadStatusOnError);
                }
                else {
                    if (!fileMD5.equalsIgnoreCase(trueMD5)) {
                        Log.e(TAG, "taskEnd: md5 not equals!");
                        handler.sendEmptyMessage(HandlerDownLoadStatusOnError);
                    }
                    else {
                        handler.sendEmptyMessage(HandlerDownLoadStatusOnCompleted);
                    }
                }
            }
        };


        DownloadTask task = new DownloadTask.Builder(DownloadFFmpegUrl+prefix+"ffmpeg", file.getParentFile())
                .setFilename(file.getName())
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(30)
                // do re-download even if the task has already been completed in the past.
                .setPassIfAlreadyCompleted(false)
                .build();
        task.enqueue(listener);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<DownloadActivity> mActivity;

        private MyHandler(DownloadActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DownloadActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerDownLoadStatusOnTaskStart:
                        Log.i(TAG, "开始下载文件！");
                        break;
                    case HandlerDownLoadStatusOnProgress:
                        Bundle bundle1 = (Bundle) msg.obj;
                        long dataTotalLength = bundle1.getLong("dataTotalLength", 0);
                        long currentOffset = bundle1.getLong("currentOffset", 0);
                        int progress = (int)(currentOffset*100.0/dataTotalLength);
                        String progressText = String.format(
                                activity.res.getString(R.string.downloadActivity_text_download_progress),
                                Utils.bytesBeHuman(currentOffset),
                                Utils.bytesBeHuman(dataTotalLength));
                        activity.gaDownloadingView.updateProgress(progress);
                        activity.downloadViewText.setText(progressText);
                        //Log.i(TAG, "progress="+progress);
                        break;
                    case HandlerDownLoadStatusOnCompleted:
                        File file = new File(activity.getExternalCacheDir(), "ffmpeg");
                        FFmpeg.getInstance(activity).setFFmpegFile(file);
                        Log.i(TAG, "文件下载完成");
                        activity.gaDownloadingView.updateProgress(100);
                        activity.downloadDialog.dismiss();
                        Toast.makeText(activity, R.string.downloadActivity_toast_downloadData_download_success, Toast.LENGTH_SHORT).show();
                        activity.finish();
                        break;
                    case HandlerDownLoadStatusOnError:
                        Log.i(TAG, "文件下载错误");
                        activity.gaDownloadingView.onFail();
                        activity.downloadDialog.setCancelable(true);
                        Log.e(TAG, "download fail!" );
                        Toast.makeText(activity, R.string.downloadActivity_toast_downloadData_fileImperfect, Toast.LENGTH_LONG).show();
                        activity.finish();
                        break;
                }
            }
        }
    }

    private void getFileMd5(final String prefix) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = DownloadFFmpegUrl+prefix+"/md5";
                OkHttpClient httpClient = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                try {
                    Response response = httpClient.newCall(request).execute();
                    String md5 = response.body() != null ? response.body().string() : null;
                    Pattern p = Pattern.compile("\\s*|\t|\r|\n");
                    Matcher m = p.matcher(md5);
                    trueMD5 = m.replaceAll("");
                    Log.i(TAG, String.format("getFileMd5: %s md5 = %s", prefix, trueMD5));
                } catch (IOException e) {
                    Log.e(TAG, "getFileMd5: ", e);
                    trueMD5 = null;
                }
            }
        }).start();
    }
}
