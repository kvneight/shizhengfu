package com.gpsdk.demo.utils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class BaseActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;

    /**
     * 显示对话框
     *
     * @param title
     * @param message
     */
    public void showDialog(String title, String message, boolean isCancel, String strOK, String strCancel, final DialogCallBack dialogCallBack) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(isCancel);
        if (strOK != null) {
            builder.setPositiveButton(strOK, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (dialogCallBack != null) {
                        dialogCallBack.Ok();
                    }
                }
            });
        }
        if (strCancel != null) {
            builder.setNegativeButton(strCancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (dialogCallBack != null) {
                        dialogCallBack.Cancel();
                    }
                }
            });
        }
        builder.show();
    }

    /**
     * Dialog对话框接口
     */
    public interface DialogCallBack {
        void Ok();

        void Cancel();
    }

    /**
     * Toast消息
     *
     * @param message
     */
    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示等待对话框
     */
    public void showWaitDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("");
        progressDialog.setMessage("请稍候...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        progressDialog.dismiss();
    }

    /**
     * 关闭等待对话框
     */
    public void dismissWaitDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
