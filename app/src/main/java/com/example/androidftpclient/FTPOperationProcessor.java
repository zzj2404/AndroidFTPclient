package com.example.androidftpclient;

import android.content.Context;
import android.nfc.Tag;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import static java.util.jar.Pack200.Packer.ERROR;


/**
 * <p>
 * Title: FTPOperationProcessor
 * </p>
 *
 * <p>
 * Description: FTP操作处理类
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2010
 * </p>
 *
 * @author servyou
 *
 * @version 1.0
 */

public class FTPOperationProcessor {
    static private FTPClient client = new FTPClient();
    /**
     * 默认编码
     */
    public final static String ENCODING = "UTF-8";

    /**
     * FTP传输用的编码
     */
    public final static String FTP_ENCODING = "ISO-8859-1";

    /**
     * 目录前缀
     */
    public final static String PREFIX = "/";
    Context main;

    public FTPOperationProcessor(Context main) {
        super();
        this.main = main;
    }

    public enum DownloadStatus {
        REMOTE_FILE_NOEXIST, // 远程文件不存在
        LOCAL_BIGGER_REMOTE, // 本地文件大于远程文件
        DOWNLOAD_FROM_BREAK_SUCCESS, // 断点下载文件成功
        DOWNLOAD_FROM_BREAK_FAILED, // 断点下载文件失败
        DOWNLOAD_NEW_SUCCESS, // 全新下载文件成功
        DOWNLOAD_NEW_FAILED; // 全新下载文件失败
    }

    public enum UploadStatus {
        CREATE_DIRECTORY_FAIL, // 远程服务器相应目录创建失败
        CREATE_DIRECTORY_SUCCESS, // 远程服务器闯将目录成功
        UPLOAD_NEW_FILE_SUCCESS, // 上传新文件成功
        UPLOAD_NEW_FILE_FAILED, // 上传新文件失败
        FILE_EXITS, // 文件已经存在
        REMOTE_BIGGER_LOCAL, // 远程文件大于本地文件
        UPLOAD_FROM_BREAK_SUCCESS, // 断点续传成功
        UPLOAD_FROM_BREAK_FAILED, // 断点续传失败
        DELETE_REMOTE_FAILD, // 删除远程文件失败
        DELETE_REMOTE_SUCCESS;
    }
    public enum LoginStatus {
        LOGIN_SUCCESS,
        LOGIN_FAIL;
    }
    private static final String TAG = "MainActivity";

