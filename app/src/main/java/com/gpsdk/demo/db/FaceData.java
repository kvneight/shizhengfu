package com.gpsdk.demo.db;

import org.litepal.crud.LitePalSupport;

public class FaceData extends LitePalSupport {
    private int id;

    private String faceFeature;//人脸特征信息

    private String facePhoto;//人脸图片信息

    private long addupTime;

    private String userId;

    private int isDel;//0默认 1删除

    private int isUpload;//是否已经上传 0默认 1已上传

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFaceFeature() {
        return faceFeature;
    }

    public void setFaceFeature(String faceFeature) {
        this.faceFeature = faceFeature;
    }

    public String getFacePhoto() {
        return facePhoto;
    }

    public void setFacePhoto(String facePhoto) {
        this.facePhoto = facePhoto;
    }

    public long getAddupTime() {
        return addupTime;
    }

    public void setAddupTime(long addupTime) {
        this.addupTime = addupTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getIsDel() {
        return isDel;
    }

    public void setIsDel(int isDel) {
        this.isDel = isDel;
    }

    public int getIsUpload() {
        return isUpload;
    }

    public void setIsUpload(int isUpload) {
        this.isUpload = isUpload;
    }
}
