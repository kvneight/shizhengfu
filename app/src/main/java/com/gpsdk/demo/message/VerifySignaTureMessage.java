package com.gpsdk.demo.message;

public class VerifySignaTureMessage {
    private int Errorcode;
    private String msg;
    private AcquireTokenMessage acquireTokenMessage;

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

    public AcquireTokenMessage getAcquireTokenMessage() {
        return acquireTokenMessage;
    }

    public void setAcquireTokenMessage(AcquireTokenMessage acquireTokenMessage) {
        this.acquireTokenMessage = acquireTokenMessage;
    }

    @Override
    public String toString() {
        return "VerifySignaTureMessage{" +
                "Errorcode=" + Errorcode +
                ", msg=" + msg +
                ", acquireTokenMessage=" + acquireTokenMessage +
                '}';
    }
}
