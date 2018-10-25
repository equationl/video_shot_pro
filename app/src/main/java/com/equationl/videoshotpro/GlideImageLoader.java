package com.equationl.videoshotpro;


import android.app.Activity;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.yancy.gallerypick.inter.ImageLoader;
import com.yancy.gallerypick.widget.GalleryImageView;

public class GlideImageLoader implements ImageLoader {

    @Override
    public void displayImage(Activity activity, Context context, String path, GalleryImageView galleryImageView, int width, int height) {
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.gallery_pick_photo)
                .centerCrop();
        Glide.with(context)
                .load(path)
                .apply(options)
                .into(galleryImageView);
    }

    @Override
    public void clearMemoryCache() {

    }
}
