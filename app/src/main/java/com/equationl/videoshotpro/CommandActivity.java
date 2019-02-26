package com.equationl.videoshotpro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

public class CommandActivity extends AppCompatActivity {
    Button btn;
    TextView textview;
    EditText edittext;
    Tools tool;
    ScrollView sv;
    int ResultDo=1;

    private static final int ActivityResultCodeAddPath = 1;
    private static final int ActivityResultCodeAddTime = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command);
        Toolbar toolbar = (Toolbar) findViewById(R.id.command_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btn = (Button) findViewById(R.id.command_button);
        textview = (TextView) findViewById(R.id.command_text);
        edittext = (EditText) findViewById(R.id.command_editText);
        sv = (ScrollView) findViewById(R.id.command_scroll);

        tool = new Tools();


        /*
         * from:https://blog.csdn.net/stimgo/article/details/80884146
         * 监听键盘是否弹出，防止键盘遮挡输出信息
        * */
        edittext.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            //当键盘弹出隐藏的时候会 调用此方法。
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //获取当前界面可视部分
                getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
                //获取屏幕的高度
                int screenHeight = getWindow().getDecorView().getRootView().getHeight();
                //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
                int heightDifference = screenHeight - r.bottom;
                if (heightDifference > 0) {
                    scrollToBottom(sv, textview);
                } else {
                }
            }
        });


        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = edittext.getText().toString();
                textview.setText("");
                Log.i("el_test", text);
                FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
                if (!ffmpeg.isFFmpegCommandRunning()) {
                    String cmd[] = text.split(" ");
                    try {
                        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                            @Override
                            public void onStart() {
                                btn.setClickable(false);
                                hintKeyBoard();
                            }
                            @Override
                            public void onFailure(String message) {
                                message = blank2n(message);
                                textview.setText(message+"\n执行失败");
                                //sv.fullScroll(ScrollView.FOCUS_DOWN);
                                scrollToBottom(sv, textview);
                            }
                            @Override
                            public void onSuccess(String message) {
                                message = blank2n(message);
                                Log.i("el_test", message);
                                textview.setText(message+"\n执行成功");
                                //sv.fullScroll(ScrollView.FOCUS_DOWN);
                                scrollToBottom(sv, textview);
                            }
                            @Override
                            public void onProgress(String message) {
                                message = blank2n(message);
                                textview.setText(textview.getText().toString()+message);
                                //sv.fullScroll(ScrollView.FOCUS_DOWN);
                                scrollToBottom(sv, textview);
                            }
                            @Override
                            public void onFinish() {
                                btn.setClickable(true);
                            }
                        });
                        } catch (FFmpegCommandAlreadyRunningException e) {
                        textview.setText("运行错误");
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_command, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.command_menu_add_path) {
            ResultDo = ActivityResultCodeAddPath;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择文件"),1);
            return true;
        }
        if (id == R.id.command_menu_add_time) {
            ResultDo = ActivityResultCodeAddTime;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            intent.addCategory(intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "请选择文件"),1);
            return true;
        }

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivityResultCodeAddTime && resultCode == 1) {
            int time = data.getIntExtra("time", 0);
            int index = edittext.getSelectionStart();
            Editable editable = edittext.getText();
            editable.insert(index, time/1000.0+"");
        }
        else if (resultCode == Activity.RESULT_OK) {
            if (ResultDo == ActivityResultCodeAddPath) {
                Uri uri = data.getData();
                String path = tool.getImageAbsolutePath(this, uri);
                int index = edittext.getSelectionStart();
                Editable editable = edittext.getText();
                editable.insert(index, path);
            }
            if (ResultDo == ActivityResultCodeAddTime) {
                Uri uri = data.getData();
                ResultDo = 0;
                Intent intent = new Intent(this, PlayerForDataActivity.class);
                intent.putExtra("do", "getTime");
                intent.setData(uri);
                startActivityForResult(intent, ActivityResultCodeAddTime);
            }
        }
        else {
            Toast.makeText(getApplicationContext(),"未选择文件！", Toast.LENGTH_LONG).show();
        }
    }

    private static String blank2n(String str) {
        if(str!=null && !"".equals(str)) {
            String newline = System.getProperty("line.separator");
            return str.replaceAll(newline,"\n");
        }else {
            return str;
        }
    }

    /**
    *  作者：gundumw100
     *  原文：https://blog.csdn.net/gundumw100/article/details/69983948
    * */
    public static void scrollToBottom(final View scroll, final View inner) {
        Handler mHandler = new Handler();
        mHandler.post(new Runnable() {
            public void run() {
                if (scroll == null || inner == null) {
                    return;
                }
                int offset = inner.getMeasuredHeight() - scroll.getHeight();
                if (offset < 0) {
                    offset = 0;
                }
                scroll.scrollTo(0, offset);
            }
        });
    }

    /**
     * from:https://www.jianshu.com/p/3f2b0ad3565b
     * */
    public void hintKeyBoard() {
        //拿到InputMethodManager
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //如果window上view获取焦点 && view不为空
        if (imm.isActive() && getCurrentFocus() != null) {
            //拿到view的token 不为空
            if (getCurrentFocus().getWindowToken() != null) {
                //表示软键盘窗口总是隐藏，除非开始时以SHOW_FORCED显示。
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

}
