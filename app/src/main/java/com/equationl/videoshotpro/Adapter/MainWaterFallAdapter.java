package com.equationl.videoshotpro.Adapter;


import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.R;
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
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        MyViewHolder holder2 = (MyViewHolder) holder;
        WaterFallData waterFallData= mData.get(position);
        //holder2.img.setImageBitmap(waterFallData.img);
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.gallery_pick_photo)
                .centerCrop();
        if (waterFallData.img == null) {
            Glide.with(mContext)
                    .load(R.mipmap.error_picture)
                    .apply(options)
                    .thumbnail(0.1f)
                    .into(holder2.img);
        }
        else {
            Glide.with(mContext)
                    .load(waterFallData.img)
                    .apply(options)
                    .thumbnail(0.1f)
                    .into(holder2.img);
        }
        holder2.img.getLayoutParams().height = waterFallData.imgHeight; //从数据源中获取图片高度，动态设置到控件上
        holder2.text.setText(waterFallData.text);
        if (waterFallData.isSelected) {
            holder2.cardViewLinearLayout.setBackgroundColor(Color.parseColor("#c5cae9"));
        }
        else {
            holder2.cardViewLinearLayout.setBackgroundColor(Color.WHITE);
        }

        if (mOnItemClickListener != null) {
            holder2.cardViewLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(holder);
                }
            });
            holder2.cardViewLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickListener.onItemLongClick(holder);
                    return false;
                }
            });
        }
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
        public LinearLayout cardViewLinearLayout;

        public MyViewHolder(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.main_recyclerView_img);
            text = (TextView) itemView.findViewById(R.id.main_recyclerView_text);
            cardViewLinearLayout = itemView.findViewById(R.id.main_cardView_linearLayout);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.ViewHolder holder);

        void onItemLongClick(RecyclerView.ViewHolder holder);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }
}
