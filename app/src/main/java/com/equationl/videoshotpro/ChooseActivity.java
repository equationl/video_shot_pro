package com.equationl.videoshotpro;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.dingmouren.colorpicker.ColorPickerDialog;
import com.equationl.videoshotpro.Image.Tools;
import com.huxq17.handygridview.HandyGridView;
import com.huxq17.handygridview.listener.OnItemCapturedListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChooseActivity extends AppCompatActivity {

    HandyGridView gridView;
    List<Bitmap> images = new ArrayList<>();
    List<String> imagePaths = new ArrayList<>();
    String[] files;
    ProgressDialog dialog;
    Resources res;
    Tools tool = new Tools();
    LayoutInflater mLayoutInflater;
    View view;
    AlertDialog.Builder builder;
    ChoosePictureAdapter pictureAdapter;
    SharedPreferences sp_init;

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


        String filepath = getExternalCacheDir().toString();
        files = tool.getFileOrderByName(filepath);

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

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showPictureDialog(position);
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

    }


    private class LoadImageThread implements Runnable {
        @Override
        public void run() {
            String path = getExternalCacheDir().toString();
            for (int i = 0; i < files.length; i++) {
                Bitmap bitmap = tool.getBitmapThumbnailFromFile(path+"/"+files[i], 128, 160);
                if (bitmap == null) {
                    bitmap = tool.drawableToBitmap(R.drawable.load_image_fail, ChooseActivity.this);
                }
                images.add(bitmap);
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
                        activity.pictureAdapter = new ChoosePictureAdapter(activity.images, activity.files, activity);
                        activity.gridView.setAdapter(activity.pictureAdapter);
                        activity.gridView.setMode(HandyGridView.MODE.LONG_PRESS);
                        activity.gridView.setAutoOptimize(false);
                        activity.gridView.setScrollSpeed(750);
                        activity.dialog.dismiss();
                        if (activity.sp_init.getBoolean("isFirstUseSortPicture", true)) {
                            activity.showGuideDialog();
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.choosePicture_menu_done:
                imagePaths = pictureAdapter.getImagePaths();
                tool.sortCachePicture(imagePaths, this);
                Intent intent = new Intent(ChooseActivity.this, MarkPictureActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPictureDialog(int position) {
        imagePaths = pictureAdapter.getImagePaths();
        //Log.i(TAG, imagePaths.toString());
        String path = getExternalCacheDir().toString();
        Bitmap bitmap = tool.getBitmapFromFile(path+"/"+imagePaths.get(position));
        if (bitmap == null) {
            bitmap = tool.drawableToBitmap(R.drawable.load_image_fail, ChooseActivity.this);
        }
        DialogInterface.OnClickListener dialogOnclicListener=new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case Dialog.BUTTON_POSITIVE:
                        break;
                }
            }
        };
        mLayoutInflater= LayoutInflater.from(ChooseActivity.this);
        view=mLayoutInflater.inflate(R.layout.dialog_show_picture, null, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.showPicture_image);
        imageView.setImageBitmap(bitmap);
        builder = new AlertDialog.Builder(ChooseActivity.this);
        builder.setView(view)
                .setPositiveButton("确定",dialogOnclicListener)
                .setCancelable(true)
                .create();
        builder.show();
    }

    private void showGuideDialog() {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setTitle(R.string.choosePicture_tip_dialog_title)
                .setMessage(R.string.choosePicture_tip_dialog_content)
                .setPositiveButton(res.getString(R.string.choosePicture_tip_dialog_btn_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

}