    /**
     * 连接FTP服务器
     *
     * @param hostname 服务器IP，或主机名
     * @param port     端口号
     * @param username 用户名
     * @param password 密码
     * @return 连接成功返回true, 失败返回false
     * @throws IOException
     */
    public LoginStatus connect(String hostname, int port, String username,
                        String password) throws IOException {
        final class FTPRunnable implements Runnable {
            private LoginStatus status = LoginStatus.LOGIN_FAIL;
            @Override
            public void run() {
                try {
                    //登录
                    client.connect(hostname, port);
                    client.login(username, password);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int replyCode = client.getReplyCode();
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    try {
                        client.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    status = LoginStatus.LOGIN_FAIL;
                } else {
                    status = LoginStatus.LOGIN_SUCCESS;
                }
            }
            public LoginStatus getStatus(){return status;}
        }

        FTPRunnable ftpRunnable = new FTPRunnable();
        Thread thread = new Thread(ftpRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ftpRunnable.getStatus();

    }

    /**
     * 用户登出，并关闭连接
     *
     * @throws IOException
     */
    public void disConnect() throws IOException {
        if (client.isConnected()) {
            client.logout();
            client.disconnect();
        }
    }

    /**
     * 遍历服务器的某一目录
     *
     * @param pathname FTP服务器的路径，如/dir1/dir2/
     * @throws IOException
     */
    public void traverseDirectory(String pathname) throws IOException {
        client.changeWorkingDirectory(pathname);
        FTPFile[] fileList = client.listFiles(pathname);
        traverse(fileList);
    }

    /**
     * 遍历FTP服务器的目录
     *
     * @param fileList 文件列表
     * @throws IOException
     */
    private void traverse(FTPFile[] fileList) throws IOException {
        String tempDir = null;
        for (FTPFile file : fileList) {
            if (file.getName().equals(".") || file.getName().equals("..")) {
                continue;
            }
            if (file.isDirectory()) {
                System.out.println("***************** Directory: " + file.getName()
                        + "  Start **************");
                tempDir = client.printWorkingDirectory();
                if (tempDir.matches("^((/\\w+))+$"))
                    tempDir += "/" + file.getName();
                else
                    tempDir += file.getName();
                client.changeWorkingDirectory(new String(tempDir.getBytes(ENCODING),
                        FTP_ENCODING));
                traverse(client.listFiles(tempDir));
                // 不是目录，是文件的情况
                System.out.println("***************** Directory:" + file.getName()
                        + "   End **************\n");
            } else {
                System.out.println("FileName:" + file.getName() + " FileSize:"
                        + file.getSize() / (1024) + "KB" + " CreateTime:"
                        + file.getTimestamp().getTime());
            }
        }
        // 遍历完当前目录，就要将工作目录改为当前目录的父目录
        client.changeToParentDirectory();
    }

//    /**
//     * 下载单个文件
//     *
//     * @param remote    远端文件
//     * @param localFile 本地文件
//     * @throws IOException
//     */
//    public DownloadStatus download(String remote, File localFile)
//            throws IOException {
//        client.enterLocalPassiveMode();
//        client.setFileType(FTPClient.BINARY_FILE_TYPE);
//        DownloadStatus result = DownloadStatus.DOWNLOAD_NEW_SUCCESS;
//        // 检查远程文件是否存在
//        FTPFile[] files = client.listFiles(new String(remote.getBytes(ENCODING),
//                FTP_ENCODING));
//        if (files.length != 1) {
//            System.out.println("远程文件不存在");
//            return DownloadStatus.REMOTE_FILE_NOEXIST;
//        }
//        long lRemoteSize = files[0].getSize();
//        // 本地存在文件，进行断点下载
//        if (localFile.exists()) {
//            long localSize = localFile.length();
//            // 判断本地文件大小是否大于远程文件大小
//            if (localSize >= lRemoteSize) {
//                System.out.println("本地文件大于远程文件，下载中止");
//                return DownloadStatus.LOCAL_BIGGER_REMOTE;
//            }
//            // 进行断点续传，并记录状态
//            FileOutputStream out = new FileOutputStream(localFile, true);
//            client.setRestartOffset(localSize);
//            InputStream in = client.retrieveFileStream(new String(remote
//                    .getBytes(ENCODING), FTP_ENCODING));
//            FileUtils.copyStream(in, out);
//            boolean isDo = client.completePendingCommand();
//            if (isDo) {
//                result = DownloadStatus.DOWNLOAD_FROM_BREAK_SUCCESS;
//            } else {
//                result = DownloadStatus.DOWNLOAD_FROM_BREAK_FAILED;
//            }
//        } else {
//            localFile.createNewFile();
//            FileOutputStream out = new FileOutputStream(localFile);
//            InputStream in = client.retrieveFileStream(new String(remote
//                    .getBytes(ENCODING), FTP_ENCODING));
//            FileUtils.copyStream(in, out);
//        }
//        return result;
//    }
/** *//**
     * 从FTP服务器上下载文件,支持断点续传，上传百分比汇报
     * @param remote 远程文件路径
     * @param localDirectory 本地文件路径
     * @return 上传的状态
     * @throws IOException
     */
    public DownloadStatus download(String remote,String localDirectory) throws IOException{
        //设置被动模式
        client.enterLocalPassiveMode();
        //设置以二进制方式传输
        client.setFileType(FTP.BINARY_FILE_TYPE);
        DownloadStatus result;

        int index =remote.lastIndexOf('/');
        String remoteDirectory = remote.substring(0,index);
        String name = remote.substring(index + 1);
        System.out.println("remoteDirectory:"+remoteDirectory);
        System.out.println("name"+name);

        //检查远程文件是否存在
        FTPFile[] files = client.listFiles(new String(remoteDirectory.getBytes(ENCODING),
                FTP_ENCODING));
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(name)){
                System.out.println("find file"+name);
                String local = localDirectory + "/" + files[i].getName();

                long lRemoteSize = files[0].getSize();
                File f = new File(local);
                //本地存在文件，进行断点下载
                if(f.exists()){
                    long localSize = f.length();
                    //判断本地文件大小是否大于远程文件大小
                    if(localSize >= lRemoteSize){
                        System.out.println("本地文件大于远程文件，下载中止");
                        return DownloadStatus.LOCAL_BIGGER_REMOTE;
                    }

                    //进行断点续传，并记录状态
                    FileOutputStream out = new FileOutputStream(f,true);
                    client.setRestartOffset(localSize);
                    InputStream in = client.retrieveFileStream(new String(remote.getBytes("GBK"),"iso-8859-1"));


                    byte[] bytes = new byte[1024];
                    long step = lRemoteSize /100;
                    long process=localSize /step;
                    int c;
                    while((c = in.read(bytes))!= -1){
                        out.write(bytes,0,c);
                        localSize+=c;
                        long nowProcess = localSize /step;
                        if(nowProcess > process){
                            process = nowProcess;
                            if(process % 10 == 0)
                                System.out.println("下载进度："+process);
                            //TODO 更新文件下载进度,值存放在process变量中
                        }
                    }
                    in.close();
                    out.close();
                    boolean isDo = client.completePendingCommand();
                    if(isDo){
                        result = DownloadStatus.DOWNLOAD_FROM_BREAK_SUCCESS;
                    }else {
                        result = DownloadStatus.DOWNLOAD_FROM_BREAK_FAILED;
                    }
                }else {
                    System.out.println("local not exist");
                    //client.changeWorkingDirectory(remoteDirectory);
                    InputStream in= client.retrieveFileStream(new String(remote.getBytes("GBK"),"iso-8859-1"));
                    //InputStream in = null;
                    System.out.println("break 1");
                    OutputStream out = new FileOutputStream(f);
                    System.out.println("break 2");
                    byte[] bytes = new byte[1024];
                    long step = lRemoteSize /100;
                    long process=0;
                    long localSize = 0L;
                    int c;
                    while((c = in.read(bytes))!= -1){
                        out.write(bytes, 0, c);
                        localSize+=c;
                        long nowProcess = localSize /step;
                        if(nowProcess > process){
                            process = nowProcess;
                            if(process % 10 == 0)
                                System.out.println("下载进度："+process);
                            //TODO 更新文件下载进度,值存放在process变量中
                        }
                    }
                    in.close();
                    out.close();
                    boolean upNewStatus = client.completePendingCommand();
                    if(upNewStatus){
                        result = DownloadStatus.DOWNLOAD_NEW_SUCCESS;
                    }else {
                        result = DownloadStatus.DOWNLOAD_NEW_FAILED;
                    }
                }
                System.out.println(result);
                return result;
            }
        }
        System.out.println("远程文件不存在");
        return DownloadStatus.REMOTE_FILE_NOEXIST;
    }
    /**
     * 上传文件到FTP服务器，支持断点续传
     *
     * @param local  本地文件名称，绝对路径
     * @param remote 远程文件路径，使用/home/directory1/subdirectory/file.ext
     *               按照Linux上的路径指定方式，支持多级目录嵌套，支持递归创建不存在的目录结构
     * @return 上传结果
     * @throws IOException
     */
    public UploadStatus upload(String local, String remote) throws IOException {
        // 设置PassiveMode传输
        client.enterLocalPassiveMode();
        // 设置以二进制流的方式传输
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        client.setControlEncoding(ENCODING);
        UploadStatus result=UploadStatus.DELETE_REMOTE_FAILD;


        // 对远程目录的处理
        String remoteFileName = remote.substring(remote.lastIndexOf("/") + 1);
        String directory = remote.substring(0,remote.lastIndexOf("/"));
        if (remote.contains("/")) {
            // 创建服务器远程目录结构，创建失败直接返回
            // 以下两句存在问题??
             if (createDirectory(remote) == UploadStatus.CREATE_DIRECTORY_FAIL) {
                return UploadStatus.CREATE_DIRECTORY_FAIL;
             }
        }
        // 检查远程是否存在文件
        FTPFile[] files = client.listFiles(directory);
        //System.out.println("size "+files.length);
        for (int i = 0; i < files.length; i++) {
            //System.out.println("list"+files[i].getName());
            if (files[i].getName().equals(remoteFileName)){
                long remoteSize = files[i].getSize();
                File f = new File(local);
                long localSize = f.length();
                if (remoteSize == localSize) {
                    //ThreadToast("已经存在的文件>_<");
                    return UploadStatus.FILE_EXITS;
                } else if (remoteSize > localSize) {
                    return UploadStatus.REMOTE_BIGGER_LOCAL;
                }
                // 尝试移动文件内读取指针,实现断点续传
                result = uploadFile(remote, f, remoteSize);
                // 如果断点续传没有成功，则删除服务器上文件，重新上传
                if (result == UploadStatus.UPLOAD_FROM_BREAK_FAILED) {
                    if (!client.deleteFile(remote)) {
                        return UploadStatus.DELETE_REMOTE_FAILD;
                    }
                    result = uploadFile(remote, f, 0);
                    break;
                }
            }
            else {
                result = uploadFile(remote, new File(local), 0);
                break;
            }
        }
        return result;
    }

