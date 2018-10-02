package com.equationl.videoshotpro;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.equationl.videoshotpro.Image.Tools;
import com.huxq17.swipecardsview.SwipeCardsView;
import com.tencent.bugly.beta.Beta;

import me.toptas.fancyshowcase.FancyShowCaseQueue;
import me.toptas.fancyshowcase.FancyShowCaseView;

public class AboutActivity extends AppCompatActivity {
    TextView text_update, text_history, text_support, text_thanks, text_adStatus, text_adBtn, text_donation;
    ImageView img_logo;
    Tools tool;
    Resources res;
    Boolean isShowSupport = false;
    SharedPreferences sp_init;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        text_update   = findViewById(R.id.about_text_version);
        text_history  = findViewById(R.id.about_text_history);
        text_support  = findViewById(R.id.about_text_support);
        text_thanks   = findViewById(R.id.about_text_thanks);
        img_logo      = findViewById(R.id.about_img_logo);
        text_donation = findViewById(R.id.about_text_donation);
        text_adBtn    = findViewById(R.id.about_text_adBtn);
        text_adStatus = findViewById(R.id.about_text_adStatus);

        text_update.setOnClickListener(clickListener);
        text_history.setOnClickListener(clickListener);
        text_support.setOnClickListener(clickListener);
        text_thanks.setOnClickListener(clickListener);
        img_logo.setOnClickListener(clickListener);
        text_donation.setOnClickListener(clickListener);
        text_adBtn.setOnClickListener(clickListener);

        tool = new Tools();
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
                case R.id.about_text_adBtn:
                    clickAdBtn();
                    break;
                case R.id.about_text_donation:
                    showTextDialog(R.string.about_text_donation, R.string.about_dialog_text_donation);
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
        try{
            Uri uri = Uri.parse("market://details?id="+getPackageName());
            Intent intent = new Intent(Intent.ACTION_VIEW,uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }catch(Exception e){
            Toast.makeText(AboutActivity.this, R.string.about_toast_goToMarkPlay_fail, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void clickSupport() {
        if (!isShowSupport) {
            text_adStatus.setVisibility(View.VISIBLE);
            text_adBtn.setVisibility(View.VISIBLE);
            text_donation.setVisibility(View.VISIBLE);

            String adStatus = sp_init.getBoolean("isCloseAd", false) ? "关闭" : "打开";
            String adBtnStatus = sp_init.getBoolean("isCloseAd", false) ? "打开" : "关闭";
            text_adStatus.setText(String.format(res.getString(R.string.about_text_adStatus), adStatus));
            text_adBtn.setText(adBtnStatus);
            isShowSupport = true;
        }
        else {
            text_adStatus.setVisibility(View.GONE);
            text_adBtn.setVisibility(View.GONE);
            text_donation.setVisibility(View.GONE);
            isShowSupport = false;
        }
    }

    private void clickAdBtn() {
        SharedPreferences.Editor editor = sp_init.edit();
        Boolean isCloseAd = sp_init.getBoolean("isCloseAd", false);
        if (isCloseAd) {
            text_adBtn.setText("关闭");
            text_adStatus.setText(String.format(res.getString(R.string.about_text_adStatus), "开启"));
            editor.putBoolean("isCloseAd", false);
        }
        else {
            text_adBtn.setText("开启");
            text_adStatus.setText(String.format(res.getString(R.string.about_text_adStatus), "关闭"));
            editor.putBoolean("isCloseAd", true);
        }

        editor.apply();
    }

}
