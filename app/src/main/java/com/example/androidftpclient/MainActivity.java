package com.example.androidftpclient;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.example.androidftpclient.IOThread.DownloadThread;
import com.example.androidftpclient.IOThread.UploadThread;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int UPLOAD_SUCCESS = 1;
    private static final int UPLOAD_FAIL = 2;
    private static final int DOWNLOAD_SUCCESS = 3;
    private static final int DOWNLOAD_FAIL = 4;
    private static final int REQUEST_DOWNLOAD = 100;
    private static final int REQUEST_DIRECTORY = 200;

    private EditText inputIP;
    private EditText inputUsername;
    private EditText inputPassword;

    private List<File> files;
    private FTPOperationProcessor FTPProcessor;

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
        inputIP = findViewById(R.id.IP);
        inputUsername = findViewById(R.id.UserName);
        inputPassword = findViewById(R.id.Password);

        files = new ArrayList<>();

        requestReadExternalPermission();
        requestWriteExternalPermission();

        FTPProcessor = new FTPOperationProcessor(this);

//        //获取文件名列表的示例
//        btn_test.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        //获取指定路径下的所有文件名
//                        System.out.println("1111111111");
//                        String storageDir = Environment.getExternalStorageDirectory().toString();
//                        List<String> list;
//                        list = FileUtils.getFilesAllName(storageDir);
//                        for (int i = 0; i < list.size(); i++) {
//                            File f = new File(list.get(i));
//                            System.out.println(list.get(i) + f.isDirectory() + f.isFile());
//                        }
//                    }
//                }).start();
//            }
//        });


    }

    public void OpenLocalFileSelection(View view){
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

    private void OpenUploadDialog(){
        FTPFile[] fileArray;
        try {
            fileArray = FTPProcessor.GetFiles("/");
            UploadDialog dialog = new UploadDialog(fileArray);
            dialog.setOnClickListener(new UploadDialog.OnClickListener() {
                @Override
                public void onUpload(String path) {
                    upload(path);
                }
            });
            dialog.show(this.getSupportFragmentManager(),"dialog");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Login(View v) {
        String ip = inputIP.getText().toString();
        String username = inputUsername.getText().toString();
        String password = inputPassword.getText().toString();
        try {
            FTPProcessor.connect(ip,21,username,password);//10.249.92.87 john 1234
            //FTPProcessor.connect("10.249.92.87",21,"john","1234");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void OpenDownloadActivity(View v){
        FTPFile[] fileArray;
        try {
            fileArray = FTPProcessor.GetFiles("/");
            Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
            intent.putExtra("list", (Serializable) fileArray);
            startActivityForResult(intent, REQUEST_DOWNLOAD);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void OpenDirectoryOPActivity(View v){
        FTPFile[] fileArray;
        try {
            fileArray = FTPProcessor.GetFiles("/");
            Intent intent = new Intent(MainActivity.this, DirectoryOperationActivity.class);
            intent.putExtra("list", (Serializable) fileArray);
            startActivityForResult(intent, REQUEST_DIRECTORY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    private void createDirectory(String path){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    FTPProcessor.createDirectory(path);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

    private void upload(String remotePath){
        Thread thread = new UploadThread(remotePath,files,FTPProcessor,handler);
        thread.start();
        //deleteRemoteFile("/test1/0.jpg");
    }

    private void download(String remoteFilePath,String localPath){
        Thread thread = new DownloadThread(remoteFilePath,localPath,FTPProcessor,handler);
        thread.start();
    }

    //例createDirectory("/test1/lala/")
    //要以斜杠结尾
    private void createDirectory(String path){
        try {
            if (FTPProcessor.createDirectory(path) == FTPOperationProcessor.UploadStatus.CREATE_DIRECTORY_SUCCESS){
                Toast.makeText(MainActivity.this,"创建成功^_^",Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(MainActivity.this,"创建失败>_<",Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //例deleteDirectory("/test1/lala")
    private void deleteDirectory(String path){
        try {
            if (FTPProcessor.deleteDirectory(path) == FTPOperationProcessor.UploadStatus.DELETE_REMOTE_SUCCESS){
                Toast.makeText(MainActivity.this,"删除成功^_^",Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(MainActivity.this,"删除失败>_<",Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //例deleteRemoteFile("/test1/0.jpg")
//    private void deleteRemoteFile(String remote){
//        try {
//            if (FTPProcessor.deleteFile(path) == FTPOperationProcessor.UploadStatus.DELETE_REMOTE_SUCCESS){
//                Toast.makeText(MainActivity.this,"删除成功^_^",Toast.LENGTH_LONG).show();
//            }
//            else{
//                Toast.makeText(MainActivity.this,"删除失败>_<",Toast.LENGTH_LONG).show();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            FTPProcessor.disConnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    OpenUploadDialog();
                    break;
                case REQUEST_DOWNLOAD:
                    download(data.getStringExtra("path"),"/storage/emulated/0/1");
                    break;
                case REQUEST_DIRECTORY:
                    if (data.getStringExtra("operation").equals("delete")){
                        deleteDirectory(data.getStringExtra("path"));
                    } else {
                        createDirectory(data.getStringExtra("path"));
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


