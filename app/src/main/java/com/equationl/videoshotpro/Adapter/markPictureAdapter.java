package com.equationl.videoshotpro.Adapter;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.R;
import com.huxq17.swipecardsview.BaseCardAdapter;

import java.util.List;

public class markPictureAdapter extends BaseCardAdapter{
    private List<String> datas;
    private Context context;
    private Tools tool;

    public markPictureAdapter(List<String> datas, Context context) {
        this.datas = datas;
        this.context = context;
        this.tool = new Tools();
    }

    public void setData(List<String> datas) {
        this.datas = datas;
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public int getCardLayoutId() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String extension = settings.getBoolean("isShotToJpg", true) ? "jpg":"png";
        float imgSize[] = tool.getBitmapSize("0", context.getExternalCacheDir(),extension);

        if (imgSize[0] > imgSize[1]) {
            return R.layout.mark_picture_card_item_horizontal;
        }
        else if (imgSize[0] < imgSize[1]) {
            return R.layout.mark_picture_card_item_vertical;
        }

        return R.layout.mark_picture_card_item_square;
    }

    @Override
    public void onBindData(int position, View cardview) {
        if (datas == null || datas.size() == 0) {
            return;
        }
        ImageView imageView = (ImageView) cardview.findViewById(R.id.markPictureImage);
        String data = datas.get(position);
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.gallery_pick_photo)
                .skipMemoryCache(true).diskCacheStrategy( DiskCacheStrategy.NONE );
        Glide.with(context)
                .load(data)
                .apply(options)
                .into(imageView);
    }
}
