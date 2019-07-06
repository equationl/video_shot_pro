package com.equationl.videoshotpro.utils;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.equationl.videoshotpro.Image.Tools;
import com.equationl.videoshotpro.R;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;

import java.io.File;

public class Share {
    public static void shareAppToQQ(Context context, IUiListener shareListener, Activity activity) {
        Resources res;
        res = context.getResources();
        Tencent mTencent = Tencent.createInstance("1106257597", context);
        final Bundle params = new Bundle();
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_APP);
        params.putString(QQShare.SHARE_TO_QQ_TITLE, res.getString(R.string.main_SHARE_TO_QQ_TITLE));
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, res.getString(R.string.main_SHARE_TO_QQ_SUMMARY));
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, res.getString(R.string.main_SHARE_TO_QQ_IMAGE_URL));
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, res.getString(R.string.main_SHARE_TO_QQ_APP_NAME));
        //params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
        mTencent.shareToQQ(activity, params, shareListener);
    }

    public static void shareAppToWx(Context context, int shareTo) {
        Resources res;
        res = context.getResources();
        IWXAPI wxApi = WXAPIFactory.createWXAPI(context, "wx45ceac6c6d2f1aff", true);
        wxApi.registerApp("wx45ceac6c6d2f1aff");

        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = "http://sj.qq.com/myapp/detail.htm?apkName=com.equationl.videoshotpro";//收到分享的好友点击会跳转到这个地址里面去
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = res.getString(R.string.main_SHARE_TO_QQ_TITLE);
        msg.description = res.getString(R.string.main_SHARE_TO_QQ_SUMMARY);
        try
        {
            Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.preview);
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 147, 237, true);
            bmp.recycle();
            msg.setThumbImage(thumbBmp);
        }
        catch (Exception e)
        {
            Toast.makeText(context, R.string.main_toast_createThumb_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = "shareAPP";
        req.message = msg;
        req.scene = shareTo;

        wxApi.sendReq(req);
    }

    public static void showShareAppDialog(final Context context, final IUiListener shareListener, final Activity activity) {
        Resources res;
        res = context.getResources();
        final String[] items = res.getStringArray(R.array.main_dialog_shareAPP_items);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle(R.string.buildPicture_dialog_share_title);
        alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int index) {
                switch (index) {
                    case 0:
                        //qq
                        shareAppToQQ(context, shareListener, activity);
                        break;
                    case 1:
                        //微信朋友圈
                        shareAppToWx(context, SendMessageToWX.Req.WXSceneTimeline);
                        break;
                    case 2:
                        //微信好友
                        shareAppToWx(context, SendMessageToWX.Req.WXSceneSession);
                        break;
                }
            }
        });
        alertBuilder.create().show();
    }

    public static void shareAppSuccess(Context context) {
        //SharedPreferences sp_init = context.getSharedPreferences("init", Context.MODE_PRIVATE);
        Toast.makeText(context, "分享成功！", Toast.LENGTH_SHORT).show();
        /*SharedPreferences.Editor editor = sp_init.edit();
        editor.putBoolean("isCloseAd", true);
        editor.apply();*/
    }


    public static void showSharePictureDialog(final Context context, final File savePath, final IUiListener shareListener, final Activity activity){
        final Resources res = context.getResources();
        final Tools tool = new Tools();
        final String[] items = res.getStringArray(R.array.buildPicture_dialog_share_items);
        final Tencent mTencent = Tencent.createInstance("1106257597", context);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle(R.string.buildPicture_dialog_share_title);
        alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int index) {
                final Bundle params = new Bundle();
                if (index == 0 || index == 1) {
                    params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, savePath.toString());
                    params.putString(QQShare.SHARE_TO_QQ_APP_NAME, res.getString(R.string.app_name));
                    params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
                }
                switch (index) {
                    case 0:
                        //qq好友
                        params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE);
                        mTencent.shareToQQ(activity, params, shareListener);
                        break;
                    case 1:
                        //qq空间
                        params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
                        mTencent.shareToQQ(activity, params, shareListener);
                        break;
                    case 2:
                        //微信好友
                        sharePictureToWX(SendMessageToWX.Req.WXSceneSession, context, savePath);
                        break;
                    case 3:
                        //微信朋友圈
                        sharePictureToWX(SendMessageToWX.Req.WXSceneTimeline, context, savePath);
                        break;
                    case 4:
                        //更多
                        Uri imageUri = //Uri.fromFile(savePath);
                                tool.getUriFromFile(savePath, context);
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        shareIntent.setType("image/*");
                        context.startActivity(Intent.createChooser(shareIntent, "分享到"));
                        break;
                }
            }
        });
        alertBuilder.create().show();
    }

    public static void sharePictureToWX(int shareTo, Context context, File savePath) {
        final Resources res = context.getResources();
        final Tools tool = new Tools();
        IWXAPI wxApi;
        wxApi = WXAPIFactory.createWXAPI(context, "wx45ceac6c6d2f1aff", true);
        wxApi.registerApp("wx45ceac6c6d2f1aff");

        WXImageObject imgObj = new WXImageObject();
        imgObj.setImagePath(savePath.toString());
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        try
        {
            Bitmap thumbBmp = Bitmap.createScaledBitmap(tool.getBitmapFromFile(savePath.toString()), 128, 160, true);
            msg.setThumbImage(thumbBmp);
        }
        catch (Exception e)
        {
            Toast.makeText(context, R.string.buildPicture_toast_createThumb_fail, Toast.LENGTH_SHORT).show();
            return;
        }

        msg.title = res.getString(R.string.main_SHARE_TO_QQ_TITLE);
        msg.description = res.getString(R.string.main_SHARE_TO_QQ_SUMMARY);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = "sharePicture";
        req.message = msg;
        req.scene = shareTo;

        // 调用api接口发送数据到微信
        if (!wxApi.sendReq(req)) {
            Toast.makeText(context, R.string.buildPicture_toast_sharePicture_fail, Toast.LENGTH_LONG).show();
        }
    }

}
