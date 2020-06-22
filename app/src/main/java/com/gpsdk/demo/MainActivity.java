package com.gpsdk.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.gpsdk.demo.message.EventMessage;
import com.gpsdk.demo.service.MyService;
import com.gpsdk.demo.utils.SharePreferenceUtils;
import com.gpsdk.demo.utils.TestMessage;
import com.gpsdk.demo.utils.ZXingUtils;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.simple.eventbus.EventBus;
import org.simple.eventbus.Subscriber;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static com.gpsdk.demo.Constant.ACTION_USB_PERMISSION;
import static com.gpsdk.demo.Constant.MESSAGE_UPDATE_PARAMETER;
import static com.gpsdk.demo.DeviceConnFactoryManager.ACTION_QUERY_PRINTER_STATE;
import static com.gpsdk.demo.DeviceConnFactoryManager.CONN_STATE_FAILED;
import static com.gpsdk.demo.MethodsForJS.HTML_URL;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_AUTOCLOSE;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_DIRECTION;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_HEIGHT;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_MIRROR;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_ONE_CODE_HEIGHT;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_ONE_CODE_WIDTH;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_PAPERSPACE;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_POINT_X;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_POINT_Y;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_ROTATION;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_TWO_CODE_HEIGHT;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_TWO_CODE_WIDTH;
import static com.gpsdk.demo.PrinterSettingActivity.PRINTER_WIDTH;
import static com.gpsdk.demo.face.FaceSettingActivity.DEFAULT_FACE_REGISTER_URL;
import static com.gpsdk.demo.face.FaceSettingActivity.TAG_FACE_REGISTER;

//import android.webkit.WebChromeClient;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//import android.webkit.ValueCallback;

