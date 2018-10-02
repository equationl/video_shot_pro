package com.equationl.videoshotpro;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dingmouren.colorpicker.ColorPickerDialog;
import com.dingmouren.colorpicker.OnColorPickerListener;
import com.equationl.videoshotpro.Adapter.markPictureAdapter;
import com.equationl.videoshotpro.Image.Tools;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.huxq17.swipecardsview.SwipeCardsView;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class MarkPictureActivity2 extends AppCompatActivity {
    SwipeCardsView swipeCardsView;
    markPictureAdapter adapter;
    List <String> pictureList =  new ArrayList<>();
    int curIndex;
    Tools tool;
    String[] fileList;
    int pic_num, pic_no=0;
    Boolean isFromExtra;
    Resources res;
    ProgressDialog dialog;
    SharedPreferences settings, sp_init;
    View dialogView;
    LayoutInflater mLayoutInflater;
    int text_color= Color.BLACK, bg_color = Color.argb(0, 255, 255, 255);
    boolean isMoveText=false;
    AlertDialog.Builder builder;
    Button start_color_picker, start_color_picker_bg;
    String addTextString;
    int addTextStringSize;
    Bitmap TextImgTemp=null;
    boolean isLongPress=false;


    FloatingActionButton fab_undo, fab_delete, fab_addText;
    FloatingActionsMenu fab_menu;


    private final MyHandler handler = new MyHandler(this);


    private static final int HandlerStatusHideTipText = 10010;
    private static final int HandlerStatusUnDO = 10011;
    private static final int HandlerStatusIsLongPress = 10012;
    private static final int HandlerCheckImgSaveFail = 10013;
    private static final int HandlerStatusProgressRunning = 10014;
    private static final int HandlerStatusProgressDone = 10015;
    private static final int HandlerStatusGetImgFail = 10016;

    public static MarkPictureActivity2 instance = null;   //FIXME  暂时这样吧，实在找不到更好的办法了
    private static final String TAG = "EL,In MarkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_picture2);

        instance = this;

        tool = new Tools();

        fab_undo    = findViewById(R.id.markPicture_fab_undo);
        fab_delete  = findViewById(R.id.markPicture_fab_delete);
        fab_addText = findViewById(R.id.markPicture_fab_addText);
        fab_menu    = findViewById(R.id.markPicture_fab_menu);

        fab_undo .setOnClickListener(clickListener);
        fab_delete.setOnClickListener(clickListener);
        fab_addText.setOnClickListener(clickListener);

        String filepath = getExternalCacheDir().toString();
        fileList = tool.getFileOrderByName(filepath);
        for (String s: fileList) {
            s = filepath+"/"+s;
            Log.i(TAG, "S= "+s);
            pictureList.add(s);
        }
        pic_num = fileList.length;

        res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        checkOrigin();

        for (int i=0;i<pic_num;i++) {
            fileList[i] = "del";
        }

        initCardView();
        showCardView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_mark_picture, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.markPicture_menu_done:
                //Toast.makeText(this, "确定", Toast.LENGTH_SHORT).show();
                if (pic_no <= 0) {
                    Toast.makeText(getApplicationContext(),"至少需要选择一张图片", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent intent = new Intent(MarkPictureActivity2.this, BuildPictureActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArray("fileList",fileList);
                    bundle.putBoolean("isFromExtra", isFromExtra);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
                break;
            case R.id.markPicture_menu_guide:
                //Toast.makeText(this, "帮助", Toast.LENGTH_SHORT).show();
                showGuide();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showCardView() {
        if (adapter == null) {
            adapter = new markPictureAdapter(pictureList, this);
            swipeCardsView.setAdapter(adapter);
        } else {
            //if you want to change the UI of SwipeCardsView,you must modify the data first
            adapter.setData(pictureList);
            swipeCardsView.notifyDatasetChanged(curIndex);
        }
    }

    private void swipeToPre() {
        if (pictureList != null) {
            adapter.setData(pictureList);
            if (pic_no > 0) {
                swipeCardsView.notifyDatasetChanged(pic_no - 1);    //此处使用 pos_no 代替 curIndex ，因为使用 cureIndex 时，在最后一张图片也标记后，curIndex的值会不正常
            }else{
                //已经是第一张卡片
            }
        }
    }

    private void deleteImg() {
        if (pic_no < pic_num) {
            if (pictureList != null) {
                pic_no++;
                adapter.setData(pictureList);
                swipeCardsView.notifyDatasetChanged(pic_no);
            }
        }
    }

    private void initCardView() {
        swipeCardsView = (SwipeCardsView) findViewById(R.id.markPictureSwipCardsView);
        //保留最后一张卡片，具体请看[#9](https://github.com/huxq17/SwipeCardsView/issues/9)
        swipeCardsView.retainLastCard(false);
        //Pass false if you want to disable swipe feature,or do nothing.
        //swipeCardsView.enableSwipe(false);
        //设置滑动监听
        swipeCardsView.setCardsSlideListener(new SwipeCardsView.CardsSlideListener() {
            @Override
            public void onShow(int index) {
                /*if (index >= pic_num-1) {
                    Toast.makeText(MarkPictureActivity2.this, R.string.markPicture_toast_markFinish, Toast.LENGTH_SHORT).show();
                    return;
                }*/
                curIndex = index;
                Log.i(TAG, "onShow "+index);
            }

            @Override
            public void onCardVanish(int index, SwipeCardsView.SlideType type) {
                //String orientation = "";
                //Toast.makeText(MarkPictureActivity2.this, "index="+index, Toast.LENGTH_SHORT).show();
                if (index >= pic_num-1) {
                    Toast.makeText(MarkPictureActivity2.this, R.string.markPicture_toast_markFinish, Toast.LENGTH_SHORT).show();
                }
                switch (type){
                    case LEFT:
                        //orientation="向左飞出";
                        //Toast.makeText(MarkPictureActivity2.this, "左边飞出", Toast.LENGTH_SHORT).show();
                        markPictureCut(index);
                        break;
                    case RIGHT:
                        //orientation="向右飞出";
                        //Toast.makeText(MarkPictureActivity2.this, "右边飞出", Toast.LENGTH_SHORT).show();
                        markPictureAll(index);
                        break;
                }
            }

            @Override
            public void onItemClick(View cardImageView, int index) {
                Toast.makeText(MarkPictureActivity2.this, "点击"+index, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.markPicture_fab_undo:
                    swipeToPre();   //更改UI
                    chooseUndo();  //处理逻辑
                    break;
                case R.id.markPicture_fab_delete:
                    deleteImg();
                    break;
                case R.id.markPicture_fab_addText:
                    Toast.makeText(MarkPictureActivity2.this, "该功能暂未开放，敬请期待！", Toast.LENGTH_SHORT).show();
                    //FIXME
                    //markPictureAddText();
                    break;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            try {
                ChooseActivity.instance.finish();
            } catch (NullPointerException e){}
            finish();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void checkOrigin() {
        isFromExtra = this.getIntent().getBooleanExtra("isFromExtra", false);
        if (isFromExtra) {
            Intent service = new Intent(MarkPictureActivity2.this, FloatWindowsService.class);
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
            new Thread(new CheckPictureThread()).start();
        }

        if (pic_num < 1) {
            Toast.makeText(this, R.string.markPicture_toast_readFile_fail, Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private class CheckPictureThread implements Runnable {
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


    private void markPictureCut(int pos) {
        if (fileList[pos].equals("text")) {
            Toast.makeText(MarkPictureActivity2.this, "添加文字后不允许裁切！", Toast.LENGTH_SHORT).show();
            return;
        }
        //tip_text.setText("裁切");
        //tip_text.setVisibility(View.VISIBLE);
        //autoHideText();
        //nums_tip_text.setText((pic_no+1+"/")+pic_num);
        //set_image(pic_no+1);
        fileList[pos] = "cut";
        pic_no++;
    }

    private void markPictureAll(int pos) {
        //tip_text.setText("全图");
        //tip_text.setVisibility(View.VISIBLE);
        //autoHideText();
        //nums_tip_text.setText((pic_no+1+"/")+pic_num);
        //set_image(pic_no+1);
        if (!fileList[pos].equals("text")) {
            fileList[pos] = "all";
        }
        pic_no++;
    }

    private void markPictureAddText() {
        if (sp_init.getBoolean("isFirstUseAddText", true)) {
            showAddTextTipDialog();
            SharedPreferences.Editor editor = sp_init.edit();
            editor.putBoolean("isFirstUseAddText", false);
            editor.apply();
        }
        else {
            //nums_tip_text.setText((pic_no+1+"/")+pic_num);
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
            mLayoutInflater= LayoutInflater.from(MarkPictureActivity2.this);
            dialogView=mLayoutInflater.inflate(R.layout.dialog_mark_picture, null, false);
            builder = new AlertDialog.Builder(MarkPictureActivity2.this);
            builder.setTitle("请输入要添加的文字")
                    .setView(dialogView)
                    .setPositiveButton("确定",dialogOnclickListener)
                    .setNegativeButton("取消", dialogOnclickListener)
                    .setCancelable(false)
                    .create();
            builder.show();
            start_color_picker = (Button) dialogView.findViewById(R.id.mark_dialog_chooseColor_btn);
            start_color_picker_bg = (Button) dialogView.findViewById(R.id.mark_dialog_chooseColorBg_btn);
            start_color_picker_bg.setBackgroundColor(bg_color);
            start_color_picker.setBackgroundColor(text_color);
            start_color_picker.setTextColor(tool.getInverseColor(text_color));
            start_color_picker_bg.setTextColor(tool.getInverseColor(bg_color));
            start_color_picker.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ColorPickerDialog mColorPickerDialog = new ColorPickerDialog(
                            MarkPictureActivity2.this,
                            text_color,
                            false,
                            mOnColorPickerListener
                    ).show();
                }
            });
            start_color_picker_bg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ColorPickerDialog mColorPickerDialog = new ColorPickerDialog(
                            MarkPictureActivity2.this,
                            bg_color,
                            false,
                            mOnColorPickerBgListener
                    ).show();
                }
            });
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

    private void clickAddTextOkBtn() {
        //btn_start.setText("确定");
        isMoveText = true;
        //imageViewText.setVisibility(View.VISIBLE);

        EditText edit_text = (EditText) dialogView.findViewById(R.id.input_text);
        EditText edit_size = (EditText) dialogView.findViewById(R.id.input_size);
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
        //imageViewText.setImageBitmap(TextImgTemp);
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


    private static class MyHandler extends Handler {
        private final WeakReference<MarkPictureActivity2> mActivity;

        private MyHandler(MarkPictureActivity2 activity) {
            mActivity = new WeakReference<MarkPictureActivity2>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MarkPictureActivity2 activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerStatusHideTipText:
                        //activity.tip_text.setVisibility(View.GONE);
                        break;
                    case HandlerStatusUnDO:
                        /*activity.tip_text.setText("撤销");
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
                        }   */
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

    private void chooseUndo() {
        if (pic_no<=0) {
            return;
        }

        if (pic_no<pic_num && fileList[pic_no].equals("text")) {
            fileList[pic_no] = "del";
        }
        else if (pic_no<pic_num && fileList[pic_no-1].equals("text")) {
          fileList[pic_no] = "del";
          pic_no--;
        }
        else if (pic_no <= pic_num){
            fileList[pic_no-1] = "del";
            pic_no--;
        }
    }

    private void showGuide() {
        fab_menu.collapseImmediately();
        fab_menu.expand();
        final FancyShowCaseView fancyShowCaseView0 = new FancyShowCaseView.Builder(this)
                .focusOn(fab_undo)
                .title(res.getString(R.string.markPicture_guide_moreTool))
                //.showOnce("choose_edit")
                .build();
        final FancyShowCaseView fancyShowCaseView1 = new FancyShowCaseView.Builder(this)
                .focusOn(fab_undo)
                .title(res.getString(R.string.markPicture_guide_undo))
                //.showOnce("choose_edit")
                .build();
        final FancyShowCaseView fancyShowCaseView2 = new FancyShowCaseView.Builder(this)
                .focusOn(fab_delete)
                .title(res.getString(R.string.markPicture_guide_delete))
                //.showOnce("choose_editSummary")
                .build();
        final FancyShowCaseView fancyShowCaseView3 = new FancyShowCaseView.Builder(this)
                .focusOn(fab_addText)
                .title(res.getString(R.string.markPicture_guide_addText))
                //.showOnce("choose_done")
                .build();
        final FancyShowCaseView fancyShowCaseView4 = new FancyShowCaseView.Builder(this)
                .titleStyle(R.style.GuideViewTextVertical,  Gravity.CENTER | Gravity.LEFT)
                .title(res.getString(R.string.markPicture_guide_swipeLeft))
                //.showOnce("choose_done")
                .build();
        final FancyShowCaseView fancyShowCaseView5 = new FancyShowCaseView.Builder(this)
                .titleStyle(R.style.GuideViewTextVertical,  Gravity.CENTER | Gravity.RIGHT)
                .title(res.getString(R.string.markPicture_guide_swipeRight))
                //.showOnce("choose_done")
                .build();
        final FancyShowCaseView fancyShowCaseView6 = new FancyShowCaseView.Builder(this)
                .title(res.getString(R.string.markPicture_guide_summary))
                //.showOnce("choose_done")
                .build();
        FancyShowCaseQueue mQueue = new FancyShowCaseQueue()
                .add(fancyShowCaseView0)
                .add(fancyShowCaseView1)
                .add(fancyShowCaseView2)
                .add(fancyShowCaseView3)
                .add(fancyShowCaseView6)
                .add(fancyShowCaseView4)
                .add(fancyShowCaseView5);
        mQueue.show();
    }
}
