package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.example.androidftpclient.IOThread.DownloadThread;
import com.example.androidftpclient.IOThread.UploadThread;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int UPLOAD_SUCCESS = 1;
    private static final int UPLOAD_FAIL = 2;
    private static final int DOWNLOAD_SUCCESS = 3;
    private static final int DOWNLOAD_FAIL = 4;

    private EditText username;
    private EditText password;
    private Button login;
    private Button btn_file;
    private Button btn_upload;
    private Button btn_download;
    private TextView tv;
    private Button btn_test;

    private List<File> files;
    private FTPClient ftpClient;
    private FTPOperationProcessor FTPProcessor;
    private Context main;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case UPLOAD_SUCCESS:
                    Toast.makeText(MainActivity.this, "上传成功^_^", Toast.LENGTH_LONG).show();
                    break;
                case UPLOAD_FAIL:
                    Toast.makeText(MainActivity.this, "上传失败>_<", Toast.LENGTH_LONG).show();
                    break;
                case DOWNLOAD_SUCCESS:
                    Toast.makeText(MainActivity.this, "下载成功^_^", Toast.LENGTH_LONG).show();
                    break;
                case DOWNLOAD_FAIL:
                    Toast.makeText(MainActivity.this, "下载失败>_<", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        username = findViewById(R.id.UserName);
        password = findViewById(R.id.Password);
        login = findViewById(R.id.Login);
        btn_file= (Button) findViewById(R.id.btn_open);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_download = (Button) findViewById(R.id.btn_download);
        btn_test = (Button) findViewById(R.id.btn_test);
        files = new ArrayList<>();
        tv = (TextView) findViewById(R.id.tv);
        main = this;

        requestReadExternalPermission();
        requestWriteExternalPermission();

        FTPProcessor = new FTPOperationProcessor(this);

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
                upload("/test1");
            }
        });

        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download("/test1/test.jpg","/storage/emulated/0/Download");
            }
        });


        //获取文件名列表的示例
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        //从server上指定目录获取文件名列表
                        FTPFile[] files;
                        try {
                            files = FTPProcessor.GetFiles("/test1");
                            System.out.println("test size:"+files.length);
                            for (int i = 0; i < files.length; i++) {
                                System.out.println(files[i].getName() + files[i].isDirectory() + files[i].isFile());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        //获取指定路径下的所有文件名
                        String storageDir = Environment.getExternalStorageDirectory().toString();
                        List<String> list;
                        list = FileUtils.getFilesAllName(storageDir);
                        for (int i = 0; i < list.size(); i++) {
                            File f = new File(list.get(i));
                            System.out.println(list.get(i) + f.isDirectory() + f.isFile());
                        }

                    }
                }).start();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });


    }

    private void Login() {
        try {
            FTPProcessor.connect("10.250.184.248",21,"john","1234");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void createDirectory(String path){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FTPProcessor.createDirectory(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void upload(String remotePath){
        Thread thread = new UploadThread(remotePath,files,FTPProcessor,handler);
        thread.start();
    }

    private void download(String remoteFilePath,String localPath){
        Thread thread = new DownloadThread(remoteFilePath,localPath,FTPProcessor,handler);
        thread.start();
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
                        //tv.setText(path1);
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

    @SuppressLint("NewApi")
    private void requestWriteExternalPermission(){
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "WRITE permission IS NOT granted...");

            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Log.d(TAG, "11111111111111");
            } else {
                // 0 是自己定义的请求coude
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Log.d(TAG, "222222222222");
            }
        } else {
            Log.d(TAG, "WRITE permission is granted...");
        }
    }

}


