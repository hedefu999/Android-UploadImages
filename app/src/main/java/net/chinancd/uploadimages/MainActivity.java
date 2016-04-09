package net.chinancd.uploadimages;

import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.serialization.SoapObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static String TAG = "MainActivity>>>>";
    private static final String nameSpace = "http://222.192.61.8:8889/webservices/";    ///http://tempuri.org/
    private static final String url = "http://222.192.61.8:8889/UploadImage.asmx";
    private static final String methodName = "FileUploadImage1";      //methods provided by webservice page
    public static final int CAPTURE = 1, GALLERY = 2;
    private String checkORuploadResult = null;

    private Button upload;
    private Spinner spinner;
    private TextView checkresult;
    private TextView uploadResult;
    private ImageView preview;
    private ImageView guide;
    private EditText idnumber;
    private LinearLayout surface;

    private String idNo = "";
    private String[] type;
    private String item = "0";

    private ArrayAdapter adapter;
    private int btnAction = 0;//按钮状态码,据此设置按钮的行为
    HashMap<String, String> params = null;//webservice里传递的参数集合
    File folder;//本软件使用的文件夹
    File picturefolder;//上传的图片存储位置
    String uploadedFile;//最终要上传的图片的路径
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();//检查网络,创建文件
        //其他监听
        idnumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.e(TAG, "id number focus changed");
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    idNo = idnumber.getText().toString();
                    //检查是否输入了正确的18位身份证
                    if (idnumber.getText() != null) {
                        if (idNo.length() >= 17) {
                            preCheckID();
                        }
                    }
                } else {
                    Log.e(TAG, "程序刚启动或正在输入");
                }
            }
        });
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clearInputFocus(idnumber);
                item = type[position];
                Log.e(TAG, "onItemSelected>>" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                clearInputFocus(idnumber);
                Log.e(TAG, "onNothingSelected");
            }
        });
    }

    private void init() {
        preferences = getSharedPreferences("launchtime", 0);
        upload = (Button) findViewById(R.id.btn_upload);
        checkresult = (TextView) findViewById(R.id.checkresult);
        uploadResult = (TextView) findViewById(R.id.tv_result);
        spinner = (Spinner) findViewById(R.id.spinner);
        preview = (ImageView) findViewById(R.id.preview);
        guide = (ImageView) findViewById(R.id.iv_guide);
        idnumber = (EditText) findViewById(R.id.id);
        surface = (LinearLayout) findViewById(R.id.ll_surface);
        adapter = ArrayAdapter.createFromResource(this, R.array.checkitems_1,
                android.R.layout.simple_spinner_item);
        type = getResources().getStringArray(R.array.type_xml);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);
        preview.setOnClickListener(this);
        surface.setOnClickListener(this);
        guide.setOnClickListener(this);
        upload.setOnClickListener(this);
        setGuide();
        //检查网络连接
        ConnectivityManager conManager =
                (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        boolean wifi =
                conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
        boolean mobile =
                conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
        //ConnectTask connectTask=new ConnectTask();
        if (wifi | mobile) {
            //connectTask.execute();
        } else {
            Toast.makeText(this, "您没有连接网络", Toast.LENGTH_SHORT).show();
        }
        //创建文件夹
        if (Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED)) {
            folder = new File(Environment.getExternalStorageDirectory() + File.separator + "chocum");
            if (!folder.exists()) {
                if (!folder.mkdir()) {
                    Log.e(TAG, "创建文件夹失败");
                }
            }
        } else {
            Toast.makeText(this, "找不到SD卡", Toast.LENGTH_SHORT).show();
        }
    }

    //根据sharePreference设置是否显示引导
    private void setGuide() {
        if (preferences.getInt("No", 1) != 0) {
            preferences.edit().putInt("No", 0).commit();
            guide.setVisibility(View.VISIBLE);
            guide.setImageResource(R.drawable.guide);
        } else {
            guide.setVisibility(View.INVISIBLE);
        }
    }

    private String getTimeStamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(date);
    }

    private void clearInputFocus(TextView textView) {
        if (textView.hasFocus()) {
            textView.clearFocus();
        }
    }

    //在此处预先查询身份证号的存在性
    private void preCheckID() {
        checkresult.setText("查询中...");
        params = new HashMap<String, String>();
        params.put("ProImage", "");
        params.put("ProName", "");
        params.put("id_card_number", idNo);
        params.put("type", "Blood");
        accessWebService(url, nameSpace, methodName, params);
        checkresult.setText(checkORuploadResult);
    }

    //根据图片的路径生成合适的缩略图,原理是根据原图片的长宽来确定压缩比例,设置的固定目标大小是宽度=600
    private void showPic(String imgpath) {
        Bitmap compressedBitmap = ImageUtils.compressByPixels(imgpath, 600);
        preview.setImageBitmap(compressedBitmap);
        upload.setText("上传照片");
        btnAction = 2;
        uploadResult.setText("点击按钮开始上传");
        //bitmap.recycle();imageview还在使用这个bitmap回收会出错
    }

    private String decodeImage(String filepath, int quality) {
        File processedJPEG = ImageUtils.compressByQualityAndSave(
                filepath,picturefolder.getAbsolutePath(),quality);
        //向图库添加图片
        MediaScannerConnection.scanFile(this,new String[]{processedJPEG.getAbsolutePath()},null,null);
        return FileUtils.decodeFile2Base64(processedJPEG.getAbsolutePath());
       /* String uploadBuffer = null;
        ByteArrayOutputStream baos=null;
        ByteArrayOutputStream partbaos=null;
        InputStream is=null;
        byte[] buffer = new byte[1024];
        int count = 0;
        try {
            partbaos=new ByteArrayOutputStream();
            baos = new ByteArrayOutputStream();

            //这个100改成30图像的分辨率是不会有损失的,但是文件大小可以从2MB变成110KB
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            Log.e(TAG,"decodeImage>>baos的字节数"+baos.size());
            *//*is=new ByteArrayInputStream(baos.toByteArray());
            while ((count=is.read(buffer))!=-1){
                partbaos.write(buffer,0,count);
                Log.e(TAG,"decodeImage>>while循环内partbaos的字节数"+partbaos.size());
            }
            Log.e(TAG,"decodeImage>>while循环内partbaos的字节数"+partbaos.size());*//*
            uploadBuffer = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (OutOfMemoryError error) {
            Toast.makeText(this, "图片过大,请调低相机分辨率重新拍摄", Toast.LENGTH_LONG).show();
        } finally {
            try {
                is.close();
                baos.close();
                partbaos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        return uploadBuffer;*/
    }

    private boolean writeBase64ToSD(String imgData) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File SDCardDir = Environment.getExternalStorageDirectory();
            File txtFileFolder = new File(SDCardDir, "/upload/text");
            if (!txtFileFolder.exists()) {
                if (!txtFileFolder.mkdir()) {
                    Log.e(TAG, "creating folder failed");
                }
            }
            File txtFile = new File(Environment.getExternalStorageDirectory(), "imgData.txt");
            try {
                FileOutputStream outputStream = new FileOutputStream(txtFile);
                outputStream.write(imgData.getBytes());
                outputStream.close();
                Log.e(TAG, "writing to SD may succeed!");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "writing to SD failed!");
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        picturefolder= new File(folder, "UploadImages");
        picturefolder.mkdir();//new File下不能创建文件夹
        File picture;
        switch (requestCode) {
            case 10:
                switch (resultCode) {
                    case 1://开始拍照,先将照片保存到SD卡,可以使得图片不至于太小
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        //uploadedFile初始化的第一种情况,指定位置
                        uploadedFile = picturefolder + File.separator +item + getTimeStamp() + ".jpg";
                        picture = new File(uploadedFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(picture));
                        startActivityForResult(intent, CAPTURE);
                        break;
                    case 2:
                        Intent intent_gallery = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent_gallery, GALLERY);
                        break;
                    default:
                        Toast.makeText(this, "中康承诺保护用户的隐私\n\t\t\t\t请放心上传", Toast.LENGTH_SHORT).show();
                        //用户点击了取消按钮或点击了选框外的区域
                        break;
                }
                break;
            //下面两个分别是拍照和从图库获取图片的内容
            //拍照完成,需要显示照片的缩略图
            case CAPTURE:
                if (resultCode == RESULT_OK) {
                    showPic(uploadedFile);
                }
                break;
            //图库只初始化全局变量uploadedFile
            case GALLERY:
                if (resultCode == RESULT_OK) {
                    //从Uri获取文件路径及文件名
                    Uri imgUri = data.getData();
                    String[] projection = {MediaStore.Images.Media.DATA};
                    CursorLoader cursorLoader = new CursorLoader(this, imgUri, projection, null, null, null);
                    Cursor cursor = cursorLoader.loadInBackground();
                    int columIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    //uploadedFile初始化的第二种情况,获取位置
                    uploadedFile = cursor.getString(columIndex);
                    cursor.close();
                    showPic(uploadedFile);
                }
                break;
            default:
                break;

        }
    }

    //此处实现了Webservice的回调,该回调很优雅地实现了返回结果,设置UI界面的功能,无需开启新线程
    private void accessWebService(String url, String nameSpace,
                                  String methodName, HashMap<String, String> params) {
        WebServiceUtils.callWebservice(url, nameSpace, methodName,
                params, new WebServiceUtils.WebServiceCallBack() {
                    @Override
                    public void callBack(SoapObject resultObj) {
                        if (resultObj != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 0; i < resultObj.getPropertyCount(); i++) {
                                stringBuilder.append(resultObj.getProperty(i)).append("\r\n");
                            }
                            checkORuploadResult = stringBuilder.toString();
                            uploadResult.setText(checkORuploadResult);
                        } else {
                            Log.e(TAG, "获取的resultObj为空");
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.preview:
                Intent intentView = new Intent(Intent.ACTION_VIEW);
                intentView.setDataAndType(Uri.fromFile(new File(uploadedFile)), "image/*");
                startActivity(intentView);
                break;
            case R.id.iv_guide:
                guide.setVisibility(View.INVISIBLE);
                break;
            case R.id.ll_surface:
                clearInputFocus(idnumber);
                break;
            case R.id.btn_upload:
                if (!(idNo.equals("") || item.equals("0")) && btnAction != 2) {
                    btnAction = 1;
                }
                clearInputFocus(idnumber);
                if (btnAction == 1) {
                    Intent intent = new Intent(MainActivity.this, PopWindow.class);
                    startActivityForResult(intent, 10);
                } else if (btnAction == 2) {
                    params = new HashMap<String, String>();
                    params.put("ProImage", decodeImage(uploadedFile, 30));
                    params.put("ProName", getTimeStamp() + ".jpg");
                    params.put("id_card_number", idNo);
                    params.put("type", item);
                    accessWebService(url, nameSpace, methodName, params);
                    btnAction = 1;
                } else {
                    Toast.makeText(getApplicationContext(), "您没有填写完整的信息", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case R.id.maincontainer:
                break;
        }
    }
}
