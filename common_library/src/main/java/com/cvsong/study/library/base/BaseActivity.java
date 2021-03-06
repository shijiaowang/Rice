package com.cvsong.study.library.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.cvsong.study.library.net.httpservice.OkHttpRequestManage;
import com.cvsong.study.library.util.utilcode.util.ActivityUtils;
import com.cvsong.study.library.util.utilcode.util.ToastUtils;

import okhttp3.OkHttpClient;

/**
 * BaseActivity-->负责业务逻辑无关的常用功能封装
 * <p>
 * Created by chenweisong on 2018/3/9.
 */

public class BaseActivity extends AppCompatActivity {

    // 再点一次退出程序时间设置
    private static final long WAIT_TIME = 2000L;
    private long TOUCH_TIME = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkHttpRequestManage.cancel(this);//撤销网络请求
    }

    /**
     * 双击退出应用
     */
    protected void exitApp() {
        if (System.currentTimeMillis() - TOUCH_TIME < WAIT_TIME) {
            ActivityUtils.finishAllActivities();
            OkHttpRequestManage.cancelAll();//撤销全部网络请求
        } else {
            TOUCH_TIME = System.currentTimeMillis();
            ToastUtils.showShort("再按一次退出程序");
        }
    }

}