/**
 * Created by Administrator
 *
 * @author 猿史森林
 * Date: 2017/8/2
 * Class description:
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 0x004;
    private static final int PHOTO_REQUEST_CHOOSE = 211;
    private static final int PHOTO_REQUEST_CHOOSE2 = 212;
    private static final int PHOTO_REQUEST_TAKE = 213;
    private static final int REQUEST_PERMISSIONS = 201;


    private static final String TAG = "MainActivity";
    ArrayList<String> per = new ArrayList<>();
    private UsbManager usbManager;
    private int counts;
    public static MainActivity mainActivity;

    private boolean HasPerimission = false;
    private boolean ret = false;
    private boolean ifExit = false;
    private Uri photoUri;
    WebView webView;
    private ValueCallback<Uri[]> mPicUploadMessage;
    private ValueCallback<Uri> mPicUploadMessage4;
    private static final int UPLOADFILETYPE_ALL = 0;
    private static final int UPLOADFILETYPE_IMAGE = 1;
    private static final int UPLOADFILETYPE_VIDEO = 2;
    private ProgressDialog mConnectingDialog;

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    /**
     * 连接状态断开
     */
    private static final int CONN_STATE_DISCONN = 0x007;
    /**
     * 使用打印机指令错误
     */
    private static final int PRINTER_COMMAND_ERROR = 0x008;


    /**
     * ESC查询打印机实时状态指令
     */
    private byte[] esc = {0x10, 0x04, 0x02};


    /**
     * TSC查询打印机状态指令
     */
    private byte[] tsc = {0x1b, '!', '?'};

    private static final int CONN_MOST_DEVICES = 0x11;
    private static final int CONN_PRINTER = 0x12;
    private PendingIntent mPermissionIntent;
    private String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private TextView tvConnState;
    private ThreadPool threadPool;
    /**
     * 判断打印机所使用指令是否是ESC指令
     */
    private int id = 0;
    private EditText etPrintCounts;

    private String devName;//打印机名称
    private String macAddress;//蓝牙地址

    //打印纸张参数
    private int printWidth;
    private int printHeight;
    private int printPaperSpace;
    private LabelCommand.DIRECTION printDirection;
    private LabelCommand.MIRROR printMirror;
    private LabelCommand.ROTATION printRotation;
    private int printerAutoClose;

    private int pointX;
    private int pointY;
    private int oneCodeHeight;
    private int twoCodeWidth;
    private int twoCodeHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;
        EventBus.getDefault().register(this);
        Log.e(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        checkPermission();
        requestPermission();
//        initPermission();
        tvConnState = findViewById(R.id.tv_connState);
        etPrintCounts = findViewById(R.id.et_print_counts);
        initWebView();
        loadDefaultSetting();
        checkBluetoothAndConnect();
        //启动MyService
        Intent intent = new Intent(this, MyService.class);
        startService(intent);

    }

    /**
     * 加载配置参数
     */
    private void loadDefaultSetting() {
        String width = SharePreferenceUtils.getString(this, PRINTER_WIDTH);
        String height = SharePreferenceUtils.getString(this, PRINTER_HEIGHT);
        String strPaperSpace = SharePreferenceUtils.getString(this, PRINTER_PAPERSPACE);
        int direction = SharePreferenceUtils.getInt(this, PRINTER_DIRECTION, 0);
        int mirror = SharePreferenceUtils.getInt(this, PRINTER_MIRROR, 0);
        int rotation = SharePreferenceUtils.getInt(this, PRINTER_ROTATION, 0);
        printerAutoClose = SharePreferenceUtils.getInt(this, PRINTER_AUTOCLOSE, 0);
        switch (rotation) {
            case 0:
                printRotation = LabelCommand.ROTATION.ROTATION_0;
                break;
            case 1:
                printRotation = LabelCommand.ROTATION.ROTATION_90;
                break;
            case 2:
                printRotation = LabelCommand.ROTATION.ROTATION_180;
                break;
            case 3:
                printRotation = LabelCommand.ROTATION.ROTATION_270;
                break;
        }
        if (width == null || width.equals("")) {
            width = "40";
        }
        if (height == null || height.equals("")) {
            height = "30";
        }
        if (strPaperSpace == null || strPaperSpace.equals("")) {
            strPaperSpace = "2";
        }
        if (direction == 0) {
            printDirection = LabelCommand.DIRECTION.FORWARD;
        } else {
            printDirection = LabelCommand.DIRECTION.BACKWARD;
        }
        if (mirror == 0) {
            printMirror = LabelCommand.MIRROR.NORMAL;
        } else {
            printMirror = LabelCommand.MIRROR.MIRROR;
        }
        printWidth = Integer.parseInt(width);
        printHeight = Integer.parseInt(height);
        printPaperSpace = Integer.parseInt(strPaperSpace);
        pointX = SharePreferenceUtils.getInt(this, PRINTER_POINT_X, 0);
        pointY = SharePreferenceUtils.getInt(this, PRINTER_POINT_Y, 0);
        int oneCodeWidth = SharePreferenceUtils.getInt(this, PRINTER_ONE_CODE_WIDTH, 40);
        oneCodeHeight = SharePreferenceUtils.getInt(this, PRINTER_ONE_CODE_HEIGHT, 30);
        twoCodeWidth = SharePreferenceUtils.getInt(this, PRINTER_TWO_CODE_WIDTH, 150);
        twoCodeHeight = SharePreferenceUtils.getInt(this, PRINTER_TWO_CODE_HEIGHT, 150);
    }

    /**
     * 检查蓝牙配置并连接
     */
    private void checkBluetoothAndConnect() {
        String bluetoothAddress = SharePreferenceUtils.getString(this, "BluetoothDevName");
        String strDevName = SharePreferenceUtils.getString(this, "BluetoothAddress");
        if (bluetoothAddress == null || bluetoothAddress.equals("")) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("提示");
//            builder.setMessage("打印机尚未连接，请先连接打印机在使用!");
//            builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    startActivityForResult(new Intent(MainActivity.this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);
//                }
//            });
//            builder.setPositiveButton("取消", null);
//            builder.show();
            toastMessage("注意：蓝牙打印机未连接");
        } else {
            toastMessage("正在连接打印机，请稍候...");
            new DeviceConnFactoryManager.Build(this)
                    .setId(id)
                    //设置连接方式
                    .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
                    .setDevName(strDevName)
                    //设置连接的蓝牙mac地址
                    .setMacAddress(bluetoothAddress)
                    .build();
            //打开端口
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
        }
    }

    /**
     * 初始化WebView
     */
    protected void initWebView() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        webView = findViewById(R.id.webMain);
        WebSettings webSettings = webView.getSettings();

        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);

        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        //缩放操作
        webSettings.setSupportZoom(false); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(false); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件
        //其他细节操作
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); //关闭webview中缓存
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        webSettings.setDomStorageEnabled(true); // 开启 DOM storage API 功能
        webSettings.setDatabaseEnabled(true);   //开启 database storage API 功能
        webSettings.setAppCacheEnabled(true);//开启 Application Caches 功能

        webView.setWebChromeClient(
                new WebChromeClient() {
                    @Override
                    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
//                        if (HasPerimission) {
                        mPicUploadMessage = filePathCallback;
                        getPhoto();
                        return true;
//                        } else {
//                            cancelPicChoose();
//                            toastMessage("系统未授权");
//                            return false;
//                        }
                    }

                    @Override
                    public void openFileChooser(ValueCallback<Uri> valueCallback, String s, String s1) {
                        mPicUploadMessage4 = valueCallback;
                        getPhoto();
                        return;
                    }
                }
        );//要JS在HTML中正常运行，需要ChromeClient
        webView.setWebViewClient(new WebViewClient() {
            //当在当前页面点击链接时，处理
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //在当前WebView打开链接
                view.loadUrl(url);
                //返回true表示点击链接已处理，WebView不再进行其他处理。返回false表示需要WebView处理链接，WebView会显示当前设备安装的浏览器，让用户选择一个浏览器打开这个链接
                return true;
            }
        });

        //开放JS可调用的对象
        webView.addJavascriptInterface(new MethodsForJS(this), "androidJS");

        String url = SharePreferenceUtils.getString(this, HTML_URL, "");
        if (url.equals("")) {
            webView.loadUrl("file:////android_asset/index.html");
        } else {
            webView.loadUrl(url);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ifExit = false;
            if (webView != null) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                webView.evaluateJavascript("javascript:backHandler();", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        //此处为 js 返回的结果
                        if (value.equals("noexit")) {

                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getMainActivity());
                            builder.setTitle("是否退出");
                            builder.setPositiveButton(
                                    "确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ifExit = true;
                                            finish();
                                        }
                                    }
                            );
                            builder.setNegativeButton(
                                    "取消",
                                    null);
                            AlertDialog alertDialog = builder.create();
                            alertDialog.show();
                        }
                    }
                });
                if (!ifExit) {
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 取消上传图片时调用
     */
    protected void cancelPicChoose() {
        if (mPicUploadMessage4 != null) {
            mPicUploadMessage4.onReceiveValue(null);
            mPicUploadMessage4 = null;
        }
        if (mPicUploadMessage != null) {
            mPicUploadMessage.onReceiveValue(new Uri[]{});
            mPicUploadMessage = null;
        }
    }

    //region 拍照或从图库中选择
    protected void getPhoto() {
        String[] item = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择媒体文件");
        item = new String[]{"选择图片", "选择视频", "拍照", "一分钟短视频"};
        builder.setItems(item, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        choosePhoto(UPLOADFILETYPE_IMAGE);
                        break;
                    case 1:
                        choosePhoto(UPLOADFILETYPE_VIDEO);
                        break;
                    case 2:
                        takePhoto(UPLOADFILETYPE_IMAGE);
                        break;
                    case 3:
                        takePhoto(UPLOADFILETYPE_VIDEO);
                        break;
                }
            }
        });
        // 取消可以不添加
        builder.setNegativeButton(
                "取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        cancelPicChoose();
                    }
                });
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
//        try {
//            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
//            mAlert.setAccessible(true);
//            Object mAlertController = mAlert.get(alertDialog);
//            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
//            mMessage.setAccessible(true);
//            TextView mMessageView = (TextView) mMessage.get(mAlertController);
//            mMessageView.setTextSize(14);
////            mMessageView.setTextColor(Color.BLUE);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * 从相册中选择
     */
    public void choosePhoto(int accType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);//ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (accType == UPLOADFILETYPE_VIDEO) {
            intent.setType("video/*");
        } else {
            intent.setType("image/*");
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            startActivityForResult(intent, PHOTO_REQUEST_CHOOSE);
        } else {
            startActivityForResult(intent, PHOTO_REQUEST_CHOOSE2);
        }
    }

    /**
     * 拍照
     */
    public void takePhoto(int accType) {
        if (accType == UPLOADFILETYPE_VIDEO) {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);//设置录像质量
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);//设置录像时间限制，以秒为单位
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024 * 128);//设置录像文件大小
            ContentValues values = new ContentValues();
            photoUri = this.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, PHOTO_REQUEST_TAKE);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//"android.media.action.IMAGE_CAPTURE"
            /***
             * 需要说明一下，以下操作使用照相机拍照，拍照后的图片会存放在相册中的
             * 这里使用的这种方式有一个好处就是获取的图片是拍照后的原图
             * 如果不实用ContentValues存放照片路径的话，拍照后获取的图片为缩略图不清晰
             */
            ContentValues values = new ContentValues();
            photoUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, PHOTO_REQUEST_TAKE);
        }

    }
    //endregion

    //region 初始化相机相关权限，适配6.0+手机的运行时权限，调用方法：initPermission()

    /**
     * 初始化相机相关权限
     * 适配6.0+手机的运行时权限
     */
    private void initPermission() {
        if (Build.VERSION.SDK_INT > 22) {
            String[] permissions = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE};
            //检查权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                HasPerimission = false;
                // 之前拒绝了权限，但没有点击 不再询问 这个时候让它继续请求权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
                } else {
                    //注册相机权限
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
                }
            } else {
                //已经有权限
                HasPerimission = true;
            }
        } else {
//这个说明系统版本在6.0之下，不需要动态获取权限。
            HasPerimission = true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //成功
                    HasPerimission = true;
//                    Toast.makeText(this, "用户授权相机权限", Toast.LENGTH_SHORT).show();
                } else {
                    HasPerimission = false;
                    // 勾选了不再询问
//                    Toast.makeText(this, "用户拒绝相机权限", Toast.LENGTH_SHORT).show();
                    Intent intent = getAppDetailSettingIntent(MainActivity.this);
                    startActivity(intent);
                }
                break;
        }
    }

    /**
     * 获取 APP 详情页面intent
     *
     * @return
     */
    public static Intent getAppDetailSettingIntent(Context context) {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", context.getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
        }
        return localIntent;
    }
    //endregion

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_QUERY_PRINTER_STATE);
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);
        registerReceiver(receiver, filter);
    }

    private void checkPermission() {
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {
                per.add(permission);
            }
        }
    }

    private void requestPermission() {
        if (per.size() > 0) {
            String[] p = new String[per.size()];
            ActivityCompat.requestPermissions(this, per.toArray(p), REQUEST_CODE);
        }
    }

    /**
     * 蓝牙连接
     */
    public void btnBluetoothConn(View view) {
        startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);
    }

    /**
     * 连接多设备
     *
     * @param view
     */
    public void btnMoreDevices(View view) {
        startActivityForResult(new Intent(this, ConnMoreDevicesActivity.class), CONN_MOST_DEVICES);
    }

    /**
     * 串口连接
     *
     * @param view
     */
    public void btnSerialPortConn(View view) {
        startActivityForResult(new Intent(this, SerialPortList.class), Constant.SERIALPORT_REQUEST_CODE);
    }

    /**
     * USB连接
     *
     * @param view
     */
    public void btnUsbConn(View view) {
        startActivityForResult(new Intent(this, UsbDeviceList.class), Constant.USB_REQUEST_CODE);
    }

    /**
     * WIFI连接
     *
     * @param view
     */
    public void btnWifiConn(View view) {
        WifiParameterConfigDialog wifiParameterConfigDialog = new WifiParameterConfigDialog(this, mHandler);
        wifiParameterConfigDialog.show();
    }

    public void btnReceiptPrint(View view) {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, getString(R.string.str_cann_printer));
            return;
        }
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask(new Runnable() {
            @Override
            public void run() {
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {
                    sendReceiptWithResponse();
                } else {
                    mHandler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget();
                }
            }
        });
    }

    public void btnLabelPrint(View view) {
        threadPool = ThreadPool.getInstantiation();
        threadPool.addTask(new Runnable() {
            @Override
            public void run() {
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                        !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                    mHandler.obtainMessage(CONN_PRINTER).sendToTarget();
                    return;
                }
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC) {
                    sendLabel("123456789");
                } else {
                    mHandler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget();
                }
            }
        });
    }

    public void btnDisConn(View view) {
        mHandler.obtainMessage(CONN_STATE_DISCONN).sendToTarget();
    }

    /**
     * 打印机状态查询
     *
     * @param view
     */
    public void btnPrinterState(View view) {
        //打印机状态查询
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, getString(R.string.str_cann_printer));
            return;
        }
        ThreadPool.getInstantiation().addTask(new Runnable() {
            @Override
            public void run() {
                Vector<Byte> data = new Vector<>(esc.length);
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {

                    for (int i = 0; i < esc.length; i++) {
                        data.add(esc[i]);
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(data);
                }

                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC) {
                    for (int i = 0; i < tsc.length; i++) {
                        data.add(tsc[i]);
                    }
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(data);
                }
            }
        });
    }

    /**
     * 连续打印
     *
     * @param view
     */
    public void btnReceiptAndLabelContinuityPrint(View view) {
        counts = Integer.parseInt(etPrintCounts.getText().toString().trim());
        sendContinuityPrint();
    }

    private void sendContinuityPrint() {
        ThreadPool.getInstantiation().addTask(new Runnable() {
            @Override
            public void run() {
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null
                        && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                    ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder("MainActivity_sendContinuity_Timer");
                    ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder);
                    scheduledExecutorService.schedule(threadFactoryBuilder.newThread(new Runnable() {
                        @Override
                        public void run() {
                            counts--;
                            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {
                                sendReceiptWithResponse();
                            } else {
                                //标签模式可直接使用LabelCommand.addPrint()方法进行打印
                                sendLabel("");
                            }
                        }
                    }), 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri ret = null;
            Uri[] rets = null;
            switch (requestCode) {
                /*蓝牙连接*/
                case Constant.BLUETOOTH_REQUEST_CODE: {
                    Utils.toast(MainActivity.this, "正在连接打印机，请稍候...");
                    /*获取蓝牙mac地址*/
                    devName = data.getStringExtra(BluetoothDeviceList.EXTRA_DEVICE_NAME);
                    macAddress = data.getStringExtra(BluetoothDeviceList.EXTRA_DEVICE_ADDRESS);
                    //初始化DeviceConnFactoryManager
                    DeviceConnFactoryManager dm = new DeviceConnFactoryManager.Build(this)
                            .setId(id)
                            //设置连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
                            .setDevName(devName)
                            //设置连接的蓝牙mac地址
                            .setMacAddress(macAddress)
                            .build();
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] = dm;
                    //打开端口
                    dm.openPort();
                    break;
                }
                /*USB连接*/
                case Constant.USB_REQUEST_CODE: {
                    //获取USB设备名
                    String usbName = data.getStringExtra(UsbDeviceList.USB_NAME);
                    //通过USB设备名找到USB设备
                    UsbDevice usbDevice = Utils.getUsbDeviceFromName(MainActivity.this, usbName);
                    //判断USB设备是否有权限
                    if (usbManager.hasPermission(usbDevice)) {
                        usbConn(usbDevice);
                    } else {//请求权限
                        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(usbDevice, mPermissionIntent);
                    }
                    break;
                }
                /*串口连接*/
                case Constant.SERIALPORT_REQUEST_CODE:
                    //获取波特率
                    int baudrate = data.getIntExtra(Constant.SERIALPORTBAUDRATE, 0);
                    //获取串口号
                    String path = data.getStringExtra(Constant.SERIALPORTPATH);

                    if (baudrate != 0 && !TextUtils.isEmpty(path)) {
                        //初始化DeviceConnFactoryManager
                        new DeviceConnFactoryManager.Build(this)
                                //设置连接方式
                                .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.SERIAL_PORT)
                                .setId(id)
                                //设置波特率
                                .setBaudrate(baudrate)
                                //设置串口号
                                .setSerialPort(path)
                                .build();
                        //打开端口
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                    }
                    break;
                case CONN_MOST_DEVICES:
                    id = data.getIntExtra("id", 0);
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null &&
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                        tvConnState.setText(getString(R.string.str_conn_state_connected) + "\n" + getConnDeviceInfo());
                        toastMessage(getString(R.string.str_conn_state_connected) + "\n" + getConnDeviceInfo());
                    } else {
                        tvConnState.setText(getString(R.string.str_conn_state_disconnect));
                        toastMessage(getString(R.string.str_conn_state_disconnect));
                    }
                    break;
                case PHOTO_REQUEST_CHOOSE:
                case PHOTO_REQUEST_CHOOSE2:
                    ret = data.getData();
                    rets = new Uri[]{ret};
                    if (mPicUploadMessage4 != null) {
                        mPicUploadMessage4.onReceiveValue(ret);
                        mPicUploadMessage4 = null;
                    }
                    if (mPicUploadMessage != null) {
                        mPicUploadMessage.onReceiveValue(rets);
                        mPicUploadMessage = null;
                    }
                    break;
                case PHOTO_REQUEST_TAKE:
                    ret = photoUri;
                    rets = new Uri[]{ret};
                    if (mPicUploadMessage4 != null) {
                        mPicUploadMessage4.onReceiveValue(ret);
                        mPicUploadMessage4 = null;
                    }
                    if (mPicUploadMessage != null) {
                        mPicUploadMessage.onReceiveValue(rets);
                        mPicUploadMessage = null;
                    }
                    break;
                default:
                    break;
            }
        } else {
            cancelPicChoose();
        }
    }

    /**
     * usb连接
     *
     * @param usbDevice
     */
    private void usbConn(UsbDevice usbDevice) {
        new DeviceConnFactoryManager.Build(this)
                .setId(id)
                .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.USB)
                .setUsbDevice(usbDevice)
                .build();
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
    }

    /**
     * 打印标签
     */
    void sendLabel(String label) {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, "蓝牙打印机未连接");
            startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);
            return;
        }
        loadDefaultSetting();
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸，按照实际尺寸设置
        tsc.addSize(printWidth, printHeight);
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addGap(printPaperSpace);
        // 设置打印方向
        tsc.addDirection(printDirection, printMirror);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(pointX, pointY);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        // 清除打印缓冲区
