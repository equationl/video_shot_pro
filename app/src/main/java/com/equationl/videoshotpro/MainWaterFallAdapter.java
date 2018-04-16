package com.equationl.videoshotpro;


import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.utils.WaterFallData;

import java.util.List;

public class MainWaterFallAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<WaterFallData> mData; //定义数据源
    private Tools tool;

    //定义构造方法，默认传入上下文和数据源
    public MainWaterFallAdapter(Context context, List<WaterFallData> data) {
        mContext = context;
        mData = data;
        tool = new Tools();
    }

    @Override  //将ItemView渲染进来，创建ViewHolder
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.main_recyclerview_item, null);
        return new MyViewHolder(view);
    }

    @Override  //将数据源的数据绑定到相应控件上
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder holder2 = (MyViewHolder) holder;
        WaterFallData waterFallData= mData.get(position);
        holder2.img.setImageBitmap(waterFallData.img);
        holder2.img.getLayoutParams().height = waterFallData.imgHeight; //从数据源中获取图片高度，动态设置到控件上
        holder2.text.setText(waterFallData.text);
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    //定义自己的ViewHolder，将View的控件引用在成员变量上
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView img;
        public TextView text;

        public MyViewHolder(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.main_recyclerView_img);
            text = (TextView) itemView.findViewById(R.id.main_recyclerView_text);
        }
    }
}
