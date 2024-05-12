package com.example.camera0107;
import android.app.Application;
public class App extends Application {
    private static App mInstance;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
    public static App getInstance() {
        return mInstance;
    }

}
