package com.gpsdk.demo.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.gpsdk.demo.db.FaceData;
import com.gpsdk.demo.message.AcquireTokenMessage;
import com.gpsdk.demo.message.EventMessage;
import com.gpsdk.demo.message.VerifySignaTureMessage;
import com.gpsdk.demo.utils.AlarmManagerUtils;
import com.gpsdk.demo.utils.HttpUtils;
import com.gpsdk.demo.utils.PostUtils;
import com.gpsdk.demo.utils.SecurityUtil;
import com.gpsdk.demo.utils.SharePreferenceUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;
import org.simple.eventbus.EventBus;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_FACE_APP_ID;
import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_FACE_APP_TYPE;
import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_FACE_DOWNLOAD_URL;
import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_FACE_REGISTER_URL;
import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_TIME_PICKER_HOUR;
import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_TIME_PICKER_MINUTE;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_FACE_APP_ID;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_FACE_APP_TYPE;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_FACE_DOWNLOAD;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_FACE_REGISTER;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_TIME_PICKER_HOUR;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_TIME_PICKER_MINUTE;

public class MyService extends Service {
    public static final String TAG = "MyService";
    public static MyService myService;
    public static Context mContext;
    public static String header;

    public MyService() {
        if (myService == null) {
            myService = this;
            mContext = this;
        }
        EventBus.getDefault().register(this);
    }

