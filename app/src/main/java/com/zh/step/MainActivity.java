package com.zh.step;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;
import static android.content.Intent.ACTION_USER_PRESENT;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity2";
    SensorManager manager;
    private Sensor sensor;
    private float stepCount;
    private TextView textView;
    private TextView tvDesc;
    private float lastCount = 0;//上次关机后存储的值。
    private long lastTime;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        textView = findViewById(R.id.tv_step);
        tvDesc = findViewById(R.id.tv_step_desc);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION}, 1101);
        } else {
            registerStepListener();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(new MyBroadCast(), intentFilter);

    }

    public void registerStepListener() {
        sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        tvDesc.setText("当前是Step counter 传感器：\n记录的是开机后的总步数，延迟<=10s ");
        if (sensor == null) {
            sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            tvDesc.setText("当前是Step detector 传感器：\n每次步数+1，延迟<=2s ");
        }
        if (sensor == null) {
            sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            tvDesc.setText("当前是加速度传感器 ：\n每次步数+1，");
        }

        StepSensorListener stepEventListener = new StepSensorListener();
        stepEventListener.initListener((count, sensorType) -> {
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    stepCount += count;
                    Log.d(TAG, "TYPE_ACCELEROMETER  onSensorChanged: 当前步数：" + stepCount);
                    textView.setText("当前步数：" + stepCount);
                case Sensor.TYPE_STEP_DETECTOR:
                    stepCount += count;
                    Log.d(TAG, "TYPE_STEP_DETECTOR  onSensorChanged: 当前步数：" + stepCount);
                    textView.setText("当前步数：" + stepCount);
                    break;
                case Sensor.TYPE_STEP_COUNTER:
                    stepCount = count;
                    Log.d(TAG, "TYPE_STEP_COUNTER  onSensorChanged: 当前步数：" + stepCount);
                    textView.setText("当前步数：" + stepCount);
                    break;
            }
        });
        manager.registerListener(stepEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1101 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerStepListener();
        }

    }

    private int flag = -1;

    /**
     * 广播监听
     * 亮灭屏 开关机
     */
    public class MyBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED:
                    Log.e(TAG, "手机开机了");
                    break;
                case Intent.ACTION_SHUTDOWN:
                    flag = 1;//关机
                    saveData(stepCount);
                    Log.e(TAG, "手机关机了");
                    break;

                case ACTION_SCREEN_ON:
                    flag = 2;//亮屏
                    lastCount = stepCount;
                    lastTime = System.currentTimeMillis();
                    Log.e(TAG, "亮屏" + " 步数:" + stepCount);
                    break;
                case ACTION_SCREEN_OFF:
                    flag = 3;//息屏
                    lastCount = stepCount;
                    lastTime = System.currentTimeMillis();
                    Log.e(TAG, "息屏" + " 步数:" + stepCount);
                    break;
                case ACTION_USER_PRESENT:
                    Log.e(TAG, "手机解锁" + " 步数:" + stepCount);
                    break;
                default:

                    break;
            }
        }
    }


    public void saveData(float lastCount) {
        SharedPreferences step = getSharedPreferences("step", Context.MODE_PRIVATE);
        step.edit().putFloat("lastData", lastCount);
        step.edit().commit();
    }


    public float getLastStep() {
        SharedPreferences step = getSharedPreferences("step", Context.MODE_PRIVATE);
        return step.getFloat("lastData", 0);
    }

    private Handler handler = new Handler();
    private TestRunnable testRunnable = new TestRunnable();
    private TestRunnable2 testRunnable2 = new TestRunnable2();

    private class TestRunnable implements Runnable {
        @Override
        public void run() {
            Log.e(TAG, "test1");
            handler.postAtTime(testRunnable, "action", SystemClock.uptimeMillis() + 1000);
        }
    }

    private class TestRunnable2 implements Runnable {
        @Override
        public void run() {
            Log.e(TAG, "test2");
            handler.postAtTime(testRunnable2, "action", SystemClock.uptimeMillis() + 1000);
        }
    }

    /**
     * @return 是否支持传感器
     */
    private boolean isSupportBodySensor(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean isSupport = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
                && (pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
                || pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR));
        Log.e(TAG, String.valueOf(isSupport));
        return isSupport;
    }
}
