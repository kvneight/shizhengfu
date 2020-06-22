package com.gpsdk.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;


public class QRCode extends AppCompatActivity {
    EditText sleCode;
    Context mContext;
    ImageView imgCode;
    int CodeWidth = 800;
    int CodeHeight = 300;
    int WHITE = 0xFFFFFFFF;
    int BLACK = 0xFF000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        initView();
    }

    protected void initView() {
        sleCode = (EditText) findViewById(R.id.sleCode);
        mContext = this;
        imgCode = (ImageView) findViewById(R.id.imgCode);

        Intent intent = getIntent();
        String strMessage = intent.getStringExtra("code");
        sleCode.setText(strMessage);
        Button btnQRDraw = (Button) findViewById(R.id.btnQRDraw);
        btnQRDraw.setOnClickListener(new ButtonClickListener());

        imgCode.setMaxWidth(CodeWidth);
        imgCode.setMaxHeight(CodeHeight);
    }

    class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View _sender) {
            switch (_sender.getId()) {
                case R.id.btnQRDraw:
                    String strCode = sleCode.getText().toString();
                    if (strCode == null || strCode.length() <= 0) {
                        Toast.makeText(mContext, "请指定要生成条形码的内容！", Toast.LENGTH_LONG).show();
                    }
                    codeCreate(strCode, CodeWidth, CodeHeight,270);
                    break;
                default:
                    break;
            }
        }
    }

    protected void codeCreate(String _code, int _width, int _height, int _rota) {
        int size = _code.length();
        for (int i = 0; i < size; i++) {
            int c = _code.charAt(i);
            if ((19968 <= c && c < 40623)) {
                Toast.makeText(mContext, "生成条形码的时刻不能是中文", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Bitmap bmp = null;
        try {
//                bmp = CreateOneDCode(_code,_width,_height);
            bmp = getBarcodeBitmap(_code, _width, _height);

            if (bmp != null) {
                if (_rota > 0 && _rota < 360){
                    int mWidth;
                    int mHeight;
                    Matrix bm = new Matrix(); //旋转图片 动作
                    bm.setRotate(270);//旋转角度
                    mWidth = bmp.getWidth();
                    mHeight = bmp.getHeight();
                    // 创建新的图片
                    Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, mWidth, mHeight, bm, true);

                    imgCode.setImageBitmap(resizedBitmap);
                }
                else{
                    imgCode.setImageBitmap(bmp);

                }
            }
        } catch (Exception err) {
            Toast.makeText(mContext, err.getMessage(), Toast.LENGTH_LONG).show();
            err.printStackTrace();
        }
    }

    protected Bitmap CreateOneDCode(String content, int _width, int _height) throws WriterException {
        // 生成一维条码,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, _width, _height);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 通过像素数组生成bitmap,具体参考api
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    /**
     * @param content  文本内容
     * @param qrWidth  条形码的宽度
     * @param qrHeight 条形码的高度
     * @return bitmap
     */
    protected Bitmap getBarcodeBitmap(String content, int qrWidth, int qrHeight) {
        content = content.trim();
        int length = content.length();
        int defaultTextSize = 10;
        int mHeight = qrHeight / 4;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix result;
            try {
                result = new MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, qrWidth, mHeight * 3, hints);
            } catch (IllegalArgumentException iae) {
                return null;
            }
            int width = result.getWidth();
            int height = result.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                }
            }
            Bitmap qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            qrBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            //大的bitmap
            Bitmap bigBitmap = Bitmap.createBitmap(width, qrHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bigBitmap);
            Rect srcRect = new Rect(0, 0, width, height);
            Rect dstRect = new Rect(0, 0, width, height);
            canvas.drawBitmap(qrBitmap, srcRect, dstRect, null);
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            p.setFilterBitmap(true);
            if(length >= 10){
                p.setTextSize(defaultTextSize);
                Rect bounds = new Rect();
                p.getTextBounds(content,0,content.length(),bounds);
                float desiredTextSize = defaultTextSize * width / bounds.width();
                p.setTextSize(desiredTextSize);
            }else{
                p.setTextSize(mHeight);
            }

            p.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fontMetrics=p.getFontMetrics();
            float distance=(fontMetrics.bottom - fontMetrics.top)/2 - fontMetrics.bottom;
            float baseline=(mHeight/2)+distance;

            canvas.translate(0, baseline);
            canvas.drawText(content, srcRect.centerX(), height, p);
            return bigBitmap;
        } catch (Exception e) {
            return null;
        }
    }

}
