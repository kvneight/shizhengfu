package com.gpsdk.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.gpsdk.demo.utils.SharePreferenceUtils;

public class PrinterSettingActivity extends AppCompatActivity {
    public static final String PRINTER_WIDTH = "printerWidth";
    public static final String PRINTER_HEIGHT = "printerHeight";
    public static final String PRINTER_PAPERSPACE = "printerPaperSpace";
    public static final String PRINTER_DIRECTION = "printerDirection";
    public static final String PRINTER_ROTATION = "printerRotation";
    public static final String PRINTER_MIRROR = "printerMIRROR";
    public static final String PRINTER_POINT_X = "printerPointX";
    public static final String PRINTER_POINT_Y = "printerPointY";
    public static final String PRINTER_ONE_CODE_WIDTH = "printOneCodeWidth";
    public static final String PRINTER_ONE_CODE_HEIGHT = "printOneCodeHeight";
    public static final String PRINTER_TWO_CODE_WIDTH = "printTwoCodeWidth";
    public static final String PRINTER_TWO_CODE_HEIGHT = "printTwoCodeHeight";
    public static final String PRINTER_AUTOCLOSE = "printerAutoClose";

    private EditText editTextWidth;
    private EditText editTextHeight;
    private EditText editPaperSpace;
    private Spinner spinnerDirection;
    private Spinner spinnerMirror;
    private Spinner spinnerRotation;
    private Spinner ddlbAutoClose;
    private EditText editPointX;
    private EditText editPointY;
    private EditText editOneCodeWidth;
    private EditText editOneCodeHeight;
    private EditText editTwoCodeWidth;
    private EditText editTwoCodeHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.activity_printer_setting, null);
        setContentView(view);
        initView(view);
    }

    /**
     * 初始化界面控件
     *
     * @param view
     */
    private void initView(View view) {
        editTextWidth = (EditText) view.findViewById(R.id.edit_text_width);
        editTextHeight = (EditText) view.findViewById(R.id.edit_text_height);
        editPaperSpace = (EditText) view.findViewById(R.id.slePaperSpace);
        spinnerDirection = (Spinner) view.findViewById(R.id.spinner_direction);
        spinnerMirror = (Spinner) view.findViewById(R.id.spinner_mirror);

        //加载数据
        String width = SharePreferenceUtils.getString(this, PRINTER_WIDTH);
        if (width == null || width.equals("")) {
            editTextWidth.setText(40 + "");
        } else {
            editTextWidth.setText(width);
        }
        String height = SharePreferenceUtils.getString(this, PRINTER_HEIGHT);
        if (height == null || height.equals("")) {
            editTextHeight.setText(50 + "");
        } else {
            editTextHeight.setText(height);
        }
        String strPaperSpace = SharePreferenceUtils.getString(this,PRINTER_PAPERSPACE);
        if (strPaperSpace == null || strPaperSpace.equals("")) {
            editPaperSpace.setText(2 + "");
        } else {
            editPaperSpace.setText(strPaperSpace);
        }

        int direction = SharePreferenceUtils.getInt(this, PRINTER_DIRECTION, 0);
        spinnerDirection.setSelection(direction);
        int mirror = SharePreferenceUtils.getInt(this, PRINTER_MIRROR, 0);
        spinnerMirror.setSelection(mirror);

        spinnerRotation = (Spinner) view.findViewById(R.id.spinner_rotation);
        int rotation = SharePreferenceUtils.getInt(this, PRINTER_ROTATION, 0);
        spinnerRotation.setSelection(rotation);

        ddlbAutoClose = (Spinner)view.findViewById(R.id.ddlbAutoClose);
        int autoClose = SharePreferenceUtils.getInt(this,PRINTER_AUTOCLOSE,0);
        ddlbAutoClose.setSelection(autoClose);

        editPointX = (EditText) view.findViewById(R.id.edit_text_point_x);
        int pointX = SharePreferenceUtils.getInt(this, PRINTER_POINT_X, 0);
        editPointX.setText(pointX + "");

        editPointY = (EditText) view.findViewById(R.id.edit_text_point_y);
        int pointY = SharePreferenceUtils.getInt(this, PRINTER_POINT_Y, 0);
        editPointY.setText(pointY + "");

        editOneCodeWidth = (EditText) view.findViewById(R.id.edit_text_one_width);
        int oneCodeWidth = SharePreferenceUtils.getInt(this, PRINTER_ONE_CODE_WIDTH, 40);
        editOneCodeWidth.setText(oneCodeWidth + "");

        editOneCodeHeight = (EditText) view.findViewById(R.id.edit_text_one_height);
        int oneCodeHeight = SharePreferenceUtils.getInt(this, PRINTER_ONE_CODE_HEIGHT, 80);
        editOneCodeHeight.setText(oneCodeHeight + "");

        editTwoCodeWidth = (EditText) view.findViewById(R.id.edit_text_two_width);
        int twoCodeWidth = SharePreferenceUtils.getInt(this, PRINTER_TWO_CODE_WIDTH, 150);
        editTwoCodeWidth.setText(twoCodeWidth + "");

        editTwoCodeHeight = (EditText) view.findViewById(R.id.edit_text_two_height);
        int twoCodeHeight = SharePreferenceUtils.getInt(this, PRINTER_TWO_CODE_HEIGHT, 150);
        editTwoCodeHeight.setText(twoCodeHeight + "");

    }

    /**
     * 保存按钮
     *
     * @param view
     */
    public void btnSave(View view) {
        String width = editTextWidth.getText().toString();
        String height = editTextHeight.getText().toString();
        String strPaperSpace = editPaperSpace.getText().toString();
        int spinnerDirectionIndex = spinnerDirection.getSelectedItemPosition();
        int spinnerMirrorIndex = spinnerMirror.getSelectedItemPosition();
        int spinnerRotationIndex = spinnerRotation.getSelectedItemPosition();
        int autoCloseIndex = ddlbAutoClose.getSelectedItemPosition();
        String pointX = editPointX.getText().toString();
        String pointY = editPointY.getText().toString();
        String oneCodeWidth = editOneCodeWidth.getText().toString();
        String oneCodeHeight = editOneCodeHeight.getText().toString();
        String twoCodeWidth = editTwoCodeWidth.getText().toString();
        String twoCodeHeight = editTwoCodeHeight.getText().toString();
        if (width.equals("") || height.equals("") || strPaperSpace.equals("") ||
                pointX.equals("") || pointY.equals("") ||
                oneCodeWidth.equals("") || oneCodeHeight.equals("") ||
                twoCodeWidth.equals("") || twoCodeHeight.equals("")) {
            Toast.makeText(this, "参数设置不完整，请重新设置！", Toast.LENGTH_SHORT).show();
            return;
        } else {
            SharePreferenceUtils.putString(this, PRINTER_WIDTH, width);
            SharePreferenceUtils.putString(this, PRINTER_HEIGHT, height);
            SharePreferenceUtils.putString(this, PRINTER_PAPERSPACE, strPaperSpace);
            SharePreferenceUtils.putInt(this, PRINTER_DIRECTION, spinnerDirectionIndex);
            SharePreferenceUtils.putInt(this, PRINTER_MIRROR, spinnerMirrorIndex);
            SharePreferenceUtils.putInt(this, PRINTER_ROTATION, spinnerRotationIndex);
            SharePreferenceUtils.putInt(this, PRINTER_POINT_X, Integer.parseInt(pointX));
            SharePreferenceUtils.putInt(this, PRINTER_POINT_Y, Integer.parseInt(pointY));
            SharePreferenceUtils.putInt(this, PRINTER_ONE_CODE_WIDTH, Integer.parseInt(oneCodeWidth));
            SharePreferenceUtils.putInt(this, PRINTER_ONE_CODE_HEIGHT, Integer.parseInt(oneCodeHeight));
            SharePreferenceUtils.putInt(this, PRINTER_TWO_CODE_WIDTH, Integer.parseInt(twoCodeWidth));
            SharePreferenceUtils.putInt(this, PRINTER_TWO_CODE_HEIGHT, Integer.parseInt(twoCodeHeight));
            SharePreferenceUtils.putInt(this, PRINTER_AUTOCLOSE, autoCloseIndex);
        }
        Toast.makeText(this, "数据保存成功！", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * 取消按钮
     *
     * @param view
     */
    public void btnCancel(View view) {
        finish();
    }
}
