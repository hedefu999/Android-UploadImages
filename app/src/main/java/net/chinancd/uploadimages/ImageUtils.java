package net.chinancd.uploadimages;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by LSQ on 2016/4/8.
 */
public class ImageUtils {
    private static final String TAG="ImageUtils";

    /**
     * 根据给定的文件路径和要压缩到的像素值进行压缩图片
     * @param filepath 文件路径
     * @param pixels 目的图片的宽度,并不是严格的数值,甚至是相差很大
     * @return 压缩得到的bitmap
     */
    public static Bitmap compressByPixels(String filepath,int pixels){
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(filepath,options);
        int originWidth=options.outWidth;
        int originHeight=options.outHeight;
        Log.e(TAG,"原始图片宽>>"+originWidth+"原始图片高度>>"+originHeight);
        int ratio=originWidth/pixels;
        Log.e(TAG,"设置图片缩放的比例是:"+ratio);
        if(ratio==0){
            options.inSampleSize=1;
        }else {
            options.inSampleSize=ratio;
        }
        options.inJustDecodeBounds=false;//不设置回false,生成的Bitmap=null
        Bitmap compressedBitmap;
        try {
            compressedBitmap=BitmapFactory.decodeFile(filepath,options);
        }catch (OutOfMemoryError error){
            options.inSampleSize=ratio+1;
            compressedBitmap=BitmapFactory.decodeFile(filepath,options);
        }
        return compressedBitmap;
    }

    /**
     * 根据给出的文件大小压缩图片,使用了matrix
     * @param filepath 文件路径
     * @param targetSize 目标文件大小,结果不一定精确,单位是KB
     * @return 压缩得到的
     */
    public static Bitmap compressBySize(String filepath,int targetSize){
        File file=new File(filepath);
        long sourceSize=0;
        try {
            FileInputStream fis=new FileInputStream(file);
            sourceSize=fis.available()/1024;//以KB为单位
            Log.e(TAG,">>file size="+sourceSize);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap originBitmap=BitmapFactory.decodeFile(filepath);
        Bitmap compressedBitmap=null;
        if((int)sourceSize>targetSize){
            double areaRatio=sourceSize/targetSize;
            float sizeRatio=(float) Math.sqrt(areaRatio);
            Matrix matrix=new Matrix();
            matrix.postScale(sizeRatio,sizeRatio);
            compressedBitmap=Bitmap.createBitmap(originBitmap,0,0,
                    originBitmap.getWidth(),originBitmap.getHeight(),matrix,true);
            originBitmap.recycle();
            originBitmap=null;
            return compressedBitmap;
        }else{
            return originBitmap;
        }
    }
}
