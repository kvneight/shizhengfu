package com.gpsdk.demo.face;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.gpsdk.demo.R;
import com.gpsdk.demo.db.FaceData;
import com.gpsdk.demo.message.EventMessage;
import com.gpsdk.demo.utils.BaseActivity;
import com.gpsdk.demo.utils.FACE_DATA;
import com.gpsdk.demo.utils.FaceUtils;
import com.gpsdk.demo.utils.SecurityUtil;
import com.gpsdk.demo.utils.SharePreferenceUtils;

import org.litepal.LitePal;
import org.simple.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_ALL_OUT;
import static com.arcsoft.face.enums.DetectMode.ASF_DETECT_MODE_VIDEO;

public class FaceLoginActivity extends BaseActivity {
    private static final String TAG = "FaceLoginActivity";
    private int MIN_FACE_WIDTH_PIXEL = 250;
    private FaceEngine mFaceEngine;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera camera;
    private TextView textTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.activity_face_login, null);
        setContentView(view);
        initView(view);
        activeFaceEngine();
        initVideoEngine(1);
        MIN_FACE_WIDTH_PIXEL = SharePreferenceUtils.getInt(getApplicationContext(), SharePreferenceUtils.SP_MIN_FACE_WIDTH, 250);
    }

    /**
     * 初始化控件
     *
     * @param view
     */
    private void initView(View view) {
        textTitle = (TextView) view.findViewById(R.id.text_view_face_title);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surFaceView);
        final ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (camera == null) {
                    int cameraIndex = 0;
                    int numberOfCameras = Camera.getNumberOfCameras();
                    if (numberOfCameras > 1) {
                        cameraIndex = 1;
                    }
                    camera = Camera.open(cameraIndex);
                    //camera.setDisplayOrientation(0);
                    setCameraDisplayOrientation(cameraIndex, camera);
                    //获取camera预览尺寸
                    Camera.Size size = getCameraBestSize(camera);
                    lp.height = size.width;
                    lp.width = size.height;
                    mSurfaceView.setLayoutParams(lp);
                }
                //开启预览
                camera.startPreview();
                try {
                    camera.setPreviewDisplay(mSurfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startFaceRecognition();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (camera != null) {
                    //停止预览
                    camera.stopPreview();
                    camera.setPreviewCallback(null);
                    //释放资源
                    camera.release();
                    camera = null;
                }
            }
        });
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 获取Camera 预览分辨率
     *
     * @param mCamera
     * @return
     */
    private Camera.Size getCameraBestSize(Camera mCamera) {
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        int count = sizeList.size();
        int index = 0;
        for (int x = 0; x < count; x++) {
            Camera.Size size = sizeList.get(x);
            int width = size.width;
            int height = size.height;
            if (width < 800) {
                index = x;
                break;
            }
            Log.i(TAG, "width=" + width + "  height=" + height);
        }
        Camera.Parameters param = mCamera.getParameters();
        param.setPreviewSize(sizeList.get(index).width, sizeList.get(index).height);
        mCamera.setParameters(param);
        return sizeList.get(index);
    }

    /**
     * 激活人脸引擎
     */
    private void activeFaceEngine() {
        mFaceEngine = new FaceEngine();
        boolean isActive = SharePreferenceUtils.getBoolean(this, Constants.SP_FACE_ENGINE_ACTIVIED, false);
        if (!isActive) {
            int activeCode = mFaceEngine.active(this,
                    Constants.APP_ID,
                    Constants.SDK_KEY);
            if (activeCode == ErrorInfo.MOK) {
                Log.i(TAG, "人脸引擎激活成功");
                SharePreferenceUtils.putBoolean(this, Constants.SP_FACE_ENGINE_ACTIVIED, true);
            } else {
                Log.i(TAG, "人脸引擎激活失败 " + activeCode);
                toastMessage("人脸引擎激活失败 " + activeCode);
            }
        }
    }

    /**
     * 初始化图片识别引擎（视频、拍照）
     * 调用FaceEngine的init方法初始化SDK，初始化成功后才能进一步使用SDK的功能。
     *
     * @param detectFaceMaxNum 最大检测人数
     */
    public boolean initVideoEngine(int detectFaceMaxNum) {
        int faceEngineCode = mFaceEngine.init(this,
                ASF_DETECT_MODE_VIDEO,
                ASF_OP_ALL_OUT,
                16,
                detectFaceMaxNum,
                FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS);
        if (faceEngineCode == ErrorInfo.MOK) {
            Log.i(TAG, "人脸引擎初始化成功");
        } else {
            Log.i(TAG, "人脸引擎初始化失败 " + faceEngineCode);
        }
        return faceEngineCode == ErrorInfo.MOK;
    }

    /**
     * 开始人脸识别
     */
    private boolean isStartCheckFace = true;

    public void startFaceRecognition() {
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                if (camera == null) return;
                if (isStartCheckFace == false) return;
                try {
                    isStartCheckFace = false;
                    byte[] myByte = bytes.clone();
                    //相机预览获取到的data数据不能直接转为bitmap存储，因为该数据是YUV格式的，需要进行数据转换
                    Camera.Size size = camera.getParameters().getPreviewSize();//获得预览图像设置的尺寸
                    YuvImage img = new YuvImage(myByte, ImageFormat.NV21, size.width, size.height, null);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    img.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                    stream.close();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(100);
                                Message message = handler.obtainMessage();
                                message.obj = bitmap;
                                handler.sendMessage(message);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();


                } catch (Exception ex) {
                    Log.e("Camera PreviewFrame", "Error:" + ex.getMessage());
                }
            }
        });
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bitmap bitmap = (Bitmap) msg.obj;
            if (mFaceEngine == null) {
                Log.e(TAG, "mFaceEngine = null");
                return;
            }
            if (bitmap == null) {
                isStartCheckFace = true;
                Log.i(TAG, "handler bitMap = null");
                return;
            }
            int[] argbData = FaceUtils.getARGB(bitmap, bitmap.getWidth(), bitmap.getHeight());
            if (argbData == null) {
                isStartCheckFace = true;
                Log.i(TAG, "handler argbData = null");
                return;
            }
            byte[] nv21data = FaceUtils.argbToNV21(argbData, bitmap.getWidth(), bitmap.getHeight());
            List<FACE_DATA> faceList = FaceUtils.detectFaces(mFaceEngine, nv21data, bitmap.getWidth(), bitmap.getHeight());
            if (faceList == null || faceList.size() == 0) {
                textTitle.setText("未检测到人脸");
                isStartCheckFace = true;
                return;
            }
            FACE_DATA rawData = null;
            int maxSize = 0;
            if (faceList.size() == 1) {
                rawData = faceList.get(0);
            } else {
                //找出面积最大的脸
                for (FACE_DATA f : faceList) {
                    if (f.getSize() > maxSize) {
                        maxSize = f.getSize();
                        rawData = f;
                    }
                }
            }
            if (rawData == null) {
                Log.d(TAG, "Not found faces");
                isStartCheckFace = true;
                return;
            }
            int w = rawData.getRect().right - rawData.getRect().left;
            int h = rawData.getRect().bottom - rawData.getRect().top;

            if (w < MIN_FACE_WIDTH_PIXEL || h < MIN_FACE_WIDTH_PIXEL) {
                Log.d(TAG, "Too small face, w= " + w + ", h=" + h);
                isStartCheckFace = true;
                return;
            }
            //直到此处，则证明检测到人脸

            textTitle.setText("人脸识别成功！");

            FaceFeature faceFeature = new FaceFeature();
            mFaceEngine.extractFaceFeature(nv21data, bitmap.getWidth(), bitmap.getHeight(), rawData.getFormat(), rawData.getFaceInfo(), faceFeature);

            float acc = 0.8f;//人脸识别灵敏度
            List<FaceData> faceDataList = LitePal.where("isDel = 0").find(FaceData.class);
            if (faceDataList.size() <= 0) {
                toastMessage("人脸数据库为空，请先下载人脸数据");
                return;
            }
            SparseArray<byte[]> sparseArray = new SparseArray<>();
            for (int x = 0; x < faceDataList.size(); x++) {
                FaceData faceData = faceDataList.get(x);
                byte[] b = SecurityUtil.stringToByteArray(faceData.getFaceFeature());
                sparseArray.put((faceData.getId() + 1), b);
            }
            int uid = FaceUtils.compareFace(mFaceEngine, rawData, sparseArray, acc);
            //判断是否识别成功
            if (uid > 0) {
                FaceData faceData = LitePal.where("id = " + (uid - 1)).findFirst(FaceData.class);
                EventMessage message = new EventMessage(EventMessage.FACE_LOGIN_SUCCESS, faceData.getUserId());
                EventBus.getDefault().post(message);
                finish();
            } else {
                EventMessage message = new EventMessage(EventMessage.FACE_LOGIN_FAIL);
                EventBus.getDefault().post(message);
                finish();
            }

        }
    };

    /**
     * 关闭当前界面
     *
     * @param view
     */
    public void exit(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isStartCheckFace = false;
        if (camera != null) {
            //停止预览
            camera.stopPreview();
            camera.setPreviewCallback(null);
            //释放资源
            camera.release();
            camera = null;
        }
        if (mFaceEngine != null) {
            //卸载FaceEngine 如不卸载打开次数过多会导致内存溢出
            int err = mFaceEngine.unInit();
            Log.d(TAG, "mFaceEngine uninit = " + err);
            mFaceEngine = null;
        }
    }
}
