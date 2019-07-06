package com.equationl.videoshotpro;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.AlipayZeroSdk;
import com.equationl.videoshotpro.utils.Utils;
import com.huxq17.swipecardsview.SwipeCardsView;
import com.tencent.bugly.beta.Beta;

import java.io.IOException;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class AboutActivity extends AppCompatActivity {
    TextView text_update, text_history, text_support, text_thanks;
    ImageView img_logo;
    Tools tool;
    Resources res;
    SharedPreferences sp_init;
    Utils utils;

    private static final String TAG = "EL,In AboutActivity";

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
        final Dialog dialog = new AlertDialog.Builder(this).setCancelable(true)
                .setTitle(R.string.about_dialog_title_support)
                .setMessage(R.string.about_dialog_text_support)
                .setPositiveButton(R.string.about_dialog_support_btn_alipay,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (AlipayZeroSdk.hasInstalledAlipayClient(AboutActivity.this)) {
                                    AlipayZeroSdk.startAlipayClient(AboutActivity.this, "fkx07332ns1pb55do8sohec");
                                }
                                else {
                                    Toast.makeText(AboutActivity.this, R.string.about_toast_unstallAlipay, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                .setNegativeButton(R.string.about_dialog_support_btn_mm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (copyQRcodeImg("aip.licenses")) {
                                    gotoMmpay();
                                }
                                else {
                                    Toast.makeText(AboutActivity.this, R.string.about_toast_copyQRimg_fail, Toast.LENGTH_LONG).show();
                                }
                    }
                })
                .setNeutralButton(R.string.about_dialog_support_btn_cancel,
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean copyQRcodeImg(String name) {
        try {
            String savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .toString()+"/videoshot_"+"mmpay.jpg";
            utils.copyAssets2Local(this, name, savePath);
            MediaScannerConnection.scanFile(this, new String[]{savePath}, null, null);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "copyQRcodeImg: ", e);
            return false;
        }
    }

    private void gotoMmpay() {
        Intent intent = new Intent("com.tencent.mm.action.BIZSHORTCUT");
        intent.setPackage("com.tencent.mm");
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            startActivity(intent);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this, R.string.about_toast_gotoMmPay_fail, Toast.LENGTH_SHORT).show();
        }
    }

}
