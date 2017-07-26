package com.equationl.videoshotpro;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MarkPictureActivity extends AppCompatActivity {
    ImageView imagview;
    String[] fileList;
    Button btn_start;
    int pic_num,pic_no=0,flag=0;
    AlertDialog.Builder builder;
    private LayoutInflater mLayoutInflater;
    private View view;
    SharedPreferences settings;
    TextView tip_text, nums_tip_text;
    boolean isLongPress=false;

    Tools tool = new Tools();

    private static final int HandlerStatusHideTipText = 10010;
    private static final int HandlerStatusLongIsWorking = 10011;
    private static final int HandlerStatusIsLongPress = 10012;

    public static MarkPictureActivity instance = null;   //FIXME  暂时这样吧，实在找不到更好的办法了

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //4.4以上设置透明状态栏
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_mark_picture);

        instance = this;

        imagview = (ImageView) findViewById(R.id.imageView);
        btn_start = (Button) findViewById(R.id.button_start);
        tip_text = (TextView) findViewById(R.id.make_picture_tip);
        nums_tip_text  = (TextView) findViewById(R.id.make_picture_nums_tip);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        final File filepath = new File(getExternalCacheDir().toString());
        fileList = filepath.list();
        pic_num = fileList.length;

        if (pic_num < 1) {
            Toast.makeText(this, R.string.markPicture_toast_readFile_fail, Toast.LENGTH_SHORT).show();
            finish();
        }

        nums_tip_text.setText("0/"+pic_num);

        Log.i("cao",pic_num+"");

        for (int i=0;i<pic_num;i++) {
            fileList[i] = "del";
        }

        btn_start.setVisibility(View.GONE);



        flag=0;
        imagview.setOnTouchListener(new View.OnTouchListener() {
            double  mPosX,mPosY,mCurPosX,mCurPosY,offsetX,offsetY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (flag == 0) {
                    Log.i("el", "第一次调用：pic_no="+pic_no);
                    set_image(pic_no);
                    flag++;
                    btn_start.setVisibility(View.VISIBLE);
                    imagview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    return false;
                }
                else {
                    Log.i("el", "非第一次调用：pic_no="+pic_no);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mPosX = event.getX();
                            mPosY = event.getY();
                            /*checkIsLongPress(false);
                            Log.i("test", "in ACTION_DOWN");  */
                            withdrawStep(mPosX, mPosY);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mCurPosX = event.getX();
                            mCurPosY = event.getY();
                            offsetX = mPosX - mCurPosX;
                            offsetY = mPosY - mCurPosY;
                            /*if (Math.abs(offsetX)<400 && Math.abs(offsetY)<400) {
                                checkIsLongPress(true);
                            }*/
                            break;
                        case MotionEvent.ACTION_SCROLL:

                        case MotionEvent.ACTION_UP:
                            if (pic_no >= pic_num) {
                                Toast.makeText(getApplicationContext(),"已是最后一张，请点击右上角“开始合成”", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            offsetX = mPosX - mCurPosX;
                            offsetY = mPosY - mCurPosY;
                            if (Math.abs(offsetY) >= Math.abs(offsetX) ) {
                                Log.i("pic num", pic_num+"");
                                Log.i("pic no", pic_no+"");
                                if (mCurPosY - mPosY > 0
                                        && (Math.abs(mCurPosY - mPosY) > 200)) {
                                    //向下滑動
                                    //Toast.makeText(getApplicationContext(),"向下滑动",Toast.LENGTH_SHORT).show();
                                    if (fileList[pic_no].equals("text")) {
                                        Toast.makeText(MarkPictureActivity.this, "添加文字后不允许裁切！", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                    tip_text.setText("裁切");
                                    tip_text.setVisibility(View.VISIBLE);
                                    autoHideText();
                                    nums_tip_text.setText((pic_no+1+"/")+pic_num);
                                    set_image(pic_no+1);
                                    fileList[pic_no] = "cut";
                                    pic_no++;
                                }
                                else if (mCurPosY - mPosY < 0
                                        && (Math.abs(mCurPosY - mPosY) > 200)) {
                                    //向上滑动
                                    //Toast.makeText(getApplicationContext(),"向上滑动",Toast.LENGTH_SHORT).show();
                                    tip_text.setText("全图");
                                    tip_text.setVisibility(View.VISIBLE);
                                    autoHideText();
                                    nums_tip_text.setText((pic_no+1+"/")+pic_num);
                                    set_image(pic_no+1);
                                    fileList[pic_no] = "all";
                                    pic_no++;
                                }
                            }
                            else {
                                if (mCurPosX - mPosX < 0
                                        && (Math.abs(mCurPosX - mPosX) > 200)) {
                                        //向左滑动
                                    nums_tip_text.setText((pic_no+1+"/")+pic_num);
                                    DialogInterface.OnClickListener dialogOnclicListener=new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch(which){
                                                case Dialog.BUTTON_POSITIVE:
                                                    EditText edit_text = (EditText) view.findViewById(R.id.input_text);
                                                    EditText edit_size = (EditText) view.findViewById(R.id.input_size);
                                                    RadioGroup radiogroup = (RadioGroup) view.findViewById(R.id.color);
                                                    RadioButton radio_color = (RadioButton) view.findViewById(radiogroup.getCheckedRadioButtonId());
                                                    String text_color = radio_color.getText().toString();
                                                    String text = edit_text.getText().toString();
                                                    if (text.equals("")) {
                                                        break;
                                                    }
                                                    int text_size;
                                                    if (edit_size.getText().toString().equals("")) {
                                                        text_size = 30;
                                                    }
                                                    else {
                                                        text_size = Integer.parseInt(edit_size.getText().toString());
                                                    }

                                                    //Log.i("ccccc",text);
                                                    Bitmap bm;
                                                    if (pic_no<pic_num && fileList[pic_no].equals("text")) {
                                                        bm = addBitmap(getBitmapFromFile(pic_no+"_t"),addTextToImage(getBitmapFromFile(pic_no+"_t"),text,text_size,text_color));
                                                    }
                                                    else {
                                                        bm = addBitmap(getBitmapFromFile(pic_no+""),addTextToImage(getBitmapFromFile(pic_no+""),text,text_size,text_color));
                                                    }
                                                    try {
                                                        saveMyBitmap(bm,pic_no+"_t");
                                                    }
                                                    catch (IOException e) {
                                                        Log.i("excuse me?",e.toString());
                                                        Toast.makeText(getApplicationContext(),"写入缓存失败！"+e.toString(), Toast.LENGTH_LONG).show();
                                                    }
                                                    set_image(pic_no, "_t");
                                                    fileList[pic_no] = "text";
                                                    break;
                                                case Dialog.BUTTON_NEGATIVE:
                                                    break;
                                            }
                                        }
                                    };
                                        mLayoutInflater= LayoutInflater.from(MarkPictureActivity.this);
                                        view=mLayoutInflater.inflate(R.layout.dialog_mark_picture, null, false);
                                        builder = new AlertDialog.Builder(MarkPictureActivity.this);
                                        builder.setTitle("请输入要添加的文字")
                                            .setView(view)
                                            .setPositiveButton("确定",dialogOnclicListener)
                                            .setNegativeButton("取消", dialogOnclicListener)
                                            .setCancelable(false)
                                            .create();
                                            builder.show();
                                }
                                else if (mCurPosX - mPosX > 0
                                        && (Math.abs(mCurPosX - mPosX) > 200)) {
                                    //向右滑动
                                    tip_text.setText("删除");
                                    tip_text.setVisibility(View.VISIBLE);
                                    autoHideText();
                                    nums_tip_text.setText((pic_no+1+"/")+pic_num);
                                    set_image(pic_no+1);
                                    pic_no++;
                                }
                            }
                            break;
                    }
                }
                return true;
            }

        });

        btn_start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pic_no <= 0) {
                    Toast.makeText(getApplicationContext(),"至少需要选择一张图片", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent intent = new Intent(MarkPictureActivity.this, BuildPictureActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArray("fileList",fileList);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });
    }

    private void set_image(int  no) {
        set_image(no, "null");
    }

    private void set_image(int no, String flag) {
        if (no < pic_num) {
            if (!flag.equals("null")) {
                imagview.setImageBitmap(getBitmapFromFile(no+flag));
            }
            else {
                imagview.setImageBitmap(getBitmapFromFile(no+""));
            }
        }
    }

    private Bitmap getBitmapFromFile(String no) {
        Bitmap bm = null;
        String extension = settings.getBoolean("isShotToJpg", true) ? "jpg":"png";

        try {
            bm = tool.getBitmapFromFile(no, getExternalCacheDir(),extension);
        }  catch (Exception e) {
            Toast.makeText(getApplicationContext(),"获取截图失败"+e, Toast.LENGTH_LONG).show();
        }

        return bm;
    }

    private Bitmap addTextToImage(Bitmap bm, String text, int size, String color) {
        if (size < 0) {
            size = 30;
        }
        int text_color= Color.BLACK;
        if (color.equals("红")) {
            text_color= Color.RED;
        }
        else if (color.equals("蓝")) {
            text_color= Color.BLUE;
        }
        else if (color.equals("绿")) {
            text_color= Color.GREEN;
        }
        int width = bm.getWidth();

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(text_color);
        textPaint.setTextSize(size);

        Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt();

        int char_height = fmi.bottom-fmi.top;

        Log.i("text", char_height+" "+fmi.bottom +" " +fmi.top);

        String[] len = text.split("\n");
        int t_height=0;
        for (int i=0;i<len.length;i++) {
            t_height+=char_height;
            int string_wdith = (int)textPaint.measureText(len[i]);
            if (string_wdith > width) {
                t_height+=char_height*(string_wdith/width);
            }
        }

        Bitmap result = Bitmap.createBitmap(width,t_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, width, bm.getHeight(), paint);

        StaticLayout layout = new StaticLayout(text,textPaint,canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL,1.0F,0.0F,true);
        canvas.translate(5,0);
        layout.draw(canvas);
        return result;
    }

    private Bitmap addBitmap(Bitmap first, Bitmap second) {
        return tool.jointBitmap(first, second);
    }

    public boolean saveMyBitmap(Bitmap bmp, String bitName) throws IOException {
        boolean flag;
        try {
            if (settings.getBoolean("isShotToJpg", true)) {
                tool.saveBitmap2png(bmp,bitName, getExternalCacheDir(), true, 100);
                flag = true;
            }
            else {
                tool.saveBitmap2png(bmp,bitName, getExternalCacheDir());
                flag = true;
            }
        } catch (Exception e) {
            flag = false;
        }

        return flag;
    }

    Timer tHide = null;
    private void autoHideText() {
        if (tHide == null) {
            Log.i("test","call in autoHideTime with tHide is null");
            tHide = new Timer();
            tHide.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(HandlerStatusHideTipText);
                    tHide = null;
                }
            }, 1000);
        }
    }

    private void checkIsLongPress(boolean isDelete) {
        Log.i("el_test", "调用 checkIsLongPress  ??");
        if (isDelete) {
            Log.i("el_test", "删除 checkIsLongPress   ??");
            handler.removeMessages(HandlerStatusIsLongPress);
            isLongPress = false;
        }
        else if (pic_no > 0){
            if (!isLongPress) {
                isLongPress = true;
                handler.sendEmptyMessageDelayed(HandlerStatusIsLongPress,1000);
            }
            else {
                Log.i("el_test", "运行 checkIsLongPress  ??");
                isLongPress = false;
                handler.sendEmptyMessage(HandlerStatusLongIsWorking);
            }
        }
    }

    long callWithdrawTime = 0;
    double x_l, y_l;
    private void withdrawStep(double x, double y) {
        long time_now = System.currentTimeMillis();
        long delay = time_now-callWithdrawTime;
        double x_d = Math.abs(x-x_l);
        double y_d = Math.abs(y-y_l);

        if (delay<500 && x_d<20 && y_d<20 && (pic_no>0
                || fileList[pic_no].equals("text"))) {
            handler.sendEmptyMessage(HandlerStatusLongIsWorking);
        }
        callWithdrawTime = time_now;
        x_l = x;
        y_l = y;
    }



    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HandlerStatusHideTipText:
                    tip_text.setVisibility(View.GONE);
                    break;
                case HandlerStatusLongIsWorking:
                    tip_text.setText("撤销");
                    tip_text.setVisibility(View.VISIBLE);
                    autoHideText();
                    if (pic_no<pic_num && fileList[pic_no].equals("text")) {
                        set_image(pic_no);
                        fileList[pic_no] = "del";
                    }
                    else {
                        nums_tip_text.setText((pic_no+"/")+pic_num);
                        set_image(pic_no-1);
                        fileList[pic_no-1] = "del";
                        pic_no--;
                    }
                    break;
                case HandlerStatusIsLongPress:
                    isLongPress = false;
                    break;
            }

        }
    };
}
