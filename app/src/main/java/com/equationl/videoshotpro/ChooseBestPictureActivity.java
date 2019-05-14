package com.equationl.videoshotpro;

import android.content.DialogInterface;
import android.content.Intent;
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
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.equationl.videoshotpro.Adapter.ChooseBestAdapter;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.GlideSimpleLoader;
import com.github.ielse.imagewatcher.ImageWatcherHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChooseBestPictureActivity extends AppCompatActivity {
    String filePath;
    List<String> imgData = new ArrayList<>();
    List<Uri> imgDataUri = new ArrayList<>();
    Tools tool;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    private ChooseBestAdapter mAdapter;
    ImageWatcherHelper vImageWatcher;

    private static final String TAG = "EL,In CBPA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_best_picture);

        initLayout();
        initRecyclerView();
        initImageWatcher();
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

        String fileNames[] = tool.getFileOrderByName(filePath, 1);
        for (String name : fileNames) {
            imgData.add(filePath+name);
            imgDataUri.add(Uri.parse(filePath+name));
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
                ImageView imageView = view.findViewById(R.id.chooseBest_item_imageView);
                SparseArray<ImageView> imageGroupList = new SparseArray<>();
                imageGroupList.put(position, imageView);
                vImageWatcher.show(imageView, imageGroupList, imgDataUri);
            }

            @Override
            public void onItemLongClick(View view, int position) {}
        });

        mRecyclerView.setAdapter(mAdapter);
    }

    private void initImageWatcher() {
        vImageWatcher = ImageWatcherHelper.with(this, new GlideSimpleLoader())
                .setTranslucentStatus(0)
                .setErrorImageRes(R.mipmap.error_picture);
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
            }
            exitActivity();
        }
    }

    private void exitActivity() {
        PlayerForDataActivity.instance.finish();
        finish();
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
                finish();
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
            if (!vImageWatcher.handleBackPressed()) {    //没有打开预览图片
                finish();
                return true;
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