//        tsc.addCls();
        // 绘制简体中文
        tsc.addText(0, 0, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, printRotation, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                label);
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响

        tsc.addSound(2, 100);
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, "蓝牙打印机未连接");
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
        if (printerAutoClose == 1) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
        }
    }

    /**
     * 打印条码
     */
    void sendOneCode(String label) {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, "蓝牙打印机未连接");
            startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);
            return;
        }
        loadDefaultSetting();
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸，按照实际尺寸设置
        tsc.addSize(printWidth, printHeight);
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addGap(printPaperSpace);
        // 设置打印方向
        tsc.addDirection(printDirection, printMirror);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(pointX, pointY);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
//        // 清除打印缓冲区
//        tsc.addCls();
        // 绘制图片
//        Bitmap b = ZXingUtils.codeCreate(this, label, oneCodeWidth, oneCodeHeight);
//        tsc.addBitmap(0, 0, b.getWidth(), b);

        tsc.add1DBarcode(0, 0,
                LabelCommand.BARCODETYPE.CODE128,
                oneCodeHeight,
                LabelCommand.READABEL.EANBEL,
                printRotation,
                label);
        // 打印标签
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响

        tsc.addSound(2, 100);
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, "蓝牙打印机未连接");
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
        if (printerAutoClose == 1) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
        }
    }

    /**
     * 打印二维码
     */
    void sendTwoCode(String content) {
        JSONArray objs = null;
        Log.d(TAG, "sendTwoCode content = " + content);
        try {
            objs = new JSONArray(content);
        } catch (JSONException e) {
            toastMessage("二维码内容错误：\n" + e.getMessage());
            e.printStackTrace();
            return;
        }
        if (objs.length() <= 0) {
            toastMessage("未收到打印内容");
            return;
        }

        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, "蓝牙打印机未连接");
            startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);
            return;
        }
        loadDefaultSetting();
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸，按照实际尺寸设置
        tsc.addSize(printWidth, printHeight);
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addGap(printPaperSpace);
        // 设置打印方向
        tsc.addDirection(printDirection, printMirror);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(pointX, pointY);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        for (int i = 0; i < objs.length(); i++) {
            //        // 清除打印缓冲区
            tsc.addCls();
            JSONObject obj = objs.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            String code = obj.optString("content");
            if (code == null || code.length() <= 0) {
                continue;
            }
            Bitmap b = ZXingUtils.createQRImage(code, twoCodeWidth, twoCodeHeight);
            tsc.addBitmap(0, 0, LabelCommand.BITMAP_MODE.OVERWRITE, twoCodeWidth, b);

            JSONArray lables = obj.optJSONArray("labels");

            if (lables != null && lables.length() > 0) {
                int space = 2;
                int linePosY = twoCodeHeight + space;
                int lineHeight = 24;
                for (int txtPos = 0; txtPos < lables.length(); txtPos++) {
                    tsc.addText(
                            0,
                            linePosY,
                            LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE,
                            printRotation,
                            LabelCommand.FONTMUL.MUL_1,
                            LabelCommand.FONTMUL.MUL_1,
                            lables.optString(txtPos)
                    );
                    linePosY += lineHeight + space;
                }
            }
            tsc.addPrint(1, 1);
        }
        // 绘制图片


