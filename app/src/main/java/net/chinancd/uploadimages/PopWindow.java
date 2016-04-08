package net.chinancd.uploadimages;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

/**
 * 作者： 林思琴 时间： 16.2.16 - 15:04
 * 邮箱：hedefu999@163.com
 * 签名：面包会有的,linsiqin会来的
 */
public class PopWindow extends Activity implements View.OnClickListener {
    private static String TAG="PopWindow";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choosemethods);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        finish();       //触摸按钮外空间退出
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_take_photo:
                setResult(1);
                break;
            case R.id.btn_pick_photo:
                setResult(2);
                break;
            case R.id.btn_cancel:
                setResult(0);
                break;
            default:
                break;
        }
        finish();      // 点击任意项目后退出
    }
}
