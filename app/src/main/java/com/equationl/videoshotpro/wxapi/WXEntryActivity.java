package com.equationl.videoshotpro.wxapi;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.equationl.videoshotpro.R;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {
    IWXAPI wxApi;
    private static final String TAG = "el,WXEntryActivity";
    SharedPreferences sp_init;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wx_entry);

        sp_init = getSharedPreferences("init", Context.MODE_PRIVATE);

        wxApi = WXAPIFactory.createWXAPI(this, "wx45ceac6c6d2f1aff", true);
        wxApi.registerApp("wx45ceac6c6d2f1aff");

        try {
            if (!wxApi.handleIntent(getIntent(), this)) {
                finish();
            }
        } catch (Exception e) {
            CrashReport.postCatchedException(e);
        }

    }


    @Override
    public void onResp(BaseResp resp) { //在这个方法中处理微信传回的数据
        //形参resp 有下面两个个属性比较重要
        //1.resp.errCode
        //2.resp.transaction则是在分享数据的时候手动指定的字符创,用来分辨是那次分享(参照4.中req.transaction)
        switch (resp.errCode) { //根据需要的情况进行处理
            case BaseResp.ErrCode.ERR_OK:
                //正确返回
                Toast.makeText(this, R.string.WXEntry_toast_share_success, Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = sp_init.edit();
                editor.putBoolean("isCloseAd", true);
                editor.apply();
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                //用户取消
                Log.i(TAG, "分享取消");
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                //认证被否决
                Toast.makeText(this, R.string.WXEntry_toast_share_ERR_AUTH_DENIED, Toast.LENGTH_SHORT).show();
                break;
            case BaseResp.ErrCode.ERR_SENT_FAILED:
                //发送失败
                Toast.makeText(this, R.string.WXEntry_toast_share_ERR_SENT_FAILED, Toast.LENGTH_SHORT).show();
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                //不支持错误
                Toast.makeText(this, R.string.WXEntry_toast_share_ERR_UNSUPPORT, Toast.LENGTH_SHORT).show();
                break;
            case BaseResp.ErrCode.ERR_COMM:
                //一般错误
                Toast.makeText(this, R.string.WXEntry_toast_share_ERR_COMM, Toast.LENGTH_SHORT).show();
                break;
            default:
                //其他不可名状的情况
                Toast.makeText(this, R.string.WXEntry_toast_share_ERR_UNKNOW, Toast.LENGTH_SHORT).show();
                break;
        }

        finish();
    }

    @Override
    public void onReq(BaseReq req) {
        //......这里是用来处理接收的请求
    }

}