    /**
     * 上传单个文件，断点续传功能
     *
     * @param remoteFile
     *            远程文件
     * @param localFile
     *            本地文件
     * @throws IOException
     */

    public UploadStatus uploadFile(String remoteFile, File localFile, long remoteSize)
            throws IOException {
        UploadStatus status;
        //显示进度的上传
        long step = localFile.length() / 100;
        long process = 0;
        long localreadbytes = 0L;
        RandomAccessFile raf = new RandomAccessFile(localFile,"r");
        OutputStream out = client.appendFileStream(new String(remoteFile.getBytes("GBK"),"iso-8859-1"));
        //断点续传
        if(remoteSize>0){
            client.setRestartOffset(remoteSize);
            process = remoteSize /step;
            raf.seek(remoteSize);
            localreadbytes = remoteSize;
        }
        byte[] bytes = new byte[1024];
        int c;
        while((c = raf.read(bytes))!= -1){
            out.write(bytes,0,c);
            localreadbytes+=c;
            if(localreadbytes / step != process){
                process = localreadbytes / step;
                System.out.println("上传进度:" + process);
                //TODO 汇报上传状态
            }
        }
        out.flush();
        raf.close();
        out.close();
        boolean result =client.completePendingCommand();

        if(remoteSize > 0){
            status = result?UploadStatus.UPLOAD_FROM_BREAK_SUCCESS:UploadStatus.UPLOAD_FROM_BREAK_FAILED;
        }else {
            status = result?UploadStatus.UPLOAD_NEW_FILE_SUCCESS:UploadStatus.UPLOAD_NEW_FILE_FAILED;
        }

        System.out.println(status);
        return status;

    }

