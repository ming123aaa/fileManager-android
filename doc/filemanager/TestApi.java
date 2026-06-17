package com.example.filemanager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@RestController
@RequestMapping("test")
public class TestApi {

    @RequestMapping("connect")
    public String canConnect() {
        return "成功";
    }

    @RequestMapping("agent")
    public String agent(@RequestParam("url") String u){
        try {
            //先创建出了一个URL对象，urlPath：是我们访问接口地址
            URL url=new URL(u);

            //URL链接对象，通过URL对象打开一个connection链接对像
            HttpURLConnection urlConnection=(HttpURLConnection) url.openConnection();
            //设置urlConnection对象链接超时
            urlConnection.setConnectTimeout(5000);
            //设置urlConnection对象获取数据超时
            urlConnection.setReadTimeout(5000);
            //设置本次urlConnection请求方式
            urlConnection.setRequestMethod("GET");
            //调用urlConnection的链接方法，线程等待，等待的是服务器所给我们返回的结果集
            urlConnection.connect();
            //获取本次网络请求的状态码
            int code=urlConnection.getResponseCode();
            //如果本次返回的状态吗是200（成功）
            if (code==200) {
                //调用urlConnection.getInputStream得到本次请求所返回的结果流
                InputStream inputStream=urlConnection.getInputStream();
                //创建一个BufferedReader，去读取结果流
                BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
                String readLine;
                StringBuffer buffer=new StringBuffer();
                while ((readLine=reader.readLine())!=null) {
                    buffer.append(readLine);

                }
                //读取完结果流之后所得到的结果
                String result=buffer.toString();
               return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "错误";
    }
}
