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
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static String TAG = "MainActivity>>>>";
    private static final String nameSpace = "http://222.192.61.8:8889/webservices/";    ///http://tempuri.org/
    private static final String url = "http://222.192.61.8:8889/UploadImage.asmx";
    private static final String methodName = "FileUploadImage1";      //methods provided by webservice page
    public static final int CAPTURE = 1, GALLERY = 2;
    private static final int ONCHECK = 1, ONUPLOAD = 2;

    private Spinner spinner;
    private TextView checkTextView;
    private TextView currentStatus;
    private ImageView checkingAnim;
    private ImageView guide;
    private ImageView ivUpload;
    private ImageView waitAnim;
    private EditText idnumber;
    private LinearLayout surface;
    private LinearLayout realtimeCheck;
    private FrameLayout circleButton;
    private Animation animation;

    private String idNo = "";
    private String[] type;
    private String item = "0";

    private ArrayAdapter adapter;
    private static int btnAction = 0;//按钮状态码,据此设置按钮的行为
    private HashMap<String, String> params = null;//webservice里传递的参数集合
    private File folder;//本软件使用的文件夹
    private File picturefolder;//上传的图片存储位置
    private String uploadedFile;//最终要上传的图片的路径
    private Handler mHandler;
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();//检查网络,创建文件
        //其他监听
        idnumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    idNo = idnumber.getText().toString();
                    //检查是否输入了正确的18位身份证
                    if (idnumber.getText() != null) {
                        if (idNo.length() >= 17) {
                            idNo = idNo.length() == 17 ? idNo + "X" : idNo;
                            preCheckID();
                        }
                    }
                } else {
                    //程序刚启动或正在输入
                }
            }
        });
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clearInputFocus(idnumber);
                item = type[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                clearInputFocus(idnumber);
                Log.e(TAG, "onNothingSelected");
            }
        });
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 521) {
                    currentStatus.setText("继续上传");
                    btnAction=1;
                }
            }
        };
    }

    private void init() {
        preferences = getSharedPreferences("launchtime", 0);
        currentStatus = (TextView) findViewById(R.id.currentStatus);
        spinner = (Spinner) findViewById(R.id.spinner);
        guide = (ImageView) findViewById(R.id.iv_guide);
        ivUpload = (ImageView) findViewById(R.id.iv_upload);
        waitAnim = (ImageView) findViewById(R.id.waitanim);
        idnumber = (EditText) findViewById(R.id.id);
        surface = (LinearLayout) findViewById(R.id.ll_surface);
        realtimeCheck = (LinearLayout) findViewById(R.id.realtimecheck);
        circleButton = (FrameLayout) findViewById(R.id.circleButton);

        animation = AnimationUtils.loadAnimation(this, R.anim.imgloading);
        checkingAnim = new ImageView(this);
        checkingAnim.setImageResource(R.drawable.imgloading);
        checkingAnim.setScaleType(ImageView.ScaleType.FIT_CENTER);
        checkTextView = new TextView(this);
        checkTextView.setTextSize(20);

        adapter = ArrayAdapter.createFromResource(this, R.array.checkitems_1,
                android.R.layout.simple_spinner_item);
        type = getResources().getStringArray(R.array.type_xml);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setVisibility(View.VISIBLE);
        surface.setOnClickListener(this);
        guide.setOnClickListener(this);
        ivUpload.setOnClickListener(this);
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

    private void handleWaitAnim(boolean showORhide) {
        if (showORhide) {
            waitAnim.setAnimation(animation);
            waitAnim.setVisibility(View.VISIBLE);
        } else {
            waitAnim.setAnimation(null);
            waitAnim.setVisibility(View.INVISIBLE);
        }
    }

    //在此处预先查询身份证号的存在性
    private void preCheckID() {
        //添加旋转图片
        checkingAnim.setAnimation(animation);
        realtimeCheck.removeAllViews();
        realtimeCheck.addView(checkingAnim);
        realtimeCheck.addView(checkTextView);
        checkTextView.setText("查询中...");
        params = new HashMap<String, String>();
        params.put("ProImage", " ");
        params.put("ProName", " ");
        params.put("id_card_number", idNo);
        params.put("type", "Blood");
        accessWebService(url, nameSpace, methodName, params, ONCHECK);
    }

    //根据图片的路径生成合适的缩略图,原理是根据原图片的长宽来确定压缩比例,设置的固定目标大小是宽度=600
    private void showPic(String imgpath) {
        Bitmap compressedBitmap = ImageUtils.compressByPixels(imgpath, 512);
        Bitmap roundBitmap = ImageUtils.getRoundBitmap(compressedBitmap, ivUpload.getWidth());
        Log.e(TAG, "ivUpload的宽度=" + ivUpload.getWidth());
        ivUpload.setImageBitmap(roundBitmap);
        currentStatus.setText("点击以上传");
        btnAction = 2;
        //bitmap.recycle();imageview还在使用这个bitmap回收会出错
    }

    private String decodeImage(String filepath, int quality) {
        File processedJPEG = ImageUtils.compressByQualityAndSave(
                filepath, picturefolder.getAbsolutePath(), quality);
        //向图库添加图片
        MediaScannerConnection.scanFile(this, new String[]{processedJPEG.getAbsolutePath()}, null, null);
        String buffer = FileUtils.decodeFile2Base64(processedJPEG.getAbsolutePath(),
                new FileUtils.FileUploadCallBack() {
                    @Override
                    public void callBack(int process) {
                        //很可惜,该callback得到的并不是上传照片的进度,而是读取文件流的进度...
                    }
                });
        //此时的buffer长度就已经是文件大小了
        return buffer;
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
        picturefolder = new File(folder, "UploadImages");
        picturefolder.mkdir();//new File下不能创建文件夹
        File picture;
        switch (requestCode) {
            case 10:
                switch (resultCode) {
                    case 1://开始拍照,先将照片保存到SD卡,可以使得图片不至于太小
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        //uploadedFile初始化的第一种情况,指定位置
                        uploadedFile = picturefolder + File.separator + item + getTimeStamp() + ".jpg";
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
                                  String methodName, HashMap<String, String> params, final int tag) {

        WebServiceUtils.callWebservice(url, nameSpace, methodName,
                params, new WebServiceUtils.WebServiceCallBack() {
                    @Override
                    public void callBack(SoapObject resultObj) {
                        if (resultObj != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 0; i < resultObj.getPropertyCount(); i++) {
                                stringBuilder.append(resultObj.getProperty(i)).append("\r\n");
                            }
                            String result = stringBuilder.toString();
                            switch (tag) {
                                case ONCHECK:
                                    if (result.indexOf("参数无效") != -1) {
                                        checkTextView.setText("尊敬的客户,您好!");
                                    } else {
                                        checkTextView.setText(Html.fromHtml("<b>您需要登录</b>" +
                                                "<a href=\"http:www.chinancd.net\">中国慢病网</a>" +
                                                "<b>进行注册才可以使用本服务</b>"));
                                        checkTextView.setMovementMethod(LinkMovementMethod.getInstance());
                                        checkTextView.setAutoLinkMask(Linkify.ALL);
                                    }
                                    break;
                                case ONUPLOAD:
                                    if (result.indexOf("成功") != -1) {
                                        currentStatus.setText("上传成功");
                                    } else {
                                        currentStatus.setText("上传失败");
                                    }
                                    handleWaitAnim(false);
                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Message msg = new Message();
                                            msg.what = 521;
                                            mHandler.sendMessage(msg);
                                        }
                                    }, 2000);
                                    break;
                            }
                        } else {
                            currentStatus.setText("\t\t\t上传失败\n服务器无法连接");
                            checkTextView.setText("服务器无法连接");
                            Log.e(TAG, "获取的resultObj为空");
                            handleWaitAnim(false);
                        }
                        checkingAnim.setAnimation(null);
                        realtimeCheck.removeView(checkingAnim);
                    }
                });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_guide:
                guide.setVisibility(View.INVISIBLE);
                break;
            case R.id.ll_surface:
                clearInputFocus(idnumber);
                break;
            case R.id.iv_upload:
                if (!(idNo.equals("") || item.equals("0")) && btnAction != 2 && btnAction != 3) {
                    btnAction = 1;
                }
                clearInputFocus(idnumber);
                switch (btnAction) {
                    case 0:
                        Toast.makeText(getApplicationContext(),
                                "您没有填写完整的信息", Toast.LENGTH_SHORT).show();
                        break;
                    case 1://choose
                        Intent intent = new Intent(MainActivity.this, PopWindow.class);
                        startActivityForResult(intent, 10);
                        break;
                    case 2://uploading
                        params = new HashMap<String, String>();
                        params.put("ProImage", decodeImage(uploadedFile, 30));
                        params.put("ProName", getTimeStamp() + ".jpg");
                        params.put("id_card_number", idNo);
                        params.put("type", item);
                        currentStatus.setText("上传中...");
                        btnAction = 3;
                        handleWaitAnim(true);
                        accessWebService(url, nameSpace, methodName, params, ONUPLOAD);
                        break;
                    case 3://loading and click
                        Intent intentView = new Intent(Intent.ACTION_VIEW);
                        intentView.setDataAndType(Uri.fromFile(new File(uploadedFile)), "image/*");
                        startActivity(intentView);
                        break;
                }
                break;
        }
    }
}
