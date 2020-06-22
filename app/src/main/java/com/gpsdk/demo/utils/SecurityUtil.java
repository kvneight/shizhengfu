package com.gpsdk.demo.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Administrator
 * @create 2019-03-20
 * @desc 安全工具类
 **/
public class SecurityUtil {
    public static final String KEY = "09bd821d3e764f44899a9dc6";
    public static final String IV = "2M9tOpWi";
    public static final String DEFAULT_ENC_NAME = "UTF-8";

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String java_openssl_encrypt(String data) {
        return java_openssl_encrypt(data, IV);
    }

    /**
     * java_openssl_encrypt加密算法
     *
     * @param data
     * @param iv
     * @return
     * @throws Exception
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String java_openssl_encrypt(String data, String iv) {
        try {
            Cipher cipher = createCipher(iv, Cipher.ENCRYPT_MODE);
            return URLEncoder.encode(Base64.encodeToString(cipher.doFinal(data.getBytes()),Base64.DEFAULT), DEFAULT_ENC_NAME);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String java_openssl_decrypt(String data) {
        return java_openssl_decrypt(data, IV);
    }

    /**
     * java_openssl_decrypt解密
     *
     * @param data
     * @param iv
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String java_openssl_decrypt(String data, String iv) {
        try {
            Cipher cipher = createCipher(iv, Cipher.DECRYPT_MODE);
            return new String(cipher.doFinal(android.util.Base64.decode(URLDecoder.decode(data, DEFAULT_ENC_NAME), Base64.DEFAULT)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建密码器Cipher
     *
     * @param iv
     * @param mode 加/解密模式
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    private static Cipher createCipher(String iv, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] key = KEY.getBytes();
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
        cipher.init(mode, new SecretKeySpec(key, "DESede"), ivParameterSpec);
        return cipher;
    }

    /**
     * Unicode转 汉字字符串
     *
     * @param str \u6728
     * @return '木' 26408
     */
    public static String unicodeToString(String str) {

        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            //group 6728
            String group = matcher.group(2);
            //ch:'木' 26408
            ch = (char) Integer.parseInt(group, 16);
            //group1 \u6728
            String group1 = matcher.group(1);
            str = str.replace(group1, ch + "");
        }
        return str;
    }

    /**
     * bitmap转base64
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = android.util.Base64.encodeToString(bitmapBytes, android.util.Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 将十六进制串转换为二进制
     */
    public static byte[] stringToByteArray(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 将二进制转换成十六进制字符串
     */
    public static String byteArrayToString(byte[] bt_ary) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bt_ary.length; i++) {
            String hex = Integer.toHexString(bt_ary[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 图片压缩
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 100;
        while (baos.toByteArray().length / 1024 > 500) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        //把压缩后的数据baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null); //把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /***
     * 等比例压缩图片
     *
     * @param bitmap
     * @param screenWidth
     * @param screenHight
     * @return
     */
    public static Bitmap getBitmapSize(Bitmap bitmap, int screenWidth, int screenHight) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Log.e("jj", "图片宽度" + w + ",screenWidth=" + screenWidth);
        Matrix matrix = new Matrix();
        float scale = (float) screenWidth / w;
        float scale2 = (float) screenHight / h;

        scale = scale < scale2 ? scale : scale2;

        // 保证图片不变形.
        matrix.postScale(scale, scale);
        // w,h是原图的属性.
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }
}