//        tsc.add1DBarcode(0, 0,
//                LabelCommand.BARCODETYPE.CODE128,
//                oneCodeHeight,
//                LabelCommand.READABEL.EANBEL,
//                printRotation,
//                label);
        //tsc.addQRCode();
        // 打印标签
        // 打印标签后 蜂鸣器响

        tsc.addSound(2, 100);
        //产生钱箱信号
//        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            Utils.toast(this, "蓝牙打印机未连接");
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
        if (printerAutoClose == 1) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
        }
    }

    /**
     * 发送票据
     */
    void sendReceiptWithResponse() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        // 设置打印居中
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        // 设置为倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
        // 打印文字
        esc.addText("Sample\n");
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        // 取消倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        // 打印文字
        esc.addText("Print text\n");
        // 打印文字
        esc.addText("Welcome to use SMARNET printer!\n");

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        esc.addText(message, "GB2312");
        esc.addPrintAndLineFeed();

        /* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("智汇");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("网络");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("设备");
        esc.addPrintAndLineFeed();

        /* 打印图片 */
        // 打印文字
        esc.addText("Print bitmap!\n");
        Bitmap b = BitmapFactory.decodeResource(getResources(),
                R.mipmap.gprinter);
        // 打印图片
        esc.addOriginRastBitImage(b, 384, 0);

        /* 打印一维条码 */
        // 打印文字
        esc.addText("Print code128\n");
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
        // 设置条码可识别字符位置在条码下方
        // 设置条码高度为60点
        esc.addSetBarcodeHeight((byte) 60);
        // 设置条码单元宽度为1
        esc.addSetBarcodeWidth((byte) 1);
        // 打印Code128码
        esc.addCODE128(esc.genCodeB("SMARNET"));
        esc.addPrintAndLineFeed();

        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        // 打印文字
        esc.addText("Print QRcode\n");
        // 设置纠错等级
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
        // 设置qrcode模块大小
        esc.addSelectSizeOfModuleForQRCode((byte) 3);
        // 设置qrcode内容
        esc.addStoreQRCodeData("www.smarnet.cc");
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        //打印文字
        esc.addText("Completed!\r\n");

        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);
        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播
        esc.addQueryPrinterStatus();
        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                System.out.println("permission ok for device " + device);
                                usbConn(device);
                            }
                        } else {
                            System.out.println("permission denied for device " + device);
                        }
                    }
                    break;
                //Usb连接断开、蓝牙连接断开广播
                case ACTION_USB_DEVICE_DETACHED:
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    mHandler.obtainMessage(CONN_STATE_DISCONN).sendToTarget();
                    break;
                case DeviceConnFactoryManager.ACTION_CONN_STATE:
                    int state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1);
                    int deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1);
                    switch (state) {
                        case DeviceConnFactoryManager.CONN_STATE_DISCONNECT:
                            if (id == deviceId) {
                                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
                                }
                                tvConnState.setText(getString(R.string.str_conn_state_disconnect));
                                toastMessage(getString(R.string.str_conn_state_disconnect));
                            } else {
                                toastMessage("非本设备");
                            }
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
                            mConnectingDialog = new ProgressDialog(MainActivity.this);
                            mConnectingDialog.setTitle(R.string.connecting);
                            mConnectingDialog.setCancelable(false);
                            mConnectingDialog.show();
                            tvConnState.setText(getString(R.string.str_conn_state_connecting));
                            toastMessage(getString(R.string.str_conn_state_connecting));
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
                            if (mConnectingDialog != null) {
                                mConnectingDialog.dismiss();
                            }
                            tvConnState.setText(getString(R.string.str_conn_state_connected) + "\n" + getConnDeviceInfo());
                            toastMessage(getString(R.string.str_conn_state_connected) + "\n" + getConnDeviceInfo());
                            SharePreferenceUtils.putString(MainActivity.this, "BluetoothDevName", devName);
                            SharePreferenceUtils.putString(MainActivity.this, "BluetoothAddress", macAddress);
                            break;
                        case CONN_STATE_FAILED:
                            if (mConnectingDialog != null) {
                                mConnectingDialog.dismiss();
                            }
                            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
                            }
                            toastMessage(getString(R.string.str_conn_fail));
                            tvConnState.setText(getString(R.string.str_conn_state_disconnect));
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_QUERY_PRINTER_STATE:
                    if (counts > 0) {
                        sendContinuityPrint();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONN_STATE_DISCONN:
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null) {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
                    }
                    break;
                case PRINTER_COMMAND_ERROR:
                    toastMessage(getString(R.string.str_choice_printer_command));
                    break;
                case CONN_PRINTER:
                    toastMessage(getString(R.string.str_cann_printer));
                    break;
                case MESSAGE_UPDATE_PARAMETER:
                    String strIp = msg.getData().getString("Ip");
                    String strPort = msg.getData().getString("Port");
                    //初始化端口信息
                    new DeviceConnFactoryManager.Build(MainActivity.this)
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            //设置端口IP地址
                            .setIp(strIp)
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(Integer.parseInt(strPort))
                            .build();
                    threadPool = ThreadPool.getInstantiation();
                    threadPool.addTask(new Runnable() {
                        @Override
                        public void run() {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
        DeviceConnFactoryManager.closeAllPort();
        if (threadPool != null) {
            threadPool.stopThreadPool();
        }
        EventBus.getDefault().unregister(this);
    }

    public String getConnDeviceInfo() {
        String str = "";
        DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
        if (deviceConnFactoryManager != null
                && deviceConnFactoryManager.getConnState()) {
            if ("USB".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "USB\n";
                str += "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
            } else if ("WIFI".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "WIFI\n";
                str += "IP: " + deviceConnFactoryManager.getIp() + "\t";
                str += "Port: " + deviceConnFactoryManager.getPort();
            } else if ("BLUETOOTH".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "BLUETOOTH\n";
                String _strName = deviceConnFactoryManager.getDeviceName();
                if (_strName.length() > 0) {
                    str += "DeviceName:" + _strName + "\n";
                } else {
                    str += "DeviceName:" + "\n";
                }
                str += "MacAddress: " + deviceConnFactoryManager.getMacAddress();
            } else if ("SERIAL_PORT".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "SERIAL_PORT\n";
                str += "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
                str += "Baudrate: " + deviceConnFactoryManager.getBaudrate();
            }
        }
        return str;
    }

    /**
     * 断开打印机
     *
     * @param eventMessage
     */
    public void bluetoothDisConnect(EventMessage eventMessage) {
        if (eventMessage.getType() == EventMessage.PRINTER_DISCONN) {
            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                toastMessage("蓝牙打印机未连接");
                return;
            }
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
            toastMessage("蓝牙打印机已断开");
        }
    }

    public void printerDisConn() {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
            toastMessage("蓝牙打印机未连接");
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
        toastMessage("蓝牙打印机已断开");
    }

    /**
     * Toast 消息
     *
     * @param message
     */
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * EventBus消息 打开蓝牙设置
     */
    @Subscriber
    public void busConnectBluetooth(EventMessage eventMessage) {
        if (eventMessage.getType() == EventMessage.PRINTER_CONNECT) {
            startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);
        }
    }

    /**
     * EventBus消息 打印标签
     */
    @Subscriber
    public void busPrinterLabel(EventMessage eventMessage) {
        if (eventMessage.getType() == EventMessage.PRINTER_LABEL) {
            sendLabel(eventMessage.getLabel());
        }
    }

    /**
     * EventBus消息 打印一维码
     */
    @Subscriber
    public void busPrinterOneCode(EventMessage eventMessage) {
        if (eventMessage.getType() == EventMessage.PRINTER_ONE_CODE) {
            sendOneCode(eventMessage.getLabel());
        }
    }

    /**
     * EventBus消息
     * PRINTER_TWO_CODE 打印二维码
     * PRINTER_SETTING  打印机属性设置
     * FACE_DATA_DOWNLOAD_SUCCESS  人脸数据下载成功
     * FACE_DATA_DOWNLOAD_FAIL  人脸数据下载失败
     */
    @Subscriber
    public void busPrinterTwoCode(EventMessage eventMessage) {
        int type = eventMessage.getType();
        switch (type) {
            case EventMessage.PRINTER_TWO_CODE:
                sendTwoCode(eventMessage.getLabel());
                break;
            case EventMessage.PRINTER_SETTING:
                Intent intent = new Intent(this, PrinterSettingActivity.class);
                startActivity(intent);
                break;
            case EventMessage.FACE_LOGIN_FAIL:
                webView.loadUrl("javascript:faceCheckBack(0,'')");
                break;
            case EventMessage.FACE_LOGIN_SUCCESS:
                webView.loadUrl("javascript:faceCheckBack(1,'" + eventMessage.getLabel() + "')");
                break;
            case EventMessage.FACE_DATA_DOWNLOAD_SUCCESS:
                toastMessage("人脸数据下载成功！");
                break;
            case EventMessage.FACE_DATA_DOWNLOAD_FAIL:
                toastMessage("人脸数据下载失败！");
                break;
            default:
                break;
        }
    }

    @Subscriber
    public void testSendPost(TestMessage message) {
        String registerUrl = SharePreferenceUtils.getString(this, TAG_FACE_REGISTER, DEFAULT_FACE_REGISTER_URL)
                + "/api/User/Faceaddorupdate";
        final JSONObject jsonObject = new JSONObject();
        String faceData = MyService.replaceBlank(message.getPhotoData());
        try {
            jsonObject.put("facefeature", message.getFaceData());
            jsonObject.put("facephoto", faceData);
            jsonObject.put("userid", message.getUserCode());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String postData = "userinfo=" + jsonObject.toString();
        webView.postUrl(registerUrl, EncodingUtils.getBytes(postData, "BASE64"));
    }

    public void appExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("是否退出");
        builder.setPositiveButton(
                "确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ifExit = true;
                        finish();
                    }
                }
        );
        builder.setNegativeButton(
                "取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}