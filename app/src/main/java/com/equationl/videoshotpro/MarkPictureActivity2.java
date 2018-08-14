package com.equationl.videoshotpro;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.equationl.videoshotpro.Adapter.markPictureAdapter;
import com.equationl.videoshotpro.Image.Tools;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.huxq17.swipecardsview.SwipeCardsView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarkPictureActivity2 extends AppCompatActivity {
    SwipeCardsView swipeCardsView;
    markPictureAdapter adapter;
    FloatingActionMenu fab;
    List <String> pictureList =  new ArrayList<>();
    int curIndex;
    Tools tool;


    private static final String TAG = "EL,In MarkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_picture2);

        tool = new Tools();

        String filepath = getExternalCacheDir().toString();
        String[] fileList = tool.getFileOrderByName(filepath);
        for (String s: fileList) {
            s = filepath+"/"+s;
            Log.i(TAG, "S= "+s);
            pictureList.add(s);
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
                Toast.makeText(this, "确定", Toast.LENGTH_SHORT).show();
                break;
            case R.id.markPicture_menu_guide:
                Toast.makeText(this, "帮助", Toast.LENGTH_SHORT).show();
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
        //必须先改变adapter中的数据，然后才能由数据变化带动页面刷新
        if (pictureList != null) {
            adapter.setData(pictureList);
            if (curIndex > 0) {
                swipeCardsView.notifyDatasetChanged(curIndex - 1);
            }else{
                //toast("已经是第一张卡片了");
            }
        }
    }

    private void initCardView() {
        swipeCardsView = (SwipeCardsView) findViewById(R.id.markPictureSwipCardsView);
        //保留最后一张卡片，具体请看[#9](https://github.com/huxq17/SwipeCardsView/issues/9)
        swipeCardsView.retainLastCard(true);
        //Pass false if you want to disable swipe feature,or do nothing.
        //swipeCardsView.enableSwipe(false);
        //设置滑动监听
        swipeCardsView.setCardsSlideListener(new SwipeCardsView.CardsSlideListener() {
            @Override
            public void onShow(int index) {
                curIndex = index;
                Log.i(TAG, "onShow "+index);
            }

            @Override
            public void onCardVanish(int index, SwipeCardsView.SlideType type) {
                //String orientation = "";
                switch (type){
                    case LEFT:
                        //orientation="向左飞出";
                        Toast.makeText(MarkPictureActivity2.this, "左边飞出", Toast.LENGTH_SHORT).show();
                        break;
                    case RIGHT:
                        //orientation="向右飞出";
                        Toast.makeText(MarkPictureActivity2.this, "右边飞出", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onItemClick(View cardImageView, int index) {
                Toast.makeText(MarkPictureActivity2.this, "点击"+index, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
