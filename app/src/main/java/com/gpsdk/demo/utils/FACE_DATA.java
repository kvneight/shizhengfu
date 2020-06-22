package com.gpsdk.demo.utils;

import android.graphics.Rect;

import com.arcsoft.face.FaceInfo;

public class FACE_DATA {
    private byte[] data;
    private Rect rect;
    private int format;
    private int ori;
    private int width;
    private int height;
    private FaceInfo faceInfo;

    public FACE_DATA(byte[] data, Rect rect, int format, int ori, int width, int height, FaceInfo feature) {
        this.data = data;
        this.rect = rect;
        this.format = format;
        this.ori = ori;
        this.width = width;
        this.height = height;
        this.faceInfo = feature;
    }

    public FACE_DATA() {

    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = new Rect(rect);
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getOri() {
        return ori;
    }

    public void setOri(int ori) {
        this.ori = ori;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public FaceInfo getFaceInfo() {
        return faceInfo;
    }

    public void setFaceInfo(FaceInfo faceInfo) {
        this.faceInfo = faceInfo;
    }

    // 获取人脸的面积（单位：像素）
    public int getSize() {
        return (rect.right - rect.left) * (rect.bottom - rect.top);
    }
}
