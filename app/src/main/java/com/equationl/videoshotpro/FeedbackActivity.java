package com.equationl.videoshotpro;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.Resource;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FeedbackActivity extends AppCompatActivity {
    Button btn_submit;
    EditText edit_content, edit_email;
    String text_content, text_email;
    Resources res;
    SharedPreferences sp_init;

    private static final int HanderStatusPostFeedbackSuccessful = 1;
    private static final int HanderStatusPostFeedbackNetFail = 2;
    private static final int HanderStatusPostFeedbackServerFail = 3;


    private static final String TAG = "el,In FeedbackActivity";

    private final MyHandler handler = new MyHandler(this);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        btn_submit = (Button) findViewById(R.id.btn_feedback_submit);
        edit_content = (EditText) findViewById(R.id.edit_feedback_content);
        edit_email = (EditText) findViewById(R.id.edit_feedback_email);
        res = getResources();

        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        btn_submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                text_content = edit_content.getText().toString();
                text_email = edit_email.getText().toString();

                if (text_content.equals("")) {
                    Toast.makeText(FeedbackActivity.this, res.getString(R.string.feedback_toast_content_null), Toast.LENGTH_SHORT).show();
                }
                else if (text_email.equals("")) {
                    Toast.makeText(FeedbackActivity.this, R.string.feedback_toast_email_null, Toast.LENGTH_SHORT).show();
                }

                else {
                    String versionName;
                    int currentapiVersion = 0;
                    try {
                        PackageManager packageManager = getPackageManager();
                        PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
                        versionName = packInfo.versionName;
                        currentapiVersion = android.os.Build.VERSION.SDK_INT;
                    } catch (Exception ex) {
                        versionName = "NULL";
                    }
                    String mail_content = String.format(getResources().getString(R.string.main_mail_content),
                            versionName, currentapiVersion + "", android.os.Build.MODEL);
                    int userFlagID = sp_init.getInt("userFlagID", 0);
                    text_email += "\n-----------\n用户ID：" + userFlagID;
                    mail_content = text_content + "\n-----------\n系统信息：\n" + mail_content + "\n------------\n联系信息：\n" + text_email;
                    mail_content = mail_content.replaceAll("\n", "<br />");
                    mail_content = mail_content.replaceAll(" ", "&nbsp;");
                    //Toast.makeText(FeedbackActivity.this, mail_content, Toast.LENGTH_LONG).show();
                    btn_submit.setText(R.string.feedback_button_posting_text);
                    btn_submit.setClickable(false);
                    postFeedback(mail_content);
                }
            }
        });


    }


    private void postFeedback(final String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "http://app.equationl.com/feedback/";
                    OkHttpClient okHttpClient = new OkHttpClient();
                    RequestBody formBody = new FormBody.Builder().add("content", content)
                            .build();
                    Request request = new Request.Builder().url(url).post(formBody).build();
                    okhttp3.Response response = okHttpClient.newCall(request).execute();
                    if (response.isSuccessful()) {
                        if (response.code() == 200) {
                            handler.sendEmptyMessage(HanderStatusPostFeedbackSuccessful);
                            Log.i(TAG, "request successful:"+response.body().string());
                        }
                        else {
                            Log.i(TAG, "request fail 3");
                            Message msg = Message.obtain();
                            msg.obj = content;
                            msg.what = HanderStatusPostFeedbackServerFail;
                            handler.sendMessage(msg);
                        }
                    }
                    else {
                        Message msg = Message.obtain();
                        msg.obj = content;
                        msg.what = HanderStatusPostFeedbackServerFail;
                        handler.sendMessage(msg);
                        Log.i(TAG, "request fail 1");
                    }
                } catch (Exception e) {
                    Message msg = Message.obtain();
                    msg.obj = content;
                    msg.what = HanderStatusPostFeedbackNetFail;
                    handler.sendMessage(msg);
                    Log.i(TAG, "request fail 2");
                    CrashReport.postCatchedException(e);
                }
            }
        }).start();
    }




    private static class MyHandler extends Handler {
        private final WeakReference<FeedbackActivity> mActivity;

        public MyHandler(FeedbackActivity activity) {
            mActivity = new WeakReference<FeedbackActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FeedbackActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HanderStatusPostFeedbackSuccessful:
                        Toast.makeText(activity, R.string.feedback_toast_postSuccessful, Toast.LENGTH_LONG).show();
                        activity.finish();
                        break;
                    case HanderStatusPostFeedbackNetFail:
                        activity.showDialogPostFail(msg.obj.toString(), HanderStatusPostFeedbackNetFail);
                        break;
                    case HanderStatusPostFeedbackServerFail:
                        activity.showDialogPostFail(msg.obj.toString(), HanderStatusPostFeedbackServerFail);
                        break;
                }
            }
        }
    }


    private void showDialogPostFail(final String content, int result) {
        String text = "发送失败";
        if (result == HanderStatusPostFeedbackServerFail) {
            text = res.getString(R.string.feedback_dialog_postFailServer_content);
        }
        else if (result == HanderStatusPostFeedbackNetFail) {
            text = res.getString(R.string.feedback_dialog_postFailNet_content);
        }

        Dialog dialog_permission = new AlertDialog.Builder(FeedbackActivity.this).setCancelable(true).setTitle("")
                .setMessage(text)
                .setPositiveButton(res.getString(R.string.feedback_dialog_postFail_btn_retry),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //重试
                                postFeedback(content);
                                dialog.dismiss();
                            }
                        }).setNegativeButton(res.getString(R.string.feedback_dialog_postFail_btn_sendBySelf),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //手动发送
                                Intent data=new Intent(Intent.ACTION_SENDTO);
                                data.setData(Uri.parse("mailto:admin@likehide.com"));
                                data.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.main_mail_title));
                                data.putExtra(Intent.EXTRA_TEXT, content);
                                startActivity(data);
                                dialog.dismiss();
                            }
                        }).create();
        dialog_permission.show();
    }

}
