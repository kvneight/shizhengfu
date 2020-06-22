package com.gpsdk.demo.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerInLooper {

    private Handler m_handle;
    private LinkedList<LooperTimer> m_looperTimers = new LinkedList<>();

    private LinkedList<ExpiredLooperTimer> expiredTimers = new LinkedList<>(); //爆发的列表

    private long nextExpiredTime = Long.MAX_VALUE;  //  Long.MAX_VALUE 代表没有定时，其他代表定时
    ScheduledFuture<?> m_timer;

    private ScheduledExecutorService mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public TimerInLooper(Looper looper) {
        m_handle = new Handler(looper);
    }


    public interface LooperTimerListener {
        void run(String name, Object param1, Object param2, Object param3);
    }

    private static class LooperTimer {
        String name;
        LooperTimerListener listener;
        Object param1;
        Object param2;
        Object param3;
        long expiredTime;  //下次爆发时间， SystemClock.elapsedRealtime() 毫秒数, 0代表立即爆发
        int repeat;
    }


    private static class ExpiredLooperTimer {
        String name;
        LooperTimerListener listener;
        Object param1;
        Object param2;
        Object param3;
        boolean bCanceled;

        ExpiredLooperTimer(String name, LooperTimerListener listener, Object param1, Object param2, Object param3) {
            this.name = name;
            this.listener = listener;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
            bCanceled = false;
        }
    }


    private void triggerTimer() {

        long now = SystemClock.elapsedRealtime();

        synchronized (expiredTimers) {
            expiredTimers.clear();
        }

        synchronized (m_looperTimers) {

            int i, count = m_looperTimers.size();

            for (i = 0; i < count; i++) {
                LooperTimer t = m_looperTimers.get(i);
                if (t.expiredTime <= now) {
                    if (t.repeat == 0) {
                        m_looperTimers.remove(i);
                        i--;
                        count--;

                    } else {
                        if (t.expiredTime > 0) {
                            t.expiredTime += t.repeat;
                        } else {
                            t.expiredTime = now + t.repeat;
                        }
                    }
                    synchronized (expiredTimers) {
                        expiredTimers.add(new ExpiredLooperTimer(t.name, t.listener, t.param1, t.param2, t.param3));
                    }
                }
            }


            setNextTimer();

        }
        for (ExpiredLooperTimer t : expiredTimers) {
            if (!t.bCanceled) {
                // Log.e(MainService.TAG, "timername:" + t.name);
                t.listener.run(t.name, t.param1, t.param2, t.param3);
            }
        }
        synchronized (expiredTimers) {
            expiredTimers.clear();
        }
    }


    private void postToLoop() {
        nextExpiredTime = Long.MAX_VALUE;
        m_handle.post(new Runnable() {

            @Override
            public void run() {
                triggerTimer();
            }
        });


    }

    public void postEvent(@NonNull String name, @NonNull LooperTimerListener listener, Object param1, Object param2,
                          Object param3) {
        setTimer(name, 0, 0, listener, param1, param2, param3);
    }


    public void setTimer(@NonNull String name, int delay, int repeat, @NonNull LooperTimerListener listener, Object
            param1, Object param2, Object param3) {
        setTimer(name, true, delay, repeat, listener, param1, param2, param3);
    }

    public void setTimer(@NonNull String name, boolean bRestart, int delay, int repeat, @NonNull LooperTimerListener listener, Object
            param1, Object param2, Object param3) {
        LooperTimer callback = null;


        synchronized (m_looperTimers) {
            for (LooperTimer t : m_looperTimers) {
                if (t.name.equals(name)) {
                    callback = t;
                    break;
                }
            }

            if (!bRestart && callback != null) {
                return;
            }

            if (callback == null) {
                callback = new LooperTimer();
                callback.name = name;
                m_looperTimers.add(callback);
            }

            callback.listener = listener;
            callback.param1 = param1;
            callback.param2 = param2;
            callback.param3 = param3;
            callback.repeat = repeat;
            callback.expiredTime = (delay > 0 ? SystemClock.elapsedRealtime() + delay : 0);

            setNextTimer();
        }
    }

    public void cancelTimer(@NonNull String name) {
        synchronized (m_looperTimers) {
            for (LooperTimer t : m_looperTimers) {
                if (t.name.equals(name)) {
                    m_looperTimers.remove(t);
                    setNextTimer();
                    break;
                }
            }
        }

        synchronized (expiredTimers) {
            for (ExpiredLooperTimer et : expiredTimers) {
                if (et.name.equals(name)) {
                    et.bCanceled = true;
                    break;
                }
            }
        }

    }

    private void setNextTimer() {
        long next = Long.MAX_VALUE;

        for (LooperTimer timer : m_looperTimers) {
            if (timer.expiredTime < next) {
                next = timer.expiredTime;
            }
        }

        if (next != nextExpiredTime) {
            nextExpiredTime = Long.MAX_VALUE;

            if (m_timer != null) {
                m_timer.cancel(false);
            }
            m_timer = null;

            if (next == 0) {
                postToLoop();
            } else if (next != Long.MAX_VALUE) {
                nextExpiredTime = next;


                long lDiff = nextExpiredTime - SystemClock.elapsedRealtime();
                lDiff = (lDiff <= 0) ? 0 : lDiff;


                m_timer = mScheduledExecutorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        postToLoop();
                    }
                }, lDiff, TimeUnit.MILLISECONDS);


            }
        }
    }


    public void destroy() {
        synchronized (m_looperTimers) {
            m_looperTimers.clear();
            if (m_timer != null) {
                m_timer.cancel(true);
            }
        }

    }
}
