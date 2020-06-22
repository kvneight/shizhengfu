package com.gpsdk.demo.face;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;

import com.gpsdk.demo.R;
import com.gpsdk.demo.utils.AlarmManagerUtils;
import com.gpsdk.demo.utils.BaseActivity;
import com.gpsdk.demo.utils.SharePreferenceUtils;

public class FaceSettingActivity extends BaseActivity {
    private EditText editRegisterUrl;
    private EditText editDownloadUrl;
    private EditText editAppId;
    private EditText editAppType;
    private EditText editFaceSize;
    private TimePicker timePicker;

    public static final String DEFAULT_FACE_DOWNLOAD_URL = "http://192.168.110.30/";
    public static final String DEFAULT_FACE_REGISTER_URL = "http://192.168.110.30/";
    public static final int DEFAULT_TIME_PICKER_HOUR = 0;
    public static final int DEFAULT_TIME_PICKER_MINUTE = 0;
    public static final int DEFAULT_FACE_APP_ID = 123456;
    public static final int DEFAULT_FACE_APP_TYPE = 3;

    public static final String TAG_FACE_REGISTER = "TAG_FACE_REGISTER";
    public static final String TAG_FACE_DOWNLOAD = "TAG_FACE_DOWNLOAD";
    public static final String TAG_TIME_PICKER_HOUR = "TAG_TIME_PICKER_HOUR";
    public static final String TAG_TIME_PICKER_MINUTE = "TAG_TIME_PICKER_MINUTE";
    public static final String TAG_FACE_APP_ID = "TAG_FACE_APP_ID";
    public static final String TAG_FACE_APP_TYPE = "TAG_FACE_APP_TYPE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.activity_face_setting, null);
        setContentView(view);
        initView(view);
    }

    /**
     * 初始化界面控件
     *
     * @param view
     */
    private void initView(View view) {
        editRegisterUrl = (EditText) view.findViewById(R.id.edit_text_face_register_url);
        editDownloadUrl = (EditText) view.findViewById(R.id.edit_text_face_download_url);
        timePicker = (TimePicker) view.findViewById(R.id.time_pick_download);
        timePicker.setIs24HourView(true);
        editAppId = (EditText) view.findViewById(R.id.edit_text_face_app_id);
        editAppType = (EditText) view.findViewById(R.id.edit_text_face_app_type);
        editFaceSize = findViewById(R.id.editText_facesize);

        String registerUrl = SharePreferenceUtils.getString(this, TAG_FACE_REGISTER, DEFAULT_FACE_REGISTER_URL);
        String downloadUrl = SharePreferenceUtils.getString(this, TAG_FACE_DOWNLOAD, DEFAULT_FACE_DOWNLOAD_URL);
        int timePickerHour = SharePreferenceUtils.getInt(this, TAG_TIME_PICKER_HOUR, DEFAULT_TIME_PICKER_HOUR);
        int timePickerMINUTE = SharePreferenceUtils.getInt(this, TAG_TIME_PICKER_MINUTE, DEFAULT_TIME_PICKER_MINUTE);
        int appId = SharePreferenceUtils.getInt(this, TAG_FACE_APP_ID, DEFAULT_FACE_APP_ID);
        int appType = SharePreferenceUtils.getInt(this, TAG_FACE_APP_TYPE, DEFAULT_FACE_APP_TYPE);
        int facesize = SharePreferenceUtils.getInt(this, SharePreferenceUtils.SP_MIN_FACE_WIDTH, 250);

        editRegisterUrl.setText(registerUrl);
        editDownloadUrl.setText(downloadUrl);
        timePicker.setCurrentHour(timePickerHour);
        timePicker.setCurrentMinute(timePickerMINUTE);
        editAppId.setText(appId + "");
        editAppType.setText(appType + "");
        editFaceSize.setText("" + facesize);
    }

    /**
     * 保存数据
     *
     * @param view
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void save(View view) {
        String registerUrl = editRegisterUrl.getText().toString().trim();
        String downloadUrl = editDownloadUrl.getText().toString().trim();
        int timePickerHour = timePicker.getCurrentHour();
        int timePickerMINUTE = timePicker.getCurrentMinute();
        String appId = editAppId.getText().toString();
        String appType = editAppType.getText().toString();
        //判断数据是否为空
        if (registerUrl.equals("") || downloadUrl.equals("") || appId.equals("") || appType.equals("")) {
            showDialog("提示", "输入错误，请重新输入！", false, "确定", null, null);
            return;
        }
        String sSize = editFaceSize.getText().toString().trim();
        try {
            int size = Integer.parseInt(sSize);
            if (size <= 0) {
                showDialog("提示", "人脸识别尺寸输入错误，请重新输入！", false, "确定", null, null);
                return;
            }
            SharePreferenceUtils.putInt(this, SharePreferenceUtils.SP_MIN_FACE_WIDTH, size);
        } catch (NumberFormatException e) {
            showDialog("提示", "人脸识别尺寸输入错误，请重新输入！", false, "确定", null, null);
            return;
        }


        AlarmManagerUtils.cancelAlarm(this);
        SharePreferenceUtils.putString(this, TAG_FACE_REGISTER, registerUrl);
        SharePreferenceUtils.putString(this, TAG_FACE_DOWNLOAD, downloadUrl);
        SharePreferenceUtils.putInt(this, TAG_TIME_PICKER_HOUR, timePickerHour);
        SharePreferenceUtils.putInt(this, TAG_TIME_PICKER_MINUTE, timePickerMINUTE);
        SharePreferenceUtils.putInt(this, TAG_FACE_APP_ID, Integer.parseInt(appId));
        SharePreferenceUtils.putInt(this, TAG_FACE_APP_TYPE, Integer.parseInt(appType));
        AlarmManagerUtils.setAlarm(this, timePickerHour, timePickerMINUTE);
        toastMessage("数据保存成功！");
        finish();
    }

    /**
     * 关闭当前界面
     *
     * @param view
     */
    public void exit(View view) {
        finish();
    }
}
