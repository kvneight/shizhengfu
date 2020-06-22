package com.gpsdk.demo.utils;


import android.util.Log;

import com.gpsdk.demo.service.MyService;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;


public class PostUtils {
    public static void testHttpUrlCon(String url, String facefeature, String facephoto, String userCode) {

        try {

            // 创建连接
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);
            // 设置参数，仿html表单提交
//            List<NameValuePair> paramList = new ArrayList<NameValuePair>();
//            BasicNameValuePair param1 = new BasicNameValuePair("userinfo", data);
//            paramList.add(param1);
            String s = "userinfo={\"facefeature\":" + facefeature + ",\"facephoto\":" + facephoto + ",\"userid\":" + userCode + "}";
            s = s+"&signature="+ MyService.header;
            post.setEntity(new StringEntity(s));
            // 发送HttpPost请求，并返回HttpResponse对象
            HttpResponse httpResponse = httpClient.execute(post);
            // 判断请求响应状态码，状态码为200表示服务端成功响应了客户端的请求
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                // 获取返回结果
                String result = EntityUtils.toString(httpResponse
                        .getEntity());
                System.out.println(result);
                Log.i("post888", SecurityUtil.unicodeToString(result));
            } else {
                Log.i("post888", "error");
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.i("post888", "error  " + e.getMessage());
        }
    }
}
