package com.equationl.videoshotpro;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.equationl.videoshotpro.Adapter.ChooseBestAdapter;
import com.equationl.videoshotpro.Image.Tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.glide.ImageLoader;

public class ChooseBestPictureActivity extends AppCompatActivity {
    String filePath;
    List<String> imgData = new ArrayList<>();
    List<Uri> imgDataUri = new ArrayList<>();
    Tools tool;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    private ChooseBestAdapter mAdapter;

    private static final String TAG = "EL,In CBPA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_best_picture);

        initLayout();
        initRecyclerView();
    }

    private void initLayout() {
        tool = new Tools();
        Toolbar toolbar = findViewById(R.id.chooseBest_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initRecyclerView() {
        Intent intent = getIntent();
        filePath = intent.getStringExtra("filePath");

        final List<String> imageList = new ArrayList<>();

        String[] fileNames = tool.getFileOrderByName(filePath, 1);
        for (String name : fileNames) {
            imgData.add(filePath+name);
            imgDataUri.add(Uri.parse(filePath+name));
            imageList.add(filePath+name);
        }

        mRecyclerView = findViewById(R.id.chooseBest_rv);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;

        mLayoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        mAdapter = new ChooseBestAdapter(this, imgData, screenWidth);

        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter.setOnItemClickListener(new ChooseBestAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                /*ImageView imageView = view.findViewById(R.id.chooseBest_item_imageView);
                SparseArray<ImageView> imageGroupList = new SparseArray<>();
                imageGroupList.put(position, imageView);
                vImageWatcher.show(imageView, imageGroupList, imgDataUri);   */
                ImageLoader.cleanDiskCache(getApplicationContext());
                ImagePreview.getInstance()
                        .setContext(ChooseBestPictureActivity.this)
                        .setEnableDragClose(true)
                        .setShowDownButton(false)
                        .setIndex(position)
                        .setImageList(imageList)
                        .start();
            }

            @Override
            public void onItemLongClick(View view, int position) {}
        });

        mRecyclerView.setAdapter(mAdapter);
    }

    private void clickSaveMenu() {
        SparseBooleanArray checkStates = mAdapter.getCheckStates();
        int itemCount = mAdapter.getItemCount();
        boolean isAllDelete = checkStates.indexOfValue(true) == -1;   //如果没有已经选中的数据则为全部删除
        if (isAllDelete) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.chooseBest_dialog_deleteAll_content)
                    .setPositiveButton(R.string.chooseBest_dialog_deleteAll_btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                tool.deleteDirectory(new File(filePath));
                            } catch (IOException e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                            exitActivity();
                        }
                    })
                    .setNegativeButton(R.string.chooseBest_dialog_deleteAll_btn_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { }
                    }).setCancelable(false).show();
        }
        else {
            for (int i=0; i<itemCount; i++) {
                if (!checkStates.get(i, false)) {
                    try {
                        tool.deleteFile(new File(imgData.get(i)));
                    } catch (IOException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
                else {
                    MediaScannerConnection.scanFile(this, new String[]{imgData.get(i)}, null, null);
                }
            }
            exitActivity();
        }
    }

    private void exitActivity() {
        PlayerForDataActivity.instance.finish();
        finish();
    }

    private void clickExit() {
        final Dialog dialog = new AlertDialog.Builder(this).
                setCancelable(true)
                .setMessage(R.string.chooseBest_dialog_exit_content)
                .setPositiveButton(R.string.chooseBest_dialog_exit_btn_save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] imgs = imgData.toArray(new String[0]);
                                MediaScannerConnection.scanFile(ChooseBestPictureActivity.this, imgs, null, null);
                                exitActivity();
                            }
                        })
                .setNegativeButton(R.string.chooseBest_dialog_exit_btn_delect,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    tool.deleteDirectory(new File(filePath));
                                } catch (IOException e) {
                                    Log.e(TAG, Log.getStackTraceString(e));
                                }
                                exitActivity();
                            }
                        })
                .setNeutralButton(R.string.chooseBest_dialog_exit_btn_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                .create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_choose_best, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                clickExit();
                break;
            case R.id.chooseBest_menu_save:
                clickSaveMenu();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            clickExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
