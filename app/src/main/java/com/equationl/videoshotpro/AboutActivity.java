package com.equationl.videoshotpro;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.Utils;
import com.huxq17.swipecardsview.SwipeCardsView;
import com.tencent.bugly.beta.Beta;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class AboutActivity extends AppCompatActivity {
    TextView text_update, text_history, text_support, text_thanks;
    ImageView img_logo;
    Tools tool;
    Resources res;
    SharedPreferences sp_init;
    Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        android.support.v7.widget.Toolbar toolbar = (Toolbar) findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        text_update   = findViewById(R.id.about_text_version);
        text_history  = findViewById(R.id.about_text_history);
        text_support  = findViewById(R.id.about_text_support);
        text_thanks   = findViewById(R.id.about_text_thanks);
        img_logo      = findViewById(R.id.about_img_logo);

        text_update.setOnClickListener(clickListener);
        text_history.setOnClickListener(clickListener);
        text_support.setOnClickListener(clickListener);
        text_thanks.setOnClickListener(clickListener);
        img_logo.setOnClickListener(clickListener);

        tool = new Tools();
        utils = new Utils();
        res = getResources();
        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        text_update.setText(tool.getVerName(this)+"（"+tool.getVersionCode(this)+"）");

        showGuide();
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.about_text_version:
                    Toast.makeText(AboutActivity.this, R.string.about_toast_checkUpdate, Toast.LENGTH_SHORT).show();
                    Beta.checkUpgrade();
                    break;
                case R.id.about_text_history:
                    showTextDialog(R.string.about_dialog_title_history, R.string.about_updateHistory_text);
                    break;
                case R.id.about_text_support:
                    clickSupport();
                    break;
                case R.id.about_text_thanks :
                    showTextDialog(R.string.about_text_thanks, R.string.about_right_text);
                    break;
                case R.id.about_img_logo:
                    goToMarkPlay();
                    break;
            }
        }
    };

    private void showGuide() {
        final FancyShowCaseView fancyShowCaseView0 = new FancyShowCaseView.Builder(this)
                .focusOn(img_logo)
                .title(res.getString(R.string.about_guide_text_logo))
                .showOnce("about_logo")
                .build();
        final FancyShowCaseView fancyShowCaseView1 = new FancyShowCaseView.Builder(this)
                .focusOn(text_update)
                .title(res.getString(R.string.about_guide_text_update))
                .showOnce("about_update")
                .build();
        final FancyShowCaseView fancyShowCaseView2 = new FancyShowCaseView.Builder(this)
                .focusOn(text_history)
                .title(res.getString(R.string.about_guide_text_history))
                .showOnce("about_history")
                .build();
        final FancyShowCaseView fancyShowCaseView3 = new FancyShowCaseView.Builder(this)
                .focusOn(text_support)
                .title(res.getString(R.string.about_guide_text_support))
                .titleStyle(R.style.GuideViewTextTop,  Gravity.START)
                .showOnce("about_support")
                .build();
        final FancyShowCaseView fancyShowCaseView4 = new FancyShowCaseView.Builder(this)
                .focusOn(text_thanks)
                .title(res.getString(R.string.about_guide_text_thanks))
                .showOnce("about_thanks")
                .build();

        FancyShowCaseQueue mQueue = new FancyShowCaseQueue()
                .add(fancyShowCaseView0)
                .add(fancyShowCaseView1)
                .add(fancyShowCaseView2)
                .add(fancyShowCaseView3)
                .add(fancyShowCaseView4);
        mQueue.show();
    }

    private void showTextDialog(int title, int content) {
        Dialog dialog = new AlertDialog.Builder(this).setCancelable(true)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(res.getString(R.string.main_dialog_btn_ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
        dialog.show();
    }

    private void goToMarkPlay() {
        utils.goToMarkPlay(this);
    }

    private void clickSupport() {
        showTextDialog(R.string.about_dialog_title_support, R.string.about_dialog_text_support);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

}
