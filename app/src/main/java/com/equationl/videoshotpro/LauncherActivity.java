package com.equationl.videoshotpro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class LauncherActivity extends AppCompatActivity {
    public static LauncherActivity instance = null;    //FIXME  暂时这样吧，实在找不到更好的办法了

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        instance = this;

        SharedPreferences sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        if (sp_init.getBoolean("isCloseSplashAd", false) || sp_init.getBoolean("isFirstBoot", true)) {
            startActivity(new Intent(LauncherActivity.this, MainActivity.class));
        }
        else {
            startActivity(new Intent(LauncherActivity.this, SplashActivity.class));
        }
    }
}
