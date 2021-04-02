package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import com.example.androidftpclient.FileUtils;

import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private EditText username;
    private EditText password;
    private Button login;
    private Button btn_file;
    private Button btn_upload;
    private TextView tv;

    private ArrayList files;
    private FTPClient ftpClient;
    private FTPOperationProcessor FTPProcessor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        username = findViewById(R.id.UserName);
        password = findViewById(R.id.Password);
        login = findViewById(R.id.Login);
        btn_file= (Button) findViewById(R.id.btn_open);
        btn_upload = (Button) findViewById(R.id.btn_upload);

        tv = (TextView) findViewById(R.id.tv);
        files = new ArrayList<File>();

        requestReadExternalPermission();

        FTPProcessor = new FTPOperationProcessor();

        ftpClient = new FTPClient();
        //根据服务器配置设置字符编码
        ftpClient.setControlEncoding("UTF-8");

        //设置连接服务器超时时间
        ftpClient.setConnectTimeout(10000);


        btn_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");//匹配所有的类型
                //intent.setType(“image/*”);//选择图片
                //intent.setType(“audio/*”); //选择音频
                //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
                //intent.setType(“video/*;image/*”);//同时选择视频和图片
                chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//设置可以多选文件
                Intent intent = Intent.createChooser(chooseFile, "title");
                startActivityForResult(intent, 1);
            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FTPProcessor.uploadFile("/",(File)files.get(0),((File)files.get(0)).length());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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

    private void Login() {

//        try {
//            if(FTPProcessor.connect("10.250.154.69",21,this.username.getText().toString(),this.password.getText().toString())){
//                Toast.makeText(MainActivity.this, "登录成功^_^", Toast.LENGTH_SHORT).show();
//            }
//            else{
//                Toast.makeText(MainActivity.this, "登录失败>_<", Toast.LENGTH_SHORT).show();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //登录
                    ftpClient.connect("10.250.154.69",21);
//            ftpClient.login(this.username.getText().toString(), this.password.getText().toString());
                    ftpClient.login("john", "1234");
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
                    Looper.prepare();
                    Toast.makeText(MainActivity.this, "登录失败>_<", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }else {
                    Looper.prepare();
                    Toast.makeText(MainActivity.this, "登录成功^_^", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }
        }).start();

    }


    private String path;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1:
                    files.clear();

                    //使用ACTION_GET_CONTENT时 选择文件时多个调用用ClipData
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {

                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item itemAt = clipData.getItemAt(i);
                            String path1 = FileUtils.getPath(this, itemAt.getUri());//使用工具类对uri进行转化
                            files.add(new File(path1));
                            //File file = new File(path1);
                        }
                    }
                    //选择文件单个直接使用Uri
                    Uri uri = data.getData();
                    if (uri != null) {
                        String path1 = FileUtils.getPath(this, uri);//使用工具类对uri进行转化
                        files.add(new File(path1));
                        //File file = new File(path1);
                    }
                    break;
            }
        }
    }

    @SuppressLint("NewApi")
    private void requestReadExternalPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ permission IS NOT granted...");

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                Log.d(TAG, "11111111111111");
            } else {
                // 0 是自己定义的请求coude
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                Log.d(TAG, "222222222222");
            }
        } else {
            Log.d(TAG, "READ permission is granted...");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "requestCode=" + requestCode + "; --->" + permissions.toString()
                + "; grantResult=" + grantResults.toString());
        switch (requestCode) {
            case 0: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    // request successfully, handle you transactions

                } else {

                    // permission denied
                    // request failed
                }

                return;
            }
            default:
                break;

        }
    }

}


