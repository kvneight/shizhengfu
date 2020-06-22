package com.gpsdk.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.tencent.smtt.sdk.QbSdk;

/**
 * Created by Administrator
 *
 * @author 猿史森林
 *         Date: 2017/11/28
 *         Class description:
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                // TODO Auto-generated method stub
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                if(arg0){
                    Toast.makeText(getContext(),"X5内核已成功加载",Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getContext(),"加载X5内核失败",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCoreInitFinished() {
                // TODO Auto-generated method stub
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(getContext(),  cb);

        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
