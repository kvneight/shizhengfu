package com.gpsdk.demo.message;

public class EventMessage {
    public static final int PRINTER_CONNECT = 1001;//连接打印机
    public static final int PRINTER_DISCONN = 1002;//断开打印机
    public static final int PRINTER_CONNECT_SUCCESS = 1011;//连接打印机成功
    public static final int PRINTER_CONNECT_FAIL = 1012;//连接打印机失败
    public static final int PRINTER_LABEL = 1103;//打印标签
    public static final int PRINTER_SETTING = 1104;//打印机属性设置 纸张大小和方向等
    public static final int PRINTER_ONE_CODE = 1105;//打印一维码
    public static final int PRINTER_TWO_CODE = 1106;//打印二维码

    public static final int FACE_SAVE_SUCCESS = 2001;//保存人脸数据成功
    public static final int FACE_SAVE_FAIL = 2002;//保存人脸数据失败
    public static final int FACE_DATA_DOWNLOAD_SUCCESS = 2003;//人脸数据下载成功
    public static final int FACE_DATA_DOWNLOAD_FAIL = 2004;//人脸数据下载失败

    public static final int FACE_LOGIN_SUCCESS = 3001;//人脸登录成功
    public static final int FACE_LOGIN_FAIL = 3002;//人脸登录失败

    public static final int FACE_TEST_POST = 11111;//测试


    /**
     * 消息类型
     */
    private int type;

    private String label;


    public EventMessage(int type) {
        this.type = type;
    }

    public EventMessage(int type, String label) {
        this.type = type;
        this.label = label;
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

}
