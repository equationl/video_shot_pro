package com.equationl.videoshotpro.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.equationl.videoshotpro.R;
import com.github.ielse.imagewatcher.ImageWatcher;

public class GlideSimpleLoader implements ImageWatcher.Loader {
    @Override
    public void load(Context context, Uri uri, final ImageWatcher.LoadCallback lc) {
        Log.i("el,in GlideSimpleLoader", "call load");
        RequestOptions options = new RequestOptions().placeholder(R.mipmap.gallery_pick_photo)
                .skipMemoryCache(true).diskCacheStrategy( DiskCacheStrategy.NONE );   //禁用磁盘缓存，否则多次使用时预览图片会出错
        Glide.with(context).load(uri.toString()).apply(options).into(new SimpleTarget<Drawable>() {   //不知道为什么直接用URI加载会 load fail
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                lc.onResourceReady(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                lc.onLoadFailed(errorDrawable);
            }

            @Override
            public void onLoadStarted(@Nullable Drawable placeholder) {
                lc.onLoadStarted(placeholder);
            }
        });
    }
}