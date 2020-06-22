package com.gpsdk.demo.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.gpsdk.demo.receiver.AlarmReceiver;

import java.util.Calendar;
import java.util.TimeZone;

import static android.content.Context.ALARM_SERVICE;

public class AlarmManagerUtils {
    public static final String TAG = "AlarmManagerUtils";

    /**
     * 设定定时任务
     *
     * @param context
     * @param mHour
     * @param mMinute
     */
    public static void setAlarm(Context context, int mHour, int mMinute) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

        long firstTime = SystemClock.elapsedRealtime();    // 开机之后到现在的运行时间(包括睡眠时间)
        long systemTime = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 这里时区需要设置一下，不然会有8个小时的时间差
        calendar.set(Calendar.MINUTE, mMinute);
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 选择的每天定时时间
        long selectTime = calendar.getTimeInMillis();

        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
        if (systemTime > selectTime) {
            //Toast.makeText(context, "设置的时间小于当前时间", Toast.LENGTH_SHORT).show();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectTime = calendar.getTimeInMillis();
        }

        // 计算现在时间到设定时间的时间差
        long time = selectTime - systemTime;
        firstTime += time;

        // 进行闹铃注册
        AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                firstTime, 24 * 60 * 1000, sender);

        Log.i(TAG, "time ==== " + time + ", selectTime ===== "
                + selectTime + ", systemTime ==== " + systemTime + ", firstTime === " + firstTime
                + "  intervalMillis ==== " + (24 * 60 * 1000));

        //Toast.makeText(context, "设置重复闹铃成功! ", Toast.LENGTH_LONG).show();
    }

    /**
     * 取消定时
     *
     * @param context
     */
    public static void cancelAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context,
                0, intent, 0);

        // 取消闹铃
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.cancel(sender);
    }
}
