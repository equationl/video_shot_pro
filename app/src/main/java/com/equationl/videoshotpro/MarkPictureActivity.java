package com.equationl.videoshotpro;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import android.support.v7.app.AppCompatDelegate;
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
import android.widget.TextView;
import android.widget.Toast;

import com.dingmouren.colorpicker.ColorPickerDialog;
import com.dingmouren.colorpicker.OnColorPickerListener;
import com.equationl.videoshotpro.Image.Tools;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class MarkPictureActivity extends AppCompatActivity {
    ImageView imagview, imageViewText, imageUndo;
    String[] fileList;
    Button btn_start;
    Button start_color_picker, start_color_picker_bg;
    int pic_num,pic_no=0,flag=0;
    AlertDialog.Builder builder;
    LayoutInflater mLayoutInflater;
    private View view;
    SharedPreferences settings, sp_init;
    TextView tip_text, nums_tip_text;
    boolean isLongPress=false;
    Boolean isFromExtra;
    ProgressDialog dialog;
    Resources res;
    int text_color=Color.BLACK, bg_color = Color.argb(0, 255, 255, 255);
    boolean isMoveText=false;
    int bgRealWidth, bgRealHeight, relativeX, relativeY;
    Bitmap TextImgTemp=null;
    String addTextString;
    int addTextStringSize;

    Tools tool = new Tools();

    private static final int HandlerStatusHideTipText = 10010;
    private static final int HandlerStatusLongIsWorking = 10011;
    private static final int HandlerStatusIsLongPress = 10012;
    private static final int HandlerCheckImgSaveFail = 10013;
    private static final int HandlerStatusProgressRunning = 10014;
    private static final int HandlerStatusProgressDone = 10015;
    private static final int HandlerStatusGetImgFail = 10016;

    private static final String TAG = "EL,In MarkActivity";


    private final MyHandler handler = new MyHandler(this);
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

        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        imagview = (ImageView) findViewById(R.id.imageView);
        imageViewText = (ImageView) findViewById(R.id.imageViewText);
        imageUndo = (ImageView) findViewById(R.id.mark_picture_undo);
        btn_start = (Button) findViewById(R.id.button_start);
        tip_text = (TextView) findViewById(R.id.make_picture_tip);
        nums_tip_text  = (TextView) findViewById(R.id.make_picture_nums_tip);

        //imagview.setImageDrawable(getResources().getDrawable(R.drawable.tip));

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();

        String filepath = getExternalCacheDir().toString();
        fileList = tool.getFileOrderByName(filepath);
        pic_num = fileList.length;

        imageViewText.setVisibility(View.GONE);

        isFromExtra = this.getIntent().getBooleanExtra("isFromExtra", false);
        if (isFromExtra) {
            Intent service = new Intent(MarkPictureActivity.this, FloatWindowsService.class);
            stopService(service);

            dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.setMessage(res.getString(R.string.markPicture_ProgressDialog_msg));
            dialog.setTitle(res.getString(R.string.markPicture_ProgressDialog_title));
            dialog.setMax(fileList.length);
            dialog.show();
            dialog.setProgress(0);
            new Thread(new MyThread()).start();
        }

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
        imageUndo.setVisibility(View.GONE);


        flag=0;
        imagview.setOnTouchListener(new View.OnTouchListener() {
            double  mPosX,mPosY,mCurPosX,mCurPosY,offsetX,offsetY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (flag == 0) {
                    //Log.i("el", "第一次调用：pic_no="+pic_no);
                    set_image(pic_no);
                    flag++;
                    btn_start.setVisibility(View.VISIBLE);
                    imageUndo.setVisibility(View.VISIBLE);
                    imagview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    return false;
                }
                else {
                    //Log.i("el", "非第一次调用：pic_no="+pic_no);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mPosX = event.getX();
                            mPosY = event.getY();
                            /*checkIsLongPress(false);
                            Log.i("test", "in ACTION_DOWN");  */
                            //withdrawStep(mPosX, mPosY);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mCurPosX = event.getX();
                            mCurPosY = event.getY();
                            offsetX = mPosX - mCurPosX;
                            offsetY = mPosY - mCurPosY;
                            /*if (Math.abs(offsetX)<400 && Math.abs(offsetY)<400) {
                                checkIsLongPress(true);
                            }*/
                            if (isMoveText) {
                                int moveX = //(int)(-offsetX+imageViewText.getPaddingLeft());
                                        (int)mCurPosX;
                                int moveY = //(int)(offsetY+imageViewText.getPaddingTop());
                                        (int)mCurPosY;
                                int bgRealSize[] = tool.getImageRealSize(imagview);
                                bgRealWidth =  bgRealSize[0];
                                bgRealHeight =  bgRealSize[1];
                                relativeX = moveX-(imagview.getWidth()-bgRealWidth)/2;
                                relativeY = moveY-(imagview.getHeight()-bgRealHeight)/2;
                                Log.i("EL", relativeX+" "+relativeY);
                                Bitmap bmTemp = getBitmapFromFile(pic_no+"");
                                Log.i("EL", moveX+" "+bmTemp.getWidth()+" "+moveY+" "+bmTemp.getHeight());
                                if (relativeY<bgRealHeight && relativeY>0) {
                                    Log.i("EL", "call");
                                    imageViewText.setPadding(moveX
                                            ,moveY
                                            ,imageViewText.getPaddingRight()
                                            ,imageViewText.getPaddingBottom());
                                }
                                /*if (imageViewText.getPaddingTop()<imagview.getHeight()) {
                                    imageViewText.setPadding(imageViewText.getPaddingTop()
                                            ,moveY
                                            ,imageViewText.getPaddingRight()
                                            ,imageViewText.getPaddingBottom());
                                }
                                if (imageViewText.getPaddingLeft()<imagview.getWidth()) {
                                    imageViewText.setPadding(moveX
                                            ,imageViewText.getPaddingLeft()
                                            ,imageViewText.getPaddingRight()
                                            ,imageViewText.getPaddingBottom());
                                }*/

                                //Log.i("EL", imageViewText.getPaddingLeft()+" "+imageViewText.getPaddingTop() + " " + offsetX);
                                break;
                            }
                            break;
                        case MotionEvent.ACTION_SCROLL:

                        case MotionEvent.ACTION_UP:
                            if (pic_no >= pic_num) {
                                Toast.makeText(getApplicationContext(),R.string.markPicture_toast_markFinish, Toast.LENGTH_SHORT).show();
                                break;
                            }

                            if (isMoveText) {
                                break;
                            }

                            offsetX = mPosX - mCurPosX;
                            offsetY = mPosY - mCurPosY;
                            if (Math.abs(offsetY) >= Math.abs(offsetX) ) {
                                Log.i("pic num", pic_num+"");
                                Log.i("pic no", pic_no+"");
                                if (mCurPosY - mPosY > 0
                                        && (Math.abs(mCurPosY - mPosY) > 200)) {
                                    //向下滑动
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
                                    if (!fileList[pic_no].equals("text")) {
                                        fileList[pic_no] = "all";
                                    }
                                    pic_no++;
                                }
                            }
                            else {
                                if (mCurPosX - mPosX < 0
                                        && (Math.abs(mCurPosX - mPosX) > 200)) {
                                    //向左滑动
                                    slideToLeft();
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
                if (isMoveText) {
                    btn_start.setText("开始合成");
                    Bitmap bitmap, bitmapBg;
                    if (pic_no<pic_num && fileList[pic_no].equals("text")) {
                        bitmapBg = getBitmapFromFile(pic_no+"_t");
                    }
                    else {
                        bitmapBg = getBitmapFromFile(pic_no+"");
                    }

                    if (relativeY+TextImgTemp.getHeight()>bitmapBg.getHeight()) {
                        if (bg_color == Color.argb(0, 255, 255, 255)) {
                            bg_color = Color.WHITE;     //如果添加到图片底部，禁止设置背景为透明
                        }
                        bitmap = addBitmap(bitmapBg,addTextToImage(bitmapBg,addTextString,addTextStringSize));
                        bg_color = Color.argb(0, 255, 255, 255);
                    }
                    else {
                        bitmap = tool.jointTextImage(TextImgTemp,bitmapBg,relativeX,relativeY);
                    }
                    try {
                        saveMyBitmap(bitmap,pic_no+"_t");
                    }
                    catch (IOException e) {
                        Toast.makeText(getApplicationContext(),"写入缓存失败！"+e.toString(), Toast.LENGTH_LONG).show();
                    }
                    set_image(pic_no, "_t");
                    fileList[pic_no] = "text";
                    imageViewText.setVisibility(View.GONE);
                    isMoveText = false;
                    return;
                }
                if (pic_no <= 0) {
                    Toast.makeText(getApplicationContext(),"至少需要选择一张图片", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent intent = new Intent(MarkPictureActivity.this, BuildPictureActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArray("fileList",fileList);
                    bundle.putBoolean("isFromExtra", isFromExtra);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });

        imageUndo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pic_no>0 || fileList[pic_no].equals("text")) {
                    handler.sendEmptyMessage(HandlerStatusLongIsWorking);
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
            Message msg;
            msg = Message.obtain();
            msg.obj = e;
            msg.what = HandlerStatusGetImgFail;
            handler.sendMessage(msg);
        }

        return bm;
    }

    private Bitmap addTextToImage(Bitmap bm, String text, int size) {
        if (size < 0) {
            size = 30;
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
        paint.setColor(bg_color);
        canvas.drawRect(0, 0, result.getWidth(), result.getHeight(), paint);

        StaticLayout layout = new StaticLayout(text,textPaint,canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL,1.0F,0.0F,true);
        canvas.translate(5,0);
        layout.draw(canvas);
        return result;
    }

    private Bitmap text2Image(Bitmap bm, String text, int size) {
        if (size < 0) {
            size = 30;
        }
        int width = bm.getWidth();

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(text_color);
        textPaint.setTextSize(size);

        Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt();

        int char_height = fmi.bottom-fmi.top;
        int char_width = (int)textPaint.measureText(text,0,text.length());
        if (char_width < width) {
            width = char_width+10;
        }
        Log.i("EL", "width="+width+" char_width="+char_height);

        Log.i("text", char_height+" "+fmi.bottom +" " +fmi.top);

        String[] len = text.split("\n");
        int t_height=0;
        for (int i=0;i<len.length;i++) {
            t_height+=char_height;
            int string_width = (int)textPaint.measureText(len[i]);
            if (string_width > width) {
                t_height+=char_height*(string_width/width);
                Log.i(TAG, "line: "+ string_width/width);
            }
        }

        Log.i(TAG, "finally width="+width+" height="+t_height);
        Bitmap result = Bitmap.createBitmap(10,10, Bitmap.Config.ARGB_8888);
        try {
            result = Bitmap.createBitmap(width,t_height, Bitmap.Config.ARGB_8888);
        }
        catch (Exception e) {
            Toast.makeText(this, "创建文字Bitmap出错："+e.toString(), Toast.LENGTH_LONG).show();
            Log.e("EL", "创建文字Bitmap出错："+e.toString());
            CrashReport.postCatchedException(e);
        }
        catch (OutOfMemoryError e) {
            Toast.makeText(this, "创建文字Bitmap出错："+e.toString(), Toast.LENGTH_LONG).show();
            Log.e("EL", "创建文字Bitmap出错："+e.toString());
            CrashReport.postCatchedException(e);
        }
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColor(bg_color);
        canvas.drawRect(0, 0, result.getWidth(), result.getHeight(), paint);

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

    /*long callWithdrawTime = 0;
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
    }   */


    private class MyThread implements Runnable {
        @Override
        public void run() {
            Message msg;
            int i = 0;
            File path = new File(getExternalCacheDir().toString());
            String extension = settings.getBoolean("isShotToJpg", true) ? "jpg":"png";
            String [] files = path.list();
            String name;
            Bitmap bitmap;

            for (String file:files) {
                msg = Message.obtain();
                msg.obj = String.format(res.getString(R.string.markPicture_ProgressDialog_msgOnRunning),
                        ""+(i+1));
                msg.what = HandlerStatusProgressRunning;
                handler.sendMessage(msg);

                if (file.contains("_")) {
                    name = file.split("_")[0];
                }
                else {
                    name = file.split("\\.")[0];
                }
                Log.i("cao", "file= "+file);
                Log.i("cao", "name= "+name);
                tool.AllowCheckBlackLines = Integer.valueOf(settings.getString("AllowCheckBlackLines", "10"));
                tool.AllowNotBlackNums = Integer.valueOf(settings.getString("AllowNotBlackNums", "20"));
                if (settings.getBoolean("isCheckBlankLines", true)) {
                    bitmap = tool.removeImgBlackSide(getBitmapFromFile(file.split("\\.")[0]));
                }
                else {
                    bitmap = getBitmapFromFile(file.split("\\.")[0]);
                }
                try {
                    if (!saveMyBitmap(bitmap, name)) {
                        msg = Message.obtain();
                        msg.obj = "保存文件失败！";
                        msg.what = HandlerCheckImgSaveFail;
                        handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    msg = Message.obtain();
                    msg.obj = e;
                    msg.what = HandlerCheckImgSaveFail;
                    handler.sendMessage(msg);
                }
                i++;
            }
            handler.sendEmptyMessage(HandlerStatusProgressDone);
        }
    }

    private void clickAddTextOkBtn() {
        btn_start.setText("确定");
        isMoveText = true;
        imageViewText.setVisibility(View.VISIBLE);

        EditText edit_text = (EditText) view.findViewById(R.id.input_text);
        EditText edit_size = (EditText) view.findViewById(R.id.input_size);
        addTextString = edit_text.getText().toString();
        if (addTextString.equals("")) {
            addTextString = "NULL";
        }
        if (edit_size.getText().toString().equals("")) {
            addTextStringSize = 30;
        }
        else {
            addTextStringSize = Integer.parseInt(edit_size.getText().toString());
        }

        //Log.i("ccccc",text);
        TextImgTemp = text2Image(getBitmapFromFile(pic_no+""),addTextString,addTextStringSize);
        imageViewText.setImageBitmap(TextImgTemp);


        /*Bitmap bm;
        if (pic_no<pic_num && fileList[pic_no].equals("text")) {
            bm = addBitmap(getBitmapFromFile(pic_no+"_t"),addTextToImage(getBitmapFromFile(pic_no+"_t"),text,text_size));
        }
        else {
            bm = addBitmap(getBitmapFromFile(pic_no+""),addTextToImage(getBitmapFromFile(pic_no+""),text,text_size));
        }
        try {
            saveMyBitmap(bm,pic_no+"_t");
        }
        catch (IOException e) {
            Log.i("excuse me?",e.toString());
            Toast.makeText(getApplicationContext(),"写入缓存失败！"+e.toString(), Toast.LENGTH_LONG).show();
        }
        set_image(pic_no, "_t");
        fileList[pic_no] = "text";  */
    }

    private void slideToLeft() {
        if (sp_init.getBoolean("isFirstUseAddText", true)) {
            showAddTextTipDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseAddText", false);
            editor.apply();
        }
        else {
            nums_tip_text.setText((pic_no+1+"/")+pic_num);
            DialogInterface.OnClickListener dialogOnclickListener=new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch(which){
                        case Dialog.BUTTON_POSITIVE:
                            clickAddTextOkBtn();
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
                    .setPositiveButton("确定",dialogOnclickListener)
                    .setNegativeButton("取消", dialogOnclickListener)
                    .setCancelable(false)
                    .create();
            builder.show();
            start_color_picker = (Button) view.findViewById(R.id.mark_dialog_chooseColor_btn);
            start_color_picker_bg = (Button) view.findViewById(R.id.mark_dialog_chooseColorBg_btn);
            start_color_picker_bg.setBackgroundColor(bg_color);
            start_color_picker.setBackgroundColor(text_color);
            start_color_picker.setTextColor(tool.getInverseColor(text_color));
            start_color_picker_bg.setTextColor(tool.getInverseColor(bg_color));
            start_color_picker.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ColorPickerDialog mColorPickerDialog = new ColorPickerDialog(
                            MarkPictureActivity.this,
                            text_color,
                            false,
                            mOnColorPickerListener
                    ).show();
                }
            });
            start_color_picker_bg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ColorPickerDialog mColorPickerDialog = new ColorPickerDialog(
                            MarkPictureActivity.this,
                            bg_color,
                            false,
                            mOnColorPickerBgListener
                    ).show();
                }
            });
        }
    }

    private OnColorPickerListener mOnColorPickerListener = new OnColorPickerListener() {
        @Override
        public void onColorCancel(ColorPickerDialog dialog) {//取消选择的颜色

        }

        @Override
        public void onColorChange(ColorPickerDialog dialog, int color) {//实时监听颜色变化

        }

        @Override
        public void onColorConfirm(ColorPickerDialog dialog, int color) {//确定的颜色
            text_color = color;
            start_color_picker.setBackgroundColor(color);
            start_color_picker.setTextColor(tool.getInverseColor(color));
        }
    };

    private OnColorPickerListener mOnColorPickerBgListener = new OnColorPickerListener() {
        @Override
        public void onColorCancel(ColorPickerDialog dialog) {//取消选择的颜色

        }

        @Override
        public void onColorChange(ColorPickerDialog dialog, int color) {//实时监听颜色变化

        }

        @Override
        public void onColorConfirm(ColorPickerDialog dialog, int color) {//确定的颜色
            bg_color = color;
            start_color_picker_bg.setBackgroundColor(color);
            start_color_picker_bg.setTextColor(tool.getInverseColor(color));
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            dialog.dismiss();
        } catch (NullPointerException e){}
    }


    private static class MyHandler extends Handler {
        private final WeakReference<MarkPictureActivity> mActivity;

        private MyHandler(MarkPictureActivity activity) {
            mActivity = new WeakReference<MarkPictureActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MarkPictureActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerStatusHideTipText:
                        activity.tip_text.setVisibility(View.GONE);
                        break;
                    case HandlerStatusLongIsWorking:
                        activity.tip_text.setText("撤销");
                        activity.tip_text.setVisibility(View.VISIBLE);
                        activity.autoHideText();
                        if (activity.pic_no<activity.pic_num && activity.fileList[activity.pic_no].equals("text")) {
                            activity.set_image(activity.pic_no);
                            activity.fileList[activity.pic_no] = "del";
                        }
                        else if (activity.pic_no>0 && activity.pic_no<activity.pic_num && activity.fileList[activity.pic_no-1].equals("text")) {
                            activity.nums_tip_text.setText((activity.pic_no+"/")+activity.pic_num);
                            activity.set_image(activity.pic_no-1, "_t");
                            activity.fileList[activity.pic_no] = "del";
                            activity.pic_no--;
                        }
                        else {
                            activity.nums_tip_text.setText((activity.pic_no+"/")+activity.pic_num);
                            activity.set_image(activity.pic_no-1);
                            activity.fileList[activity.pic_no-1] = "del";
                            activity.pic_no--;
                        }
                        break;
                    case HandlerStatusIsLongPress:
                        activity.isLongPress = false;
                        break;
                    case HandlerCheckImgSaveFail:
                        Toast.makeText(activity, "保存图片失败："+msg.obj, Toast.LENGTH_SHORT).show();
                        break;
                    case HandlerStatusProgressRunning:
                        activity.dialog.setProgress(activity.dialog.getProgress()+1);
                        activity.dialog.setMessage(msg.obj.toString());
                        break;
                    case HandlerStatusProgressDone:
                        activity.tool.MakeCacheToStandard(activity);
                        activity.dialog.dismiss();
                        break;
                    case HandlerStatusGetImgFail:
                        Toast.makeText(activity, "获取图片失败："+msg.obj, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    private void showAddTextTipDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.markPicture_tip_dialog_tittle)
                .setMessage(R.string.markPicture_tip_dialog_content)
                .setPositiveButton(res.getString(R.string.markPicture_tip_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

}