    /**
     * 创建目录,(远程目录格式必须是/aaa/bbb/ccc/ddd/的形式)
     *
     * @param remote
     *            远程目录路径
     * @throws IOException
     */
    public UploadStatus createDirectory(String remote) throws IOException {
        final class FTPRunnable implements Runnable {
            private UploadStatus status = UploadStatus.CREATE_DIRECTORY_FAIL;
            @Override
            public void run() {
                try {
                    int start = 0, end = 0;
                    start = remote.startsWith("/") ? 1 : 0;
                    end = remote.indexOf("/", start);
                    int subcount = 0;
                    for (; start < end;subcount++) {

                        String subDirectory = remote.substring(start, end);
                        System.out.println(subDirectory);
                        if (!client.changeWorkingDirectory(new String(subDirectory
                                .getBytes(ENCODING), FTP_ENCODING))) {
                            // 目录不存在则在服务器端创建目录
                            if (!client.makeDirectory(new String(subDirectory.getBytes(ENCODING),
                                    FTP_ENCODING))) {
                                status = UploadStatus.CREATE_DIRECTORY_FAIL;
                                return;
                            } else {
                                client.changeWorkingDirectory(new String(subDirectory
                                        .getBytes(ENCODING), FTP_ENCODING));
                            }
                        }
                        start = end + 1;
                        end = remote.indexOf("/", start);
                        System.out.println(String.valueOf(start)+" "+String.valueOf(end));
                    }
                    for (int i =0;i<subcount;i++) {
                            client.changeToParentDirectory();
                    }
                    status = UploadStatus.CREATE_DIRECTORY_SUCCESS;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public UploadStatus getStatus(){return status;}
        }

        FTPRunnable ftpRunnable = new FTPRunnable();
        Thread thread = new Thread(ftpRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ftpRunnable.getStatus();
    }


    public UploadStatus deleteDirectory(String remote) throws IOException{
        final class FTPRunnable implements Runnable {
            private UploadStatus status = UploadStatus.DELETE_REMOTE_FAILD;
            @Override
            public void run() {
                try {
                    if (client.removeDirectory(remote)){
                        status = UploadStatus.DELETE_REMOTE_SUCCESS;
                    }
                    else{
                        status = UploadStatus.DELETE_REMOTE_FAILD;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public UploadStatus getStatus(){return status;}
        }

        FTPRunnable ftpRunnable = new FTPRunnable();
        Thread thread = new Thread(ftpRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ftpRunnable.getStatus();
    }

    public UploadStatus deleteFile(String remote) throws IOException{
        final class FTPRunnable implements Runnable {
            private UploadStatus status = UploadStatus.DELETE_REMOTE_FAILD;
            @Override
            public void run() {
                try {
                    if (client.deleteFile(remote)){
                        status = UploadStatus.DELETE_REMOTE_SUCCESS;
                    }
                    else{
                        status = UploadStatus.DELETE_REMOTE_FAILD;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public UploadStatus getStatus(){return status;}
        }

        FTPRunnable ftpRunnable = new FTPRunnable();
        Thread thread = new Thread(ftpRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ftpRunnable.getStatus();
    }

    public FTPFile[] GetFiles(String remote) throws IOException {
        final class FTPRunnable implements Runnable{
            private FTPFile[] files;
            @Override
            public void run() {
                try {
                    files = client.listFiles(new String(remote.getBytes(ENCODING),
                            FTP_ENCODING));
                    System.out.println("test size:"+files.length);
                    for (int i = 0; i < files.length; i++) {
                        System.out.println(files[i].getName() + files[i].isDirectory() + files[i].isFile());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public FTPFile[] getData(){
                return files;
            }
        }
        FTPRunnable ftpRunnable = new FTPRunnable();
        Thread thread = new Thread(ftpRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ftpRunnable.getData();
//        FTPFile[] files = client.listFiles(new String(remote.getBytes(ENCODING),
//                FTP_ENCODING));
//        return files;
    }

    public File[] GetLocalDirectory(){
        return main.getExternalCacheDirs();
    }
    /**
     * 递归遍历本地机器的要上传的目录，遍历的同时，在FTP服务器上创建目录
     * （如果在FTP服务器上目录不存在的话），上传文件
     *
     * @param directory
     *            本地目录
     * @throws IOException
     */
    private void traverseLocalDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            client.changeToParentDirectory();
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String directoryName = file.getName();
                client.makeDirectory(new String(directoryName.getBytes(ENCODING),
                        FTP_ENCODING));
                client.changeWorkingDirectory(new String(directoryName
                        .getBytes(ENCODING), FTP_ENCODING));
                traverseLocalDirectory(file);
            } else {
                System.out.println("FileName : " + file.getName());
                upload(file.getAbsolutePath(), client.printWorkingDirectory() + "/"
                        + file.getName());
            }
        }
        client.changeToParentDirectory();
    }

    /**
     * 上传本地机器的某一目录到FTP服务器的某一路径
     *
     * @param remoteBasePath
     *            FTP服务器的一个路径
     * @param localDirectoryPath
     *            本地机器需要上传的目录路径
     * @throws IOException
     */
    public UploadStatus uploadDirectory(String remoteBasePath,
                                        String localDirectoryPath) throws IOException {
        if (createDirectory(remoteBasePath) == UploadStatus.CREATE_DIRECTORY_FAIL) {
            return UploadStatus.CREATE_DIRECTORY_FAIL;
            // remoteBasePath FTP服务器上基目录,创建成功的话
        } else {
            if (client.changeWorkingDirectory(new String(remoteBasePath
                    .getBytes(ENCODING), FTP_ENCODING))) {
                File localDirectory = new File(localDirectoryPath);
                traverseLocalDirectory(localDirectory);
                return UploadStatus.CREATE_DIRECTORY_SUCCESS;
            } else {
                return UploadStatus.CREATE_DIRECTORY_FAIL;
            }
        }
    }
}

