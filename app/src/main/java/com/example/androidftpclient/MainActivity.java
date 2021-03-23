package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FTPClient ftpClient = new FTPClient();

        //根据服务器配置设置字符编码
        ftpClient.setControlEncoding("UTF-8");

        //设置连接服务器超时时间
        ftpClient.setConnectTimeout(10000);


        try {
            //登录
            ftpClient.login("username", "password");

            //获取当前文件列表
            FTPFile[] ftpFiles = ftpClient.listFiles();

            //进入目录
            ftpClient.changeWorkingDirectory("directory1");

            //返回上层目录
            ftpClient.changeToParentDirectory();

            //新建文件夹
            ftpClient.makeDirectory("myDirectory");

            //删除文件
            ftpClient.deleteFile("a.txt");

            //重命名文件
            ftpClient.rename("旧文件名.txt","新文件名.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}