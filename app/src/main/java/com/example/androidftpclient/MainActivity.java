package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private EditText username;
    private EditText password;
    private Button login;

    private FTPClient ftpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        username = findViewById(R.id.UserName);
        password = findViewById(R.id.Password);
        login = findViewById(R.id.Login);

        ftpClient = new FTPClient();

        //根据服务器配置设置字符编码
        ftpClient.setControlEncoding("UTF-8");

        //设置连接服务器超时时间
        ftpClient.setConnectTimeout(10000);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });






//        try {
//            //登录
//            ftpClient.login(this.username.getText().toString(), this.password.getText().toString());
//
//            //获取当前文件列表
//            FTPFile[] ftpFiles = ftpClient.listFiles();
//
//            //进入目录
//            ftpClient.changeWorkingDirectory("directory1");
//
//            //返回上层目录
//            ftpClient.changeToParentDirectory();
//
//            //新建文件夹
//            ftpClient.makeDirectory("myDirectory");
//
//            //删除文件
//            ftpClient.deleteFile("a.txt");
//
//            //重命名文件
//            ftpClient.rename("旧文件名.txt","新文件名.txt");
//            //aaaaaaaa
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void Login(){
        try {
            //登录
            ftpClient.connect("10.250.154.69",21);
            ftpClient.login(this.username.getText().toString(), this.password.getText().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int replyCode = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(MainActivity.this, "登录成功^_^", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(MainActivity.this, "登录失败>_<", Toast.LENGTH_SHORT).show();
        }
    }

}