package net.chinancd.uploadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by LSQ on 2016/4/8.
 */
public class ImageUtils {
    private static final String TAG = "ImageUtils";

    /**
     * 根据给定的文件路径和目标图片像素值进行压缩
     *
     * @param filepath 文件路径
     * @param maxside  目标图片的最大边
     * @return 压缩得到的bitmap
     */
    public static Bitmap compressByPixels(String filepath, int maxside) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, options);
        int originWidth = options.outWidth;
        int originHeight = options.outHeight;
        //Log.e(TAG,"原始图片宽>>"+originWidth+"原始图片高度>>"+originHeight);
        float ratio;
        ratio = originHeight > originWidth ? (float) maxside / originHeight : (float) maxside / originHeight;
        //Log.e(TAG,"compressByPixels>>获得的缩放比例="+ratio);
        Matrix matrix = new Matrix();
        matrix.postScale(ratio, ratio);
        Bitmap originBitmap = BitmapFactory.decodeFile(filepath);//貌似发生了oom
        Bitmap compressedBitmap = null;
        compressedBitmap = originBitmap
                .createBitmap(originBitmap, 0, 0, originWidth, originHeight, matrix, true);
        return compressedBitmap;
    }

    /**
     * 根据给出的文件大小压缩图片,使用了matrix
     *
     * @param filepath   文件路径
     * @param targetSize 目标文件大小,结果不一定精确,单位是KB
     * @return 压缩得到的
     */
    //该方法易造成OOM,费力不讨好
    public static Bitmap compressBySize(String filepath, int targetSize) {
        File file = new File(filepath);
        long sourceSize = 0;
        //下面获取文件大小
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            sourceSize = fis.available() / 1024;//以KB为单位
            Log.e(TAG, "compressBySize>>file size=" + sourceSize);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Bitmap originBitmap = BitmapFactory.decodeFile(filepath);//貌似发生了oom
        Bitmap compressedBitmap = null;
        if ((int) sourceSize > targetSize) {
            double areaRatio = (double) targetSize / sourceSize;//areaRatio<1
            float sizeRatio = (float) Math.sqrt(areaRatio);
            Matrix matrix = new Matrix();
            matrix.postScale(sizeRatio, sizeRatio);
            Log.e(TAG, "compressBySize>>ratio of compress:sizeRatio>>" + sizeRatio + ",areaRatio>>" + areaRatio);
            compressedBitmap = Bitmap.createBitmap(originBitmap, 0, 0,
                    originBitmap.getWidth(), originBitmap.getHeight(), matrix, true);
            if (originBitmap != null) {
                originBitmap.recycle();
                originBitmap = null;
            }
            return compressedBitmap;
        } else {
            return originBitmap;
        }
    }

    public static File compressByQualityAndSave(String filename, String targetfolder, int quallity) {
        Bitmap originbitmap = BitmapFactory.decodeFile(filename);
        File compressedfile = FileUtils.renameFile(filename, targetfolder);
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            fos = new FileOutputStream(compressedfile);
            bos = new BufferedOutputStream(fos);
            originbitmap.compress(Bitmap.CompressFormat.JPEG, quallity, bos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (originbitmap != null) {
                originbitmap.recycle();
                originbitmap = null;
                System.gc();
            }
            try {
                bos.flush();
                fos.flush();
                bos.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return compressedfile;
    }

    public static Bitmap getRoundCornerBitmap(Bitmap bitmap, float cornerRedius) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rect1 = new RectF(rect);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rect1, cornerRedius, cornerRedius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);
        return output;

    }

    public static Bitmap getRoundBitmap(Bitmap bitmap, int radius) {
            //int radius = targetRadius / 2;
            Bitmap output = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_4444);
            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            final Rect rect = new Rect(0, 0, radius, radius);
            final RectF rect1 = new RectF(rect);
            canvas.drawARGB(0, 0, 0, 0);
            canvas.drawRoundRect(rect1, radius, radius, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;
    }
}