    public static MyService getMyService() {
        return myService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AlarmManagerUtils.cancelAlarm(this);
        //重新设定定时任务
        int timePickerHour = SharePreferenceUtils.getInt(this, TAG_TIME_PICKER_HOUR, DEFAULT_TIME_PICKER_HOUR);
        int timePickerMINUTE = SharePreferenceUtils.getInt(this, TAG_TIME_PICKER_MINUTE, DEFAULT_TIME_PICKER_MINUTE);
        AlarmManagerUtils.setAlarm(this, timePickerHour, timePickerMINUTE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static String replaceBlank(String src) {
        String dest = "";
        if (src != null) {
            Pattern pattern = Pattern.compile("\t|\r|\n|\\s*");
            Matcher matcher = pattern.matcher(src);
            dest = matcher.replaceAll("");
        }
        return dest;
    }

    /**
     * 上传并保存人脸信息
     *
     * @param userCode
     * @param faceFeature
     */
    public static void uploadFaceFeature(final String userCode, final String faceFeature, String photo) {
        String registerUrl = SharePreferenceUtils.getString(mContext, TAG_FACE_REGISTER, DEFAULT_FACE_REGISTER_URL)
                + "/api/User/Faceaddorupdate";
        final JSONObject jsonObject = new JSONObject();
        photo = replaceBlank(photo);
        try {
            jsonObject.put("facefeature", faceFeature);
            jsonObject.put("facephoto", photo);
            jsonObject.put("userid", userCode);
            //registerUrl = registerUrl + jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject object = new JSONObject();
        try {
            object.put("userinfo", jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new HttpUtils().postJson(registerUrl, object.toString(), new HttpUtils.HttpCallBack() {
            @Override
            public void onSuccess(String data) {
                Log.i(TAG, "uploadFaceFeature data=" + SecurityUtil.unicodeToString(data));
                try {
                    JSONObject object = new JSONObject(data);
                    int errorCode = object.getInt("Errorcode");
                    String msg = SecurityUtil.unicodeToString(object.getString("msg"));
                    if (errorCode == 0) {
                        EventMessage message = new EventMessage(EventMessage.FACE_SAVE_SUCCESS, msg);
                        EventBus.getDefault().post(message);
                        //判断本地是否存在，如存在则更新，不存在则添加
                        FaceData faceData = LitePal.where("userId = '" + userCode + "'").findFirst(FaceData.class);
                        if (faceData != null) {
                            faceData.setFaceFeature(faceFeature);
                            faceData.update(faceData.getId());
                        } else {
                            faceData = new FaceData();
                            faceData.setUserId(userCode);
                            faceData.setFaceFeature(faceFeature);
                            faceData.setIsDel(0);
                            faceData.setAddupTime(System.currentTimeMillis());
                            faceData.setIsUpload(1);
                            faceData.save();
                        }
                    } else {
                        EventMessage message = new EventMessage(EventMessage.FACE_SAVE_FAIL, msg);
                        EventBus.getDefault().post(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    EventMessage message = new EventMessage(EventMessage.FACE_SAVE_FAIL, e.getMessage());
                    EventBus.getDefault().post(message);
                }
            }

            @Override
            public void onError(String meg) {
                Log.i(TAG, "uploadFaceFeature meg=" + SecurityUtil.unicodeToString(meg));
                EventMessage message = new EventMessage(EventMessage.FACE_SAVE_FAIL, meg);
                EventBus.getDefault().post(message);
            }
        });
    }

    /**
     * 人脸数据下载
     */
    public static void downloadFaceFeature() {
        //查询最近更新的时间戳
        long lastTime = 1;
        final FaceData faceData = LitePal.order("addupTime desc").findFirst(FaceData.class);
        if (faceData != null) {
            lastTime = faceData.getAddupTime();
        }
        String downloadUrl = SharePreferenceUtils.getString(mContext, TAG_FACE_DOWNLOAD, DEFAULT_FACE_DOWNLOAD_URL);
        downloadUrl = downloadUrl + "/api/User/Facedowndata?userinfo=";
        JSONObject object = new JSONObject();
        try {
            object.put("adduptime", lastTime);
            downloadUrl = downloadUrl + object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "downloadFaceFeature url=" + downloadUrl);
        new HttpUtils().postJson(downloadUrl, "", new HttpUtils.HttpCallBack() {
            @Override
            public void onSuccess(String data) {
                Log.i(TAG, "downloadFaceFeature onSuccess=" + SecurityUtil.unicodeToString(data));
                try {
                    JSONObject objectData = new JSONObject(data);
                    int errorCode = objectData.getInt("Errorcode");
                    String msg = objectData.getString("msg");
                    if (errorCode == 0) {
                        JSONObject object1 = new JSONObject(objectData.getString("data"));
                        long adduptimeOne = object1.getLong("adduptime");
                        JSONArray arrayNew = object1.getJSONArray("faceusernew");
                        JSONArray arrayDel = object1.getJSONArray("faceuserdel");
                        for (int x = 0; x < arrayNew.length(); x++) {
                            JSONObject objectNew = arrayNew.getJSONObject(x);
                            long adduptimeNew = objectNew.getLong("adduptime");
                            String userIdNew = objectNew.getString("userid");
                            //此处调用获取人脸详细数据
                            acquireFaceFeature(userIdNew);

                        }
                        for (int y = 0; y < arrayDel.length(); y++) {
                            JSONObject objectDel = arrayDel.getJSONObject(y);
                            String userIdDel = objectDel.getString("userid");
                            LitePal.deleteAll(FaceData.class, "userId = " + userIdDel);
                        }
                        EventMessage eventMessage = new EventMessage(EventMessage.FACE_DATA_DOWNLOAD_SUCCESS, data);
                        EventBus.getDefault().post(eventMessage);
                    } else {
                        EventMessage eventMessage = new EventMessage(EventMessage.FACE_DATA_DOWNLOAD_FAIL, data);
                        EventBus.getDefault().post(eventMessage);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    EventMessage eventMessage = new EventMessage(EventMessage.FACE_DATA_DOWNLOAD_FAIL, e.getMessage());
                    EventBus.getDefault().post(eventMessage);
                }
            }

            @Override
            public void onError(String meg) {
                Log.i(TAG, "downloadFaceFeature onError=" + meg);
                EventMessage eventMessage = new EventMessage(EventMessage.FACE_DATA_DOWNLOAD_FAIL, meg);
                EventBus.getDefault().post(eventMessage);
            }
        });
    }

    /**
     * 根据userId获取人脸详细数据
     *
     * @param userId
     */
    public static void acquireFaceFeature(final String userId) {
        String downloadUrl = SharePreferenceUtils.getString(mContext, TAG_FACE_DOWNLOAD, DEFAULT_FACE_DOWNLOAD_URL);
        downloadUrl = downloadUrl + "/api/User/Faceoneinfo?userinfo=";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userid", userId);
            downloadUrl = downloadUrl + jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new HttpUtils().postJson(downloadUrl, "", new HttpUtils.HttpCallBack() {
            @Override
            public void onSuccess(String data) {
                Log.i(TAG, "acquireFaceFeature onSuccess=" + SecurityUtil.unicodeToString(data));
                try {
                    JSONObject object = new JSONObject(data);
                    int errorCode = object.getInt("Errorcode");
                    if (errorCode == 0) {
                        LitePal.deleteAll(FaceData.class);
                        JSONObject objectNew = object.getJSONObject("data");
                        String faceFeature = objectNew.getString("facefeature");
                        String facePhoto = objectNew.getString("facephoto");
                        long addupTime = objectNew.getLong("adduptime");
                        String isDel = objectNew.getString("isdel");

                        FaceData faceData = new FaceData();
                        faceData.setFaceFeature(faceFeature);
                        faceData.setFacePhoto(facePhoto);
                        faceData.setUserId(userId);
                        faceData.setAddupTime(addupTime);
                        faceData.setIsDel(Integer.parseInt(isDel));
                        boolean b = faceData.save();
                        Log.i(TAG, "保存本地数据 ----" + b);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String meg) {
                Log.i(TAG, "acquireFaceFeature onError=" + meg);
            }
        });
    }

    /**
     * 获取Token
     */
    public static void acquireToken() {
        String registerUrl = SharePreferenceUtils.getString(mContext, TAG_FACE_REGISTER, DEFAULT_FACE_REGISTER_URL);
        final int appId = SharePreferenceUtils.getInt(mContext, TAG_FACE_APP_ID, DEFAULT_FACE_APP_ID);
        int appType = SharePreferenceUtils.getInt(mContext, TAG_FACE_APP_TYPE, DEFAULT_FACE_APP_TYPE);
        registerUrl = registerUrl + "/Api/Gettoken/index?appid=" + appId + "&apptype=" + appType;
        new HttpUtils().getJson(registerUrl, new HttpUtils.HttpCallBack() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSuccess(String str) {
                Log.i(TAG, "acquireToken = " + SecurityUtil.unicodeToString(str));
                //解析收到的数据
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    int errorCode = jsonObject.getInt("Errorcode");
                    if (errorCode == 0) {
                        String msg = jsonObject.getString("msg");
                        String s = jsonObject.getString("data");
                        JSONObject jsonObject1 = new JSONObject(s);
                        String signature = jsonObject1.getString("signature");
                        //解密signature
                        String signaTureData = SecurityUtil.java_openssl_decrypt(signature, SecurityUtil.IV);
                        JSONObject jsonObject2 = new JSONObject(signaTureData);
                        String s_appId = jsonObject2.getString("AppID");
                        String s_appName = SecurityUtil.unicodeToString(jsonObject2.getString("Appname"));
                        String s_appType = jsonObject2.getString("Apptype");
                        long time_token = jsonObject2.getLong("time_token");
                        long sxtime = jsonObject1.getLong("sxtime");
                        String appurl = jsonObject1.getString("appurl");
                        //设置数据
                        AcquireTokenMessage message = new AcquireTokenMessage();
                        message.setErrorcode(errorCode);
                        message.setMsg(msg);
                        message.setSignature(signature);
                        message.setSxtime(sxtime);
                        message.setAppurl(appurl);
                        message.setAppId(s_appId);
                        message.setAppName(s_appName);
                        message.setAppType(s_appType);
                        message.setTimeToken(time_token);
                        header = signaTureData;
                        Log.i(TAG, message.toString());
                        EventBus.getDefault().post(message);
                        //获取Token后验证签名是否正确
                        VerifySignaTure(message);
                    } else {
                        AcquireTokenMessage message = new AcquireTokenMessage();
                        message.setErrorcode(errorCode);
                        EventBus.getDefault().post(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    AcquireTokenMessage message = new AcquireTokenMessage();
                    message.setErrorcode(10001);
                    message.setMsg(e.getMessage());
                    EventBus.getDefault().post(message);
                }
            }

            @Override
            public void onError(String meg) {
                Log.i(TAG, "acquireToken = " + meg);
                AcquireTokenMessage message = new AcquireTokenMessage();
                message.setErrorcode(10001);
                message.setMsg(meg);
                EventBus.getDefault().post(message);
            }
        });
    }

    /**
     * 验证签名是否正确
     *
     * @param message
     */
    public static void VerifySignaTure(final AcquireTokenMessage message) {
        String registerUrl = SharePreferenceUtils.getString(mContext, TAG_FACE_REGISTER, DEFAULT_FACE_REGISTER_URL);
        registerUrl = registerUrl + "/Api/index/index?Appid=" + message.getAppId() + "&Apptype=" + message.getAppType() + "&Appurl=" + message.getAppurl() + "&Sxtime=" + message.getSxtime();
        new HttpUtils().postJson(registerUrl, "", new HttpUtils.HttpCallBack() {
            @Override
            public void onSuccess(String data) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    int errorCode = jsonObject.getInt("Errorcode");
                    String msg = jsonObject.getString("msg");
                    VerifySignaTureMessage verifySignaTureMessage = new VerifySignaTureMessage();
                    verifySignaTureMessage.setErrorcode(errorCode);
                    verifySignaTureMessage.setMsg(msg);
                    verifySignaTureMessage.setAcquireTokenMessage(message);
                    EventBus.getDefault().post(verifySignaTureMessage);

                } catch (JSONException e) {
                    e.printStackTrace();
                    VerifySignaTureMessage verifySignaTureMessage = new VerifySignaTureMessage();
                    verifySignaTureMessage.setErrorcode(1001);
                    verifySignaTureMessage.setMsg(e.getMessage());
                    verifySignaTureMessage.setAcquireTokenMessage(message);
                    EventBus.getDefault().post(verifySignaTureMessage);
                }
            }

            @Override
            public void onError(String meg) {
                Log.i(TAG, "meg=" + meg);
                VerifySignaTureMessage verifySignaTureMessage = new VerifySignaTureMessage();
                verifySignaTureMessage.setErrorcode(1001);
                verifySignaTureMessage.setMsg(meg);
                verifySignaTureMessage.setAcquireTokenMessage(message);
                EventBus.getDefault().post(verifySignaTureMessage);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
