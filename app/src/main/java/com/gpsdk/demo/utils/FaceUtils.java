package com.gpsdk.demo.utils;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import android.util.SparseArray;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.enums.CompareModel;
import com.arcsoft.face.enums.DetectModel;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.arcsoft.face.FaceEngine.CP_PAF_NV21;

public class FaceUtils {

    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;
    private static final String TAG = "FaceUtils";


    static synchronized public List<FACE_DATA> detectFaces(FaceEngine faceEngine, byte[] data, int width, int height) {

        List<FaceInfo> faceInfoList = new ArrayList<>();
        //输入的 data 数据为 NV21 格式（如 Camera 里 NV21 格式的 preview数据），其中 height 不能为奇
        //        数，人脸检测返回结果保存在 result。
        int detectCode = faceEngine.detectFaces(data, width, height, CP_PAF_NV21,
                DetectModel.RGB, faceInfoList);
        if (detectCode != ErrorInfo.MOK) {
            Log.i(TAG, "detectFaces failed dectectCode = " + detectCode);
            return null;
        }
        StringBuilder sb = new StringBuilder();

        if (faceInfoList.size() <= 0) {
//            Log.d(TAG, "Detect no faces");
            return null;
        }

        List<FACE_DATA> list = new ArrayList<>();
        for (FaceInfo face : faceInfoList) {
            Log.d("com.arcsoft", "Face:" + face.toString());
            sb.append(face.toString()).append("\n");
            Rect rect = face.getRect();
            FACE_DATA d = new FACE_DATA(data, rect, CP_PAF_NV21, face.getOrient(), width, height, face);
            list.add(d);
        }

        return list;
    }

    private static class Score {
        int key;
        float score;

        Score(int key, float score) {
            this.key = key;
            this.score = score;
        }
    }

    public static int compareFace(FaceEngine mFaceCompEngine, FACE_DATA rawData,
                                  SparseArray<byte[]> faceInDb, float acc) {

        if (faceInDb.size() == 0) return 0;

        FaceFeature faceData = new FaceFeature();
        int error = mFaceCompEngine.extractFaceFeature(rawData.getData(),
                rawData.getWidth(), rawData.getHeight(),
                rawData.getFormat(), rawData.getFaceInfo(), faceData);
        Log.d("com.arcsoft", "ExtractFRFeature error=" + error);

        Score[] scoreMap = new Score[faceInDb.size()];
        int mainFaceCount = 0;
        int width = rawData.getWidth();
        int height = rawData.getHeight();

        FaceFeature facefeatureInDB = new FaceFeature();

        // 获取所有键值对对象的集合
        for (int i = 0; i < faceInDb.size(); i++) {
            facefeatureInDB.setFeatureData(faceInDb.valueAt(i));
            float score = faceCompare(mFaceCompEngine, facefeatureInDB, faceData, width, height);
            scoreMap[mainFaceCount++] = new Score(faceInDb.keyAt(i), score);
        }


        final int max = 200;

        Score[] list; // 得分最高的人，不超过max个
        float topScore;
        int topUser;

        if (mainFaceCount > max) {
            int index = 0;
            list = new Score[max];
            int length = mainFaceCount; // index是Face主脸有数据的个数
            //选出得分最高
            for (int i = 0; i < max; i++) {
                int topIndex = 0; //本轮中得分最高的下标
                for (int j = 1; j < length; j++) {
                    if (scoreMap[j].score > scoreMap[topIndex].score) {
                        topIndex = j;
                    }
                }

                // 得分最高的放到list里
                list[index++] = scoreMap[topIndex];
                //scoreMap 里删除得分最高的这个人
                System.arraycopy(scoreMap, topIndex + 1, scoreMap, topIndex, length - topIndex - 1);
                length--;
            }
            //找出最高分的人和分数
            topScore = list[0].score;
            topUser = list[0].key;
        } else {
            list = scoreMap;
            //找出最高分的人和分数
            topScore = list[0].score;
            topUser = list[0].key;
            for (int i = 1; i < scoreMap.length; i++) {
                if (scoreMap[i].score > topScore) {
                    topScore = scoreMap[i].score;
                    topUser = scoreMap[i].key;
                }
            }
        }
        return topScore > acc ? topUser : 0;
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    public static byte[] getDataFromImage(Image image, byte[] data, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }


    /**
     * Bitmap转化为ARGB数据，再转化为NV21数据
     *
     * @param src    传入的Bitmap，格式为{@link Bitmap.Config#ARGB_8888}
     * @param width  NV21图像的宽度
     * @param height NV21图像的高度
     * @return nv21数据
     */
    //避免反复申请内存，一直使用一个内存块
    private static byte[] gNV21Data = new byte[1];

    public static byte[] argbToNV21(int[] data, int width, int height) {
        if (data != null && data.length >= width * height) {

//            if (gNV21Data.length < width * height * 3 / 2) {
//                gNV21Data = new byte[width * height * 3 / 2];
//            }
//
//            if (!argbToNv21(data, gNV21Data, width, height)) {
//                Log.e(TAG, "argbToNv21 failed!");
//            }
//            return gNV21Data;
            byte[] nv21data = new byte[width * height * 3 / 2];
            if (!argbToNv21(data, nv21data, width, height)) {
                Log.e(TAG, "argbToNv21 failed!");
            }
            return nv21data;

        } else {
            return null;
        }
    }

    /**
     * ARGB数据转化为NV21数据
     *
     * @param argb   argb数据
     * @param width  宽度
     * @param height 高度
     * @return nv21数据
     */
    private static boolean argbToNv21(int[] argb, byte[] nv21, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        int nv21Len = width * height * 3 / 2;
        if (nv21.length < nv21Len) return false;

        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0x00FF00) >> 8;
                int B = argb[index] & 0x0000FF;
                int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
                int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
                int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21Len - 2) {
                    nv21[uvIndex++] = (byte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                    nv21[uvIndex++] = (byte) (U < 0 ? 0 : (U > 255 ? 255 : U));
                }

                ++index;
            }
        }
        return true;
    }

    /**
     * bitmap转化为bgr数据，格式为{@link Bitmap.Config#ARGB_8888}
     *
     * @param image 传入的bitmap
     * @return bgr数据
     */
    public static byte[] bitmapToBgr(Bitmap image) {
        if (image == null) {
            return null;
        }
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);
        byte[] temp = buffer.array();
        byte[] pixels = new byte[(temp.length / 4) * 3];
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[i * 3] = temp[i * 4 + 2];
            pixels[i * 3 + 1] = temp[i * 4 + 1];
            pixels[i * 3 + 2] = temp[i * 4];
        }
        return pixels;
    }

    public static float faceCompare(FaceEngine faceEngine, FaceFeature face1, FaceFeature face2, int width,
                                    int height) {
        //输入的data数据为NV21格式（如Camera里NV21格式的preview数据）；人脸坐标一般使用人脸检测返回的Rect传入；人脸角度请按照人脸检测引擎返回的值传入。
        FaceSimilar matching = new FaceSimilar();
        //比对两个人脸特征获取相似度信息
        faceEngine.compareFaceFeature(face1, face2, CompareModel.LIFE_PHOTO, matching);
        return matching.getScore();
    }

    //避免反复申请内存，一直使用一个内存块
    private static int[] gARGBArr = new int[1];

    public static int[] getARGB(Bitmap src, int width, int height) {
        if (gARGBArr.length < width * height) {
            gARGBArr = new int[width * height];
        }
        try {
            src.getPixels(gARGBArr, 0, width, 0, 0, width, height);
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return gARGBArr;
    }
}
