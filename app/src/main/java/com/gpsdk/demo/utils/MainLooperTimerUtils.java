package com.gpsdk.demo.utils;

import android.os.Looper;
import android.support.annotation.NonNull;


public class MainLooperTimerUtils {

    static TimerInLooper m_timerInLooper = new TimerInLooper(Looper.getMainLooper());

    /**
     * controller 通用接口
     */
    public static void postEvent(@NonNull String name, @NonNull TimerInLooper.LooperTimerListener listener, Object param1, Object param2, Object param3) {
        m_timerInLooper.postEvent(name, listener, param1, param2, param3);
    }

    public static void setTimer(@NonNull String name, boolean bRestart, int delay, int repeat, @NonNull TimerInLooper.LooperTimerListener listener, Object param1, Object param2, Object param3) {
        m_timerInLooper.setTimer(name, bRestart, delay, repeat, listener, param1, param2, param3);
    }

    public static void setTimer(@NonNull String name, int delay, int repeat, @NonNull TimerInLooper.LooperTimerListener listener, Object param1, Object param2, Object param3) {
        m_timerInLooper.setTimer(name, delay, repeat, listener, param1, param2, param3);
    }

    public static void setTimer(@NonNull String name, int delay, int repeat, @NonNull TimerInLooper.LooperTimerListener listener) {
        setTimer(name, delay, repeat, listener, null, null, null);
    }

    public static void cancelTimer(@NonNull String name) {
        m_timerInLooper.cancelTimer(name);
    }
}
