package com.equationl.videoshotpro;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class BuildPictureActivity extends AppCompatActivity {
    Button btn_up, btn_down, btn_done;
    ImageView imageTest;
    String[] fileList;
    Canvas canvas;
    Paint paint;
    Bitmap bm_test;
    float startY, stopY;
    int bWidth,bHeight;
    ProgressDialog dialog;
    int isDone=0;
    File savePath=null;
    SharedPreferences settings;
    Tools tool = new Tools();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_picture);

        btn_up    = (Button)   findViewById(R.id.button_up);
        btn_down  = (Button)    findViewById(R.id.button_down);
        btn_done  = (Button)    findViewById(R.id.button_final_done);
        imageTest = (ImageView) findViewById(R.id.imageTest);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle bundle = this.getIntent().getExtras();
        fileList = bundle.getStringArray("fileList");

        //Log.i("filelist", fileList.toString());

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);// 设置样式
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setMessage("正在处理...");
        dialog.setTitle("请稍等");
        dialog.setMax(fileList.length+1);

        Toast.makeText(getApplicationContext(),"请调整剪切字幕的位置", Toast.LENGTH_LONG).show();

        bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);

        canvas = new Canvas(bm_test);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth((float) 5);
        bHeight = bm_test.getHeight();
        bWidth = bm_test.getWidth();
        startY = (float) (bHeight*0.8);
        stopY = startY;
        canvas.drawLine(0,startY,bWidth,stopY,paint);
        imageTest.setImageBitmap(bm_test);

        btn_up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isDone == 0) {
                    bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);
                    canvas = new Canvas(bm_test);
                    startY = startY-8;
                    if (startY < 0) {
                        startY = 0;
                    }
                    stopY = startY;
                    canvas.drawLine(0,startY,bm_test.getWidth(),stopY,paint);
                    imageTest.setImageBitmap(bm_test);
                }
                else {
                    PlayerActivity.instance.finish();
                    MarkPictureActivity.instance.finish();
                    MainActivity.instance.finish();
                    Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        btn_down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isDone == 0) {
                    bm_test = getCutImg().copy(Bitmap.Config.ARGB_8888,true);
                    canvas = new Canvas(bm_test);
                    startY = startY+8;
                    if (startY > bHeight) {
                        startY = bHeight;
                    }
                    stopY = startY;
                    canvas.drawLine(0,startY,bm_test.getWidth(),stopY,paint);
                    imageTest.setImageBitmap(bm_test);
                }
            }
        });

        btn_done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isDone==1) {
                    Uri imageUri = Uri.fromFile(savePath);
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    shareIntent.setType("image/*");
                    startActivity(Intent.createChooser(shareIntent, "分享到"));
                }
                else {
                    new Thread(new MyThread()).start();
                    dialog.show();
                    dialog.setProgress(0);
                }
            }
        });

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (isDone == 1) {
                PlayerActivity.instance.finish();
                MarkPictureActivity.instance.finish();
                MainActivity.instance.finish();
                Intent intent = new Intent(BuildPictureActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            return false;
        }else {
            return super.onKeyDown(keyCode, event);
        }

    }

    private Bitmap getBitmap(String no) {
        /*File path = new File(getExternalCacheDir(), no+".png");
        FileInputStream f;
        Bitmap bm = null;
        try {
            f = new FileInputStream(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            BufferedInputStream bis = new BufferedInputStream(f);
            bm = BitmapFactory.decodeStream(bis, null, options);
        } catch (FileNotFoundException e) {
            Toast.makeText(getApplicationContext(),"获取截图失败"+e,Toast.LENGTH_LONG).show();
        }

        return bm;  */
        Bitmap bm = null;
        String extension;
        if (settings.getBoolean("isShotToJpg", true)) {
            extension = "jpg";
        }
        else {
            extension = "png";
        }

        try {
            bm = tool.getBitmapFromFile(no, getExternalCacheDir(),extension);
        }  catch (Exception e) {
            Toast.makeText(getApplicationContext(),"获取截图失败"+e, Toast.LENGTH_LONG).show();
        }

        return bm;
    }

    private Bitmap getCutImg() {
        for (int i=0;i<fileList.length;i++) {
            if (fileList[i].equals("cut")) {
                return getBitmap(i+"");
            }
        }
        return getBitmap(0+"");
    }

    private Handler handler = new Handler() {
        // 在Handler中获取消息，重写handleMessage()方法
        @Override
        public void handleMessage(Message msg) {
            // 判断消息码是否为1
            if (msg.what == 1) {
                dialog.setProgress(dialog.getProgress()+1);
                dialog.setMessage(msg.obj.toString());
            }
            else if (msg.what == 2) {
                dialog.dismiss();
                btn_up.setText("返回");
                btn_done.setText("分享");
                btn_down.setVisibility(View.INVISIBLE);
                isDone=1;
                Toast.makeText(getApplicationContext(),"处理完成！图片已保存至 "+ Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES)+"/"+msg.obj.toString()+" 请进入图库查看", Toast.LENGTH_LONG).show();
            }
            else if (msg.what == 3) {
                Boolean isShow = settings.getBoolean("isMonitoredShow", false);
                if (isShow) {
                    imageTest.setImageBitmap((Bitmap) msg.obj);
                }
                else {
                    DisplayMetrics dm = new DisplayMetrics();
                    dm = getResources().getDisplayMetrics();
                    int screenHeight = dm.heightPixels;

                    Bitmap bm = (Bitmap) msg.obj;
                    if (bm.getHeight() > screenHeight) {
                        Bitmap newbm = Bitmap.createBitmap(bm, 0, bm.getHeight()-screenHeight, bm.getWidth(), screenHeight);
                        imageTest.setImageBitmap(newbm);
                    }
                    else {
                        imageTest.setImageBitmap(bm);
                    }
                }
            }
            else if (msg.what == 4) {
                Toast.makeText(getApplicationContext(),msg.obj.toString(), Toast.LENGTH_LONG).show();
                dialog.dismiss();
                isDone = 1;
                btn_up.setText("退出");
                btn_down.setVisibility(View.GONE);
                btn_done.setVisibility(View.GONE);
            }
        }
    };

    public class MyThread implements Runnable {
        int delete_nums=0;
        @Override
        public void run() {
            Message msg;
            int len = fileList.length;
            Bitmap final_bitmap = Bitmap.createBitmap(bWidth,1, Bitmap.Config.ARGB_8888);
            for (int i=0;i<len;i++) {
                msg = Message.obtain();
                msg.obj = "处理第"+i+"张图片";
                msg.what = 1;
                handler.sendMessage(msg);
               /* Log.i("test,soure file width:",getBitmap(i+"").getWidth()+"");
                Log.i("test,soure file height:",getBitmap(i+"").getHeight()+"");
                Log.i("test,cut file width:",cutBimap(getBitmap(i+"")).getWidth()+"");
                Log.i("test,cut file height:",cutBimap(getBitmap(i+"")).getHeight()+"");
                Log.i("test,final file width:",final_bitmap.getWidth()+"");
                Log.i("test,final file height:",final_bitmap.getHeight()+"");  */
                if (fileList[i].equals("cut")) {
                    final_bitmap = addBitmap(final_bitmap,cutBimap(getBitmap(i+"")));
                    msg = Message.obtain();
                    msg.obj = final_bitmap;
                    msg.what = 3;
                    handler.sendMessage(msg);
                }
                else if (fileList[i].equals("all")) {
                    final_bitmap = addBitmap(final_bitmap,getBitmap(i+""));
                    msg = Message.obtain();
                    msg.obj = final_bitmap;
                    msg.what = 3;
                    handler.sendMessage(msg);
                }
                else if (fileList[i].equals("text")) {
                    final_bitmap = addBitmap(final_bitmap,getBitmap(i+"_t"));
                    msg = Message.obtain();
                    msg.obj = final_bitmap;
                    msg.what = 3;
                    handler.sendMessage(msg);
                }
                else {
                    delete_nums++;
                }
            }
            Boolean isAddWatermark = settings.getBoolean("isAddWatermark_switch",false);
            if (isAddWatermark) {
                Canvas canvas = new Canvas(final_bitmap);
                String watermark = settings.getString("watermark_text","NULL");
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.argb(80,150,150,150));
                textPaint.setTextSize(40);

                Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt();
                int char_height = fmi.bottom-fmi.top;

                int watermarkPosition = Integer.parseInt(settings.getString("watermark_position_value", "1"));
                Layout.Alignment align = Layout.Alignment.ALIGN_NORMAL;
                switch (watermarkPosition) {
                    case 1:
                        break;
                    case 2:
                        canvas.translate(0,final_bitmap.getHeight()/2);
                        align = Layout.Alignment.ALIGN_CENTER;
                        break;
                    case 3:
                        canvas.translate(0, final_bitmap.getHeight()-char_height);
                        break;
                }

                StaticLayout layout = new StaticLayout(watermark,textPaint,canvas.getWidth(), align,1.0F,0.0F,true);
                canvas.translate(5,0);
                layout.draw(canvas);
            }

            if (delete_nums >= len) {
                msg = Message.obtain();
                msg.obj = "你全部删除了我合成什么啊？？？";
                msg.what = 4;
                handler.sendMessage(msg);
            }

            else {
                msg = Message.obtain();
                msg.obj = "导出图片";
                msg.what = 1;
                handler.sendMessage(msg);
                SimpleDateFormat sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
                String date    =    sDateFormat.format(new    java.util.Date());
                try {
                    if(saveMyBitmap(final_bitmap,date+"-by_EL", settings.getBoolean("isReduce_switch", false))) {
                        msg = Message.obtain();
                        msg.obj = date+"-by_EL";
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    //Toast.makeText(getApplicationContext(),"保存截图失败"+e,Toast.LENGTH_LONG).show();
                    msg = Message.obtain();
                    msg.obj = "保存截图失败"+e;
                    msg.what = 4;
                    handler.sendMessage(msg);
                }
            }
        }
    }

    private Bitmap cutBimap(Bitmap bm) {
        //return Bitmap.createBitmap(bm, 0, (int)startY, bWidth, (int)(bm.getHeight()-startY));
        return tool.cutBimap(bm, (int)startY, bWidth);
    }

    private Bitmap addBitmap(Bitmap first, Bitmap second) {
        /*int width = Math.max(first.getWidth(),second.getWidth());
        int height = first.getHeight() + second.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(first, 0, 0, null);
        canvas.drawBitmap(second, 0, first.getHeight(), null);
        return result;*/
        return tool.jointBitmap(first, second);
    }

    private boolean saveMyBitmap(Bitmap bmp, String bitName, boolean isReduce) throws IOException {
       /* File f = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES),bitName + ".png");

        Log.i("cao",f.toString());
        int quality = 100;
        Bitmap.CompressFormat imgFormat = Bitmap.CompressFormat.PNG;
        if (isReduce) {
            quality = Integer.parseInt(settings.getString("reduce_value","100"));
            imgFormat = Bitmap.CompressFormat.JPEG;
            Log.i("test","is Reduced, quality="+quality);
        }
        savePath = f;
        boolean flag = false;
        f.createNewFile();
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
            bmp.compress(imgFormat, quality, fOut);
            flag = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;  */


        boolean flag;
        try {
            if (isReduce) {
                int quality = Integer.parseInt(settings.getString("reduce_value","100"));
                savePath = tool.saveBitmap2png(bmp,bitName, Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), true, quality);
            }
            else {
                savePath = tool.saveBitmap2png(bmp,bitName, Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES));
            }
            flag = true;
        } catch (Exception e) {
            flag = false;
        }

        return flag;
    }
}
