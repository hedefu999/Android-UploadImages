package net.chinancd.uploadimages;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 作者： 林思琴 时间： 16.2.21 - 9:20
 * 邮箱：hedefu999@163.com
 * 访问中国慢病网提供的webservice
 */
public class WebServiceUtils {
    private static final String TAG = "WebServiceUtils";

    /**
     * @param params  传递的参数,包括图片的base64编码和图片名称
     * @param methodName 方法名
     * @return 服务器返回的结果
     * 该方法扩展时应将参数放在map中,并加入methodname
     * 由于很多变量要在新线程中访问,所以声明为final
     */
    public static void callWebservice (String url,String nameSpace,String methodName, HashMap<String,String> params,
                                       final WebServiceCallBack webServiceCallBack) {
        final String soapAction = nameSpace+methodName;
        final HttpTransportSE httpTransSE = new HttpTransportSE(url);
        httpTransSE.debug = true;
        final SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        SoapObject soapObject = new SoapObject(nameSpace, methodName);
        if(params!=null){
            for(Iterator<Map.Entry<String,String>>iterator=params.entrySet().iterator();
                iterator.hasNext();){
                Map.Entry<String,String> entry=iterator.next();
                soapObject.addProperty(entry.getKey(),entry.getValue());
                Log.e(TAG,"向soapObject添加参数");
            }
        }else{
            Log.e(TAG,"该方法无参数");
        }
        //soapObject.addProperty("base64string", params.get("imgData"));
        //soapObject.addProperty("filename", params.get("fileName"));

        envelope.setOutputSoapObject(soapObject);//等于envelope.bodyOut=soapObject;
        envelope.dotNet = true;

        final Handler solveAfterTransHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                webServiceCallBack.callBack((SoapObject)msg.obj);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"线程开始进行");
                SoapObject resObj=null;
                try {
                    httpTransSE.call(soapAction, envelope);
                    Log.e(TAG,"httpTransSE calling finished!");
                    if (envelope.getResponse() != null) {
                        resObj = (SoapObject) envelope.bodyIn;
                    } else {
                        Log.e(TAG, "获取内容为空！");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }finally {
                    solveAfterTransHandler.sendMessage(
                            solveAfterTransHandler.obtainMessage(1,resObj));
                }
            }
        }).start();
    }
    public interface WebServiceCallBack{
        public void callBack(SoapObject resultObj);
    }
}
