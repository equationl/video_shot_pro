package com.equationl.videoshotpro.Adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.equationl.videoshotpro.R;

import java.util.List;

public class ChooseBestAdapter extends RecyclerView.Adapter{

    private Context mContext;
    private List<String> mData;
    private int screenWidth;
    private SparseBooleanArray mCheckStates = new SparseBooleanArray();

    public ChooseBestAdapter(Context context, List<String> data, int screenWidth) {
        this.mContext = context;
        this.mData = data;
        this.screenWidth = screenWidth;
    }

    public SparseBooleanArray getCheckStates() {
        return mCheckStates;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder holder = new MyViewHolder(LayoutInflater.from(mContext).inflate(R.layout.choose_best_recyclerview_item, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder holder2 = (MyViewHolder) holder;
        String imgPath = mData.get(position);
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.gallery_pick_photo)
                .centerCrop();
        if (imgPath.equals("")) {
            Glide.with(mContext)
                    .load(R.mipmap.error_picture)
                    .apply(options)
                    .thumbnail(0.1f)
                    .into(holder2.imageView);
        }
        else {
            Glide.with(mContext)
                    .load(imgPath)
                    .apply(options)
                    .thumbnail(0.1f)
                    .into(holder2.imageView);
        }

        holder2.checkBox.setTag(position);
        holder2.relativeLayout.getLayoutParams().width = screenWidth / 3;
        holder2.relativeLayout.getLayoutParams().height = screenWidth / 3;

        holder2.checkBox.setChecked(mCheckStates.get(position, false));

        if (mOnItemClickListener != null) {
            holder2.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder2.getLayoutPosition();
                    mOnItemClickListener.onItemClick(holder2.itemView, pos);
                }
            });

            holder2.imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder2.getLayoutPosition();
                    mOnItemClickListener.onItemLongClick(holder2.itemView, pos);
                    return false;
                }
            });
        }

        holder2.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                int pos = (int) compoundButton.getTag();
                if (b) {
                    mCheckStates.put(pos, true);
                } else {
                    mCheckStates.delete(pos);
                }
            }
        });

        holder2.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*int pos = holder2.getLayoutPosition();
                holder2.checkBox.setChecked(true);  */
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public RelativeLayout relativeLayout;
        public CheckBox checkBox;

        public MyViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.chooseBest_item_imageView);
            relativeLayout = itemView.findViewById(R.id.chooseBest_item_relativeLayout);
            checkBox = itemView.findViewById(R.id.chooseBest_item_checkBox);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

}
