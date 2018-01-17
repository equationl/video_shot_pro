package com.equationl.videoshotpro;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn = (Button) findViewById(R.id.command_button);
        textview = (TextView) findViewById(R.id.command_text);
        edittext = (EditText) findViewById(R.id.command_editText);
        sv = (ScrollView) findViewById(R.id.command_scroll);

        tool = new Tools();

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
                            }
                            @Override
                            public void onFailure(String message) {
                                message = blank2n(message);
                                textview.setText(message+"\n执行失败");
                                sv.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                            @Override
                            public void onSuccess(String message) {
                                message = blank2n(message);
                                Log.i("el_test", message);
                                textview.setText(message+"\n执行成功");
                                sv.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                            @Override
                            public void onProgress(String message) {
                                message = blank2n(message);
                                textview.setText(textview.getText().toString()+message);
                                sv.fullScroll(ScrollView.FOCUS_DOWN);
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
                //String path = uri.getPath();
                String path = tool.getImageAbsolutePath(this, uri);
                int index = edittext.getSelectionStart();
                Editable editable = edittext.getText();
                editable.insert(index, path);
            }
            if (ResultDo == ActivityResultCodeAddTime) {
                Uri uri = data.getData();
                String path = tool.getImageAbsolutePath(this, uri);

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

}
