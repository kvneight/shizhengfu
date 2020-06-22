package com.gpsdk.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gpsdk.demo.face.FaceLoginActivity;
import com.gpsdk.demo.face.FaceRegisterActivity;
import com.gpsdk.demo.face.FaceSettingActivity;
import com.gpsdk.demo.message.EventMessage;
import com.gpsdk.demo.service.MyService;
import com.gpsdk.demo.utils.SharePreferenceUtils;

import org.simple.eventbus.EventBus;


public class MethodsForJS extends Object {
    public static final int PRINT_TEXT = 0;
    public static final int PRINT_ONE_CODE = 1;
    public static final int PRINT_TWO_CODE = 2;
    public static final String HTML_URL = "htmlUrl";
    Context context;

    public MethodsForJS(Context _context) {
        context = _context;
    }

    //开放接口

    /**
     * 配置H5主页地址配
     */
    @JavascriptInterface
    public void setH5RootUrl() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = MainActivity.getMainActivity().getLayoutInflater().inflate(R.layout.layout_dialog_html_set, null);
        Button btnSave = (Button) view.findViewById(R.id.btn_save);
        AlertDialog dialog = null;
        builder.setView(view);
        dialog = builder.show();
        final EditText editText = (EditText) view.findViewById(R.id.edit_html_value);
        editText.setText(SharePreferenceUtils.getString(context, HTML_URL, ""));
        final AlertDialog finalDialog = dialog;
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = editText.getText().toString();
                SharePreferenceUtils.putString(context, HTML_URL, str);
                Toast.makeText(context, "保存成功！请重启生效！", Toast.LENGTH_SHORT).show();
                finalDialog.dismiss();
            }
        });

    }

    /**
     * 打开连接蓝牙打印机/设置默认蓝牙打印机窗口
     */
    @JavascriptInterface
    public void setBluetooth() {
        EventBus.getDefault().post(new EventMessage(EventMessage.PRINTER_CONNECT));
    }

    /**
     * 打开配置蓝牙打印机参数窗口
     */
    @JavascriptInterface
    public void printSetup() {
        EventBus.getDefault().post(new EventMessage(EventMessage.PRINTER_SETTING));
    }

    /**
     * 显示一个Toast消息
     *
     * @param message
     */
    @JavascriptInterface
    public void androidAlerForJS(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 打印数据方法
     *
     * @param type    打印类型 0-字符串 1-条码 2-二维码
     * @param content 打印数据
     */
    @JavascriptInterface
    public void printQRCode(int type, String content) {
        if (content == null || content.equals("")) {
            Toast.makeText(context, "打印失败！请检查打印内容再重新打印！", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (type) {
            case PRINT_TEXT:
                //暂时不支持直接打文字了
                EventBus.getDefault().post(new EventMessage(EventMessage.PRINTER_LABEL, content));
//                break;
                return;
            case PRINT_ONE_CODE:
                //暂时不支持一维码/条形码了
                return;
//                EventBus.getDefault().post(new EventMessage(EventMessage.PRINTER_ONE_CODE, content));
//                break;
            case PRINT_TWO_CODE:
                EventBus.getDefault().post(new EventMessage(EventMessage.PRINTER_TWO_CODE, content));
                break;
        }
    }

    /**
     * 获取当前连接打印机信息 不可返回Null
     *
     * @return
     */
    @JavascriptInterface
    public String getPrintName() {
        String str = MainActivity.getMainActivity().getConnDeviceInfo();
        if (str == null) {
            return "";
        } else {
            return str;
        }
    }

    /**
     * 断开与蓝牙打印机的连接
     */
    @JavascriptInterface
    public void closePrinter() {
        MainActivity.getMainActivity().printerDisConn();
        //EventBus.getDefault().post(new EventMessage(EventMessage.PRINTER_DISCONN));
    }

    /**
     * 配置人脸识别参数
     */
    @JavascriptInterface
    public void faceServiceSetup() {
        Intent intent = new Intent(context, FaceSettingActivity.class);
        context.startActivity(intent);
    }

    /**
     * 开始人脸登记
     *
     * @param userCode
     */
    @JavascriptInterface
    public void userFaceRegist(String userCode) {
        Intent intent = new Intent(context, FaceRegisterActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userCode", userCode);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    /**
     * 开始人脸登录
     */
    @JavascriptInterface
    public void userFaceCheck() {
        Intent intent = new Intent(context, FaceLoginActivity.class);
        context.startActivity(intent);
    }

    /**
     * 即时下载（同步人脸数据）
     */
    @JavascriptInterface
    public void downFaceData() {
        MyService.downloadFaceFeature();
    }

    /**
     * 退出APP
     */
    @JavascriptInterface
    public void appExit() {
        MainActivity.getMainActivity().appExit();
    }
}
