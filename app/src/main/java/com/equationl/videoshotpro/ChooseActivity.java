package com.equationl.videoshotpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.equationl.videoshotpro.Adapter.ChoosePictureAdapter;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.GlideSimpleLoader;
import com.github.ielse.imagewatcher.ImageWatcher;
import com.github.ielse.imagewatcher.ImageWatcherHelper;
import com.huxq17.handygridview.HandyGridView;
import com.huxq17.handygridview.listener.OnItemCapturedListener;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class ChooseActivity extends AppCompatActivity implements ChoosePictureAdapter.OnPictureDeleteListener{

    HandyGridView gridView;
    List<Bitmap> images = new ArrayList<>();
    List<String> imagePaths = new ArrayList<>();
    List<Uri> fullFiles = new ArrayList<>();
    String[] files;
    ProgressDialog dialog;
    Resources res;
    Tools tool = new Tools();
    View view;
    ChoosePictureAdapter pictureAdapter;
    SharedPreferences sp_init;
    Boolean isFromExtra;
    boolean isEditMode = false;

    ImageWatcherHelper vImageWatcher;
    ImageWatcher.OnPictureLongPressListener mOnPictureLongPressListener;


    private final MyHandler handler = new MyHandler(this);

    private final static String TAG = "EL,In ChooseActivity";

    private final static int HandlerStatusLoadImageNext = 1000;
    private final static int HandlerStatusLoadImageDone = 1001;

    public static ChooseActivity instance = null;   //FIXME  暂时这样吧，实在找不到更好的办法了

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        gridView=(HandyGridView) findViewById(R.id.choosePicture_handyGridView);

        res = getResources();

        instance = this;

        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        isFromExtra = this.getIntent().getBooleanExtra("isFromExtra", false);
        if (isFromExtra) {
            Intent service = new Intent(ChooseActivity.this, FloatWindowsService.class);
            stopService(service);
        }


        String filepath = getExternalCacheDir().toString();
        files = tool.getFileOrderByName(filepath, 1);

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setMessage(res.getString(R.string.chooseActivity_ProgressDialog_loadImage_content));
        dialog.setTitle(res.getString(R.string.chooseActivity_ProgressDialog_loadImage_title));
        dialog.setMax(files.length);
        dialog.show();
        dialog.setProgress(0);
        new Thread(new LoadImageThread()).start();

        initPictureWatcher();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showPictureDialog(position);
                //ChoosePictureAdapter.ViewHolder viewHolder = (ChoosePictureAdapter.ViewHolder) pictureAdapter.getView(position, view, parent).getTag();
                ImageView imageview = (ImageView) pictureAdapter.getView(position, view, parent);
                SparseArray<ImageView> imageGroupList = new SparseArray<>();
                imageGroupList.put(position, imageview);
                imagePaths = pictureAdapter.getImagePaths();

                new FancyShowCaseView.Builder(ChooseActivity.this)
                        .focusOn(imageview)
                        .title(res.getString(R.string.choosePicture_guideView_clickImage))
                        .showOnce("choose_clickImage")
                        .titleStyle(R.style.GuideViewTextBlank, Gravity.CENTER)
                        .build()
                        .show();
                fullFiles = pictureAdapter.getImagesUri();
                vImageWatcher.show(imageview, imageGroupList, fullFiles);
            }
        });

        gridView.setOnItemCapturedListener(new OnItemCapturedListener() {
            @Override
            public void onItemCaptured(View v, int position) {
                v.setScaleX(1.2f);
                v.setScaleY(1.2f);
            }

            @Override
            public void onItemReleased(View v, int position) {
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        });


        //showGuideDialog();
    }


    //删除图片回调
    @Override
    public void onDelete(int position) {}


    private class LoadImageThread implements Runnable {
        @Override
        public void run() {
            String path = getExternalCacheDir().toString();
            for (int i = 0; i < files.length; i++) {
                String file = path+"/"+files[i];
                Bitmap bitmap = tool.getBitmapThumbnailFromFile(file, 128, 160);
                if (bitmap == null) {
                    bitmap = tool.drawableToBitmap(R.mipmap.error_picture, ChooseActivity.this);
                }
                images.add(bitmap);
                fullFiles.add(Uri.parse(file));
                handler.sendEmptyMessage(HandlerStatusLoadImageNext);
            }
            handler.sendEmptyMessage(HandlerStatusLoadImageDone);
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ChooseActivity> mActivity;

        private MyHandler(ChooseActivity activity) {
            mActivity = new WeakReference<ChooseActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ChooseActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case HandlerStatusLoadImageNext:
                        activity.dialog.setProgress(activity.dialog.getProgress()+1);
                        break;
                    case HandlerStatusLoadImageDone:
                        activity.pictureAdapter = new ChoosePictureAdapter(activity.images, activity.files, activity.fullFiles, activity);
                        activity.pictureAdapter.setOnPictureDeleteListener(activity);
                        activity.gridView.setAdapter(activity.pictureAdapter);
                        activity.gridView.setMode(HandyGridView.MODE.LONG_PRESS);
                        activity.gridView.setAutoOptimize(false);
                        activity.gridView.setScrollSpeed(750);
                        try {    //避免因为非正常关闭Activity导致闪退
                            activity.dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                            CrashReport.postCatchedException(e);
                        }
                        if (activity.sp_init.getBoolean("isFirstUseSortPicture", true)) {
                            SharedPreferences.Editor editor = activity.sp_init.edit();
                            editor.putBoolean("isFirstUseSortPicture", false);
                            editor.apply();
                        }
                        break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_choose_picture, menu);
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        final FancyShowCaseView fancyShowCaseView1 = new FancyShowCaseView.Builder(ChooseActivity.this)
                                .focusOn(findViewById(R.id.choosePicture_menu_edit))
                                .title(res.getString(R.string.choosePicture_guideView_edit))
                                .showOnce("choose_edit")
                                .build();
                        final FancyShowCaseView fancyShowCaseView2 = new FancyShowCaseView.Builder(ChooseActivity.this)
                                .title(res.getString(R.string.choosePicture_guideView_editSummary))
                                .showOnce("choose_editSummary")
                                .build();
                        final FancyShowCaseView fancyShowCaseView3 = new FancyShowCaseView.Builder(ChooseActivity.this)
                                .focusOn(findViewById(R.id.choosePicture_menu_done))
                                .title(res.getString(R.string.choosePicture_guideView_done))
                                .showOnce("choose_done")
                                .build();
                        FancyShowCaseQueue mQueue = new FancyShowCaseQueue()
                                .add(fancyShowCaseView1)
                                .add(fancyShowCaseView2)
                                .add(fancyShowCaseView3);

                        mQueue.show();
                    }
                }, 50
        );
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isEditMode) {
            menu.findItem(R.id.choosePicture_menu_edit).setIcon(
                    R.drawable.square_edit_outline_blank);
        } else {
            menu.findItem(R.id.choosePicture_menu_edit).setIcon(R.drawable.square_edit_outline);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.choosePicture_menu_done:
                imagePaths = pictureAdapter.getImagePaths();
                tool.sortCachePicture(imagePaths, this);
                Intent intent = new Intent(ChooseActivity.this, MarkPictureActivity.class);
                if (isFromExtra) {
                    intent.putExtra("isFromExtra", true);
                }
                startActivity(intent);
                break;
            case R.id.choosePicture_menu_edit:
                if (pictureAdapter.inEditMode) {
                    isEditMode = false;
                    pictureAdapter.setInEditMode(false);
                }
                else {
                    isEditMode = true;
                    pictureAdapter.setInEditMode(true);
                }
                invalidateOptionsMenu();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPictureWatcher() {
        mOnPictureLongPressListener = new ImageWatcher.OnPictureLongPressListener() {
            @Override
            public void onPictureLongPress(ImageView v, final Uri url, final int pos) {
                //Toast.makeText(MainActivity.this, "call long press:"+url, Toast.LENGTH_SHORT).show();
                String[] items;
                //Log.i(TAG, "in longPress path= "+new File(url.toString()).getParent()+" RootPath= "+tool.getSaveRootPath());
                items = new String[] {"保存"};
                AlertDialog.Builder builder = new AlertDialog.Builder(ChooseActivity.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
                                String date    =    sDateFormat.format(new    java.util.Date());
                                date += "-by_EL."+url.toString().substring(url.toString().lastIndexOf(".") + 1);
                                String savePath =  tool.getSaveRootPath() + "/" + date;
                                try {
                                    tool.copyFile(new File(url.toString()), new File(savePath));
                                    MediaScannerConnection.scanFile(ChooseActivity.this, new String[]{savePath}, null, null);
                                    Toast.makeText(ChooseActivity.this, R.string.choosePicture_toast_saveSuccess, Toast.LENGTH_SHORT).show();
                                } catch (IOException e) {
                                    Toast.makeText(ChooseActivity.this, R.string.choosePicture_toast_saveFail, Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }
                    }
                });
                builder.create();
                builder.show();
            }
        };

        vImageWatcher = ImageWatcherHelper.with(this, new GlideSimpleLoader())
                .setTranslucentStatus(0)
                .setErrorImageRes(R.mipmap.error_picture)
                .setOnPictureLongPressListener(mOnPictureLongPressListener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!vImageWatcher.handleBackPressed()) {    //没有打开预览图片
                finish();
                return true;
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

}
