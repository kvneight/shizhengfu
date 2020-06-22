package com.gpsdk.demo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.gpsdk.demo.service.MyService;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AlarmReceiver","闹铃响了, 正在同步更新数据~~");
        //同步更新数据
        MyService.downloadFaceFeature();
    }
}
