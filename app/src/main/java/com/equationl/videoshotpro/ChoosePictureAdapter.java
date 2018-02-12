package com.equationl.videoshotpro;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.equationl.videoshotpro.Image.Tools;
import com.huxq17.handygridview.scrollrunner.OnItemMovedListener;

public class ChoosePictureAdapter extends BaseAdapter  implements OnItemMovedListener{
    private Context context;

    private List<Bitmap> pictures = new ArrayList<Bitmap>();

    private final static String TAG = "EL,In ChooseAdapter";

    private List<String> imagePaths = new ArrayList<>();


    public ChoosePictureAdapter( List<Bitmap> images, String[] files, Context context) {
        super();
        this.context = context;

        List list = Arrays.asList(files);
        imagePaths = new ArrayList(list);

        /*for (Bitmap image : images) {
            Picture picture = new Picture(image);
            pictures.add(picture);
        }  */

        pictures = images;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    @Override
    public int getCount() {

        if (null != pictures) {
            return pictures.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {

        return pictures.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder = null;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            // 获得容器
            convertView = LayoutInflater.from(this.context).inflate(R.layout.picture_item, null);

            // 初始化组件
            viewHolder.image = (ImageView) convertView.findViewById(R.id.picture_item);
            // 给converHolder附加一个对象
            convertView.setTag(viewHolder);
        } else {
            // 取得converHolder附加的对象
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // 给组件设置资源
        //Picture picture = pictures.get(position);
        //viewHolder.image.setImageResource(picture.getImageId());
        viewHolder.image.setImageBitmap(pictures.get(position));

        return convertView;
    }

    @Override
    public void onItemMoved(int from, int to) {
        Log.i(TAG, from+" "+to);
        /*String temp = imagePaths[to];
        imagePaths[to] = imagePaths[from];
        imagePaths[from] = temp;  */
        String s = imagePaths.remove(from);
        imagePaths.add(to, s);
        Bitmap b =  pictures.remove(from);
        pictures.add(to, b);
    }

    @Override
    public boolean isFixed(int position) {
        //When postion==0,the item can not be dragged.
        /*if (position == 0) {
            return true;
        }  */
        //Log.i(TAG, "fix: "+position);
        return false;
    }

    class ViewHolder {
        public ImageView image;
    }

   /* class Picture {

        private Bitmap image;

        public Picture(Bitmap image) {
            this.image = image;
        }

        public Bitmap getImage() {
            return image;
        }

    }*/
}