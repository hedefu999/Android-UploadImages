package net.chinancd.uploadimages;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by LSQ on 2016/4/9.
 */
public class FileUtils {
    private static final String TAG = "FileUtils>>";

    /**
     * 将文件按base64编码,返回编码出的字符串,上传到服务器
     * 使用了Stream流的方法,可以避免OOM,一次只读取1024KB
     *
     * @param filepath 文件路径
     * @return base64编码
     */
    //从文件完整的路径生成编码流,该方法可避免OOM,不仅仅可以用于图片文件
    public static String decodeFile2Base64(String filepath) {
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        String decodeString = null;
        try {
            fis = new FileInputStream(new File(filepath));
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, count);
            }
            decodeString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                baos.flush();
                fis.close();
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return decodeString;
    }

    /**
     * @param originfile
     * @param targetfolder
     * @return
     */
    public static File renameFile(String originfile, String targetfolder) {
        File temp = new File(originfile);
        String newfilename = targetfolder + File.separator + temp.getName();
        temp = new File(newfilename);
        return temp;
    }
}
