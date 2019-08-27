package com.plateid.demo;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * @author user
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //leaks内存检测
//        LeakCanary.install(this);
    }
}
