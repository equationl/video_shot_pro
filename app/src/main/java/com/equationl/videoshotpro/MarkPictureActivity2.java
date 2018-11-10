package com.equationl.videoshotpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Adapter.markPictureAdapter;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.GlideSimpleLoader;
import com.equationl.videoshotpro.utils.Utils;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.ielse.imagewatcher.ImageWatcher;
import com.github.ielse.imagewatcher.ImageWatcherHelper;
import com.huxq17.swipecardsview.SwipeCardsView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.toptas.fancyshowcase.DismissListener;
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
    boolean isLongPress=false;
    TextView text_markStatus, text_markDoneTip;
    Utils utils = new Utils();


    FloatingActionButton fab_undo, fab_delete, fab_addText;
    FloatingActionsMenu fab_menu;

    ImageWatcherHelper vImageWatcher;
    ImageWatcher.OnPictureLongPressListener mOnPictureLongPressListener;

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

        text_markStatus = findViewById(R.id.mark_text_markStatus);
        text_markDoneTip = findViewById(R.id.mark_text_doneTip);

        fab_undo .setOnClickListener(clickListener);
        fab_delete.setOnClickListener(clickListener);
        fab_addText.setOnClickListener(clickListener);

        String filepath = getExternalCacheDir().toString();
        fileList = tool.getFileOrderByName(filepath, 1);
        for (String s: fileList) {
            s = filepath+"/"+s;
            Log.i(TAG, "S= "+s);
            pictureList.add(s);
        }
        pic_num = fileList.length;

        res = getResources();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        checkOrigin();

        for (int i=0;i<pic_num;i++) {
            fileList[i] = "del";
        }

        initCardView();
        showCardView();
        initPictureWathcher();

        text_markStatus.setText(String.format(res.getString(R.string.markPicture_text_markStatus), pic_no, pic_num));
        text_markDoneTip.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_mark_picture, menu);

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        final FancyShowCaseView fancyShowCaseView1 = new FancyShowCaseView.Builder(MarkPictureActivity2.this)
                                .focusOn(findViewById(R.id.markPicture_menu_guide))
                                .title(res.getString(R.string.markPicture_guide_guide))
                                .showOnce("mark_guide")
                                .build();
                        final FancyShowCaseView fancyShowCaseView2 = new FancyShowCaseView.Builder(MarkPictureActivity2.this)
                                .focusOn(findViewById(R.id.markPicture_menu_done))
                                .title(res.getString(R.string.markPicture_guide_done))
                                .showOnce("mark_done")
                                .dismissListener(new DismissListener() {
                                    @Override
                                    public void onDismiss(String id) {
                                        // FancyShowCaseView is dismissed by user
                                        showGuide();
                                    }

                                    @Override
                                    public void onSkipped(String id) {
                                        // Skipped because it was setup to shown only once and shown before
                                    }
                                })
                                .build();
                        FancyShowCaseQueue mQueue = new FancyShowCaseQueue()
                                .add(fancyShowCaseView1)
                                .add(fancyShowCaseView2);
                        mQueue.show();
                    }
                }, 50
        );

        return super.onCreateOptionsMenu(menu);
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
        text_markDoneTip.setVisibility(View.GONE);
    }

    private void deleteImg() {
            if (pic_no < pic_num) {
                if (pictureList != null) {
                    pic_no++;
                    adapter.setData(pictureList);
                    swipeCardsView.notifyDatasetChanged(pic_no);
                    text_markStatus.setText(String.format(res.getString(R.string.markPicture_text_markStatus), pic_no, pic_num));
                }
                if (pic_no >= pic_num) {
                    text_markDoneTip.setVisibility(View.VISIBLE);
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
                if (index >= pic_num-1) {
                    text_markDoneTip.setVisibility(View.VISIBLE);
                }
                else {
                    text_markDoneTip.setVisibility(View.GONE);
                }
                switch (type){
                    case LEFT:
                        //向左飞出
                        markPictureCut(index);
                        break;
                    case RIGHT:
                        //向右飞出
                        markPictureAll(index);
                        break;
                }
            }

            @Override
            public void onItemClick(View cardImageView, int index) {
                ImageView imageview = (ImageView) cardImageView.findViewById(R.id.markPictureImage);
                SparseArray<ImageView> imageGroupList = new SparseArray<>();
                imageGroupList.put(0, imageview);
                String path = getExternalCacheDir().toString();
                String extension = settings.getBoolean("isShotToJpg", true) ? "jpg":"png";
                String file = path+"/"+index+"."+extension;
                Log.i(TAG, file);
                vImageWatcher.show(imageview, imageGroupList, Collections.singletonList(Uri.parse(file)));
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
                    //TODO 添加文字
                    //markPictureAddText();
                    break;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (!vImageWatcher.handleBackPressed()) {    //没有打开预览图片
                utils.finishActivity(ChooseActivity.instance);
                finish();
                return true;
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
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
        fileList[pos] = "cut";
        pic_no++;
        text_markStatus.setText(String.format(res.getString(R.string.markPicture_text_markStatus), pic_no, pic_num));
    }

    private void markPictureAll(int pos) {
        if (!fileList[pos].equals("text")) {
            fileList[pos] = "all";
        }
        pic_no++;
        text_markStatus.setText(String.format(res.getString(R.string.markPicture_text_markStatus), pic_no, pic_num));
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
                        break;
                    case HandlerStatusUnDO:
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
            text_markStatus.setText(String.format(res.getString(R.string.markPicture_text_markStatus), pic_no, pic_num));
        }
        else if (pic_no <= pic_num){
            fileList[pic_no-1] = "del";
            pic_no--;
            text_markStatus.setText(String.format(res.getString(R.string.markPicture_text_markStatus), pic_no, pic_num));
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

    private void initPictureWathcher() {
        mOnPictureLongPressListener = new ImageWatcher.OnPictureLongPressListener() {
            @Override
            public void onPictureLongPress(ImageView v, final Uri url, final int pos) {
            }
        };

        vImageWatcher = ImageWatcherHelper.with(this, new GlideSimpleLoader())
                .setTranslucentStatus(0)
                .setErrorImageRes(R.mipmap.error_picture)
                .setOnPictureLongPressListener(mOnPictureLongPressListener);
    }
}
