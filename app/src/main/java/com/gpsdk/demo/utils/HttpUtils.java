package com.gpsdk.demo.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static okhttp3.MultipartBody.FORM;

/**
 * Created by CaoAnyang on 2016/4/11.
 * OKHttp工具类
 */
public class HttpUtils {

    private OkHttpClient client;


    //json请求
    public static final MediaType JSON = MediaType
            .parse("application/json; charset=utf-8");

    private Handler handler = new Handler(Looper.getMainLooper());

    public HttpUtils() {
        this(50 * 1000);
    }

    //超时时间ms
    public HttpUtils(int timeout_ms) {
        client = new OkHttpClient();

        //设置超时
        client.newBuilder().connectTimeout(timeout_ms, TimeUnit.MILLISECONDS).
                writeTimeout(timeout_ms, TimeUnit.MILLISECONDS).readTimeout(timeout_ms, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * post请求,阻塞  json数据为body, 回调在调用者线程
     */
    public void postJsonSync(String url, String json, final HttpCallBack callBack) {
        postJsonSync(url, null, null, json, callBack);
    }

    /**
     * 强制走移动数据网络测试
     *
     * @param context
     */
    public void postJsonByMobileData(Context context) {

    }

    /**
     * post请求，阻塞  json数据为body， 回调在调用者线程
     */
    public void postJsonSync(String url, String headerName, String headerValue, String json, final HttpCallBack callBack) {
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headerName != null) {
            builder.addHeader(headerName, headerValue);
        }

        final Request request = builder.build();
        if (callBack != null) {
            callBack.onStart();
        }

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                if (callBack != null) {
                    callBack.onSuccess(response.body().string());
                }
            } else {
                if (callBack != null) {
                    callBack.onError(response.message());
                }
            }

        } catch (IOException e) {
            if (callBack != null) {
                callBack.onError(e.getMessage());
            }
        }

    }

    /**
     * post请求，不阻塞，回调在UI线程里  json数据为body
     */
    public void postJson(String url, String json, final HttpCallBack callBack) {
        RequestBody body = RequestBody.create(JSON, json);
        final Request request = new Request.Builder().url(url).post(body).build();

        notifyStartInUI(callBack);

        client.newCall(request).enqueue(new Callback() {
            //回调在一个新的线程里
            @Override
            public void onFailure(Call call, IOException e) {
                // Log.e("ThreadTest", "callback thread id = " + Thread.currentThread().getId());
                notifyErrorInUI(callBack, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Log.e("ThreadTest", "callback thread id = " + Thread.currentThread().getId());
                if (response.isSuccessful()) {
                    notifySuccessInUI(callBack, response.body().string());
                } else {
                    notifyErrorInUI(callBack, response.message());
                }
            }
        });
    }

    /**
     * post请求，不阻塞，回调在UI线程里  json数据为body
     */
    public void postForm(String url, String json, final HttpCallBack callBack) {
        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, json);
        final Request request = new Request.Builder().url(url).post(body).build();

        notifyStartInUI(callBack);

        client.newCall(request).enqueue(new Callback() {
            //回调在一个新的线程里
            @Override
            public void onFailure(Call call, IOException e) {
                // Log.e("ThreadTest", "callback thread id = " + Thread.currentThread().getId());
                notifyErrorInUI(callBack, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Log.e("ThreadTest", "callback thread id = " + Thread.currentThread().getId());
                if (response.isSuccessful()) {
                    notifySuccessInUI(callBack, response.body().string());
                } else {
                    notifyErrorInUI(callBack, response.message());
                }
            }
        });
    }

    /**
     * post请求  json数据为body 回调在UI线程里
     */
    public void postJson(String url, String headerName, String headerValue, String json, final HttpCallBack callBack) {
        RequestBody body = RequestBody.create(JSON, json);
        final Request request = new Request.Builder().url(url).addHeader(headerName, headerValue).post(body).build();

        notifyStartInUI(callBack);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyErrorInUI(callBack, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccessInUI(callBack, response.body().string());
                } else {
                    notifyErrorInUI(callBack, response.message());
                }
            }
        });
    }

    /**
     * put请求 json为body，回调在UI线程里
     */
    public void putJson(String url, String json, final HttpCallBack callBack) {
        RequestBody body = RequestBody.create(JSON, json);
        final Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        notifyStartInUI(callBack);

        client.newCall(request).enqueue(new Callback() {
            // 回调一定是在主线程，不论调用者是否在UI线程
            @Override
            public void onFailure(Call call, IOException e) {
                notifyErrorInUI(callBack, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccessInUI(callBack, response.body().string());
                } else {
                    notifyErrorInUI(callBack, response.message());
                }
            }
        });
    }

    /**
     * post请求  map是body， 回调在UI线程里
     *
     * @param url
     * @param map
     * @param callBack
     */
    public void postMap(String url, Map<String, String> map, final HttpCallBack callBack) {
        FormBody.Builder builder = new FormBody.Builder();

        //遍历map
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                builder.add(entry.getKey(), entry.getValue().toString());
            }
        }
        RequestBody body = builder.build();
        Request request = new Request.Builder().url(url).post(body).build();
        notifyStartInUI(callBack);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyErrorInUI(callBack, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccessInUI(callBack, response.body().string());
                } else {
                    notifyErrorInUI(callBack, response.message());
                }
            }
        });
    }

    /**
     * get 异步请求
     *
     * @param url
     * @param callBack 回调在UI线程里
     */
    public void getJson(String url, final HttpCallBack callBack) {
        Request request = new Request.Builder().url(url).build();
        notifyStartInUI(callBack);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                notifyErrorInUI(callBack, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    notifySuccessInUI(callBack, response.body().string());
                } else {
                    notifyErrorInUI(callBack, response.message());
                }
            }
        });
    }

    public ResponseBody upload(String url, String filePath, String fileName) throws Exception {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(FORM)
                .addFormDataPart("file", fileName,
                        RequestBody.create(MediaType.parse("multipart/form-data"), new File(filePath)))
                .build();

        Request request = new Request.Builder()
                .header("Authorization", "Client-ID " + UUID.randomUUID())
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        return response.body();
    }

    public void notifyStartInUI(final HttpCallBack callBack) {
        if (callBack != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {//在主线程操作
                    callBack.onStart();
                }
            });
        }
    }

    public void notifySuccessInUI(final HttpCallBack callBack, final String data) {
        if (callBack != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {//在主线程操作
                    callBack.onSuccess(data);
                }
            });
        }
    }

    public void notifyErrorInUI(final HttpCallBack callBack, final String msg) {
        if (callBack != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callBack.onError(msg);
                }
            });
        }
    }

    public static abstract class HttpCallBack {
        //开始
        public void onStart() {
        }

        //成功回调
        public abstract void onSuccess(String data);

        //失败
        public void onError(String meg) {
        }
    }
}
