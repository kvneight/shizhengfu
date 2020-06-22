package com.gpsdk.demo.message;

public class AcquireTokenMessage {
    private int Errorcode;
    private String msg;
    private String signature;
    private long sxtime;
    private String appurl;
    private String appId;
    private String appName;
    private String appType;
    private long timeToken;

    public int getErrorcode() {
        return Errorcode;
    }

    public void setErrorcode(int errorcode) {
        Errorcode = errorcode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getSxtime() {
        return sxtime;
    }

    public void setSxtime(long sxtime) {
        this.sxtime = sxtime;
    }

    public String getAppurl() {
        return appurl;
    }

    public void setAppurl(String appurl) {
        this.appurl = appurl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public long getTimeToken() {
        return timeToken;
    }

    public void setTimeToken(long timeToken) {
        this.timeToken = timeToken;
    }

    @Override
    public String toString() {
        return "AcquireTokenMessage{" +
                "Errorcode=" + Errorcode +
                ", msg='" + msg + '\'' +
                ", signature='" + signature + '\'' +
                ", sxtime=" + sxtime +
                ", appurl='" + appurl + '\'' +
                ", appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                ", appType='" + appType + '\'' +
                ", timeToken=" + timeToken +
                '}';
    }
}
