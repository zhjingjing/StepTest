package com.zh.step;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    private final static String TAG = "MainActivity";
    SensorManager manager;
    private Sensor stepCountSensor;
    private Sensor stepDetectorSensor;
    private Sensor accSensor;
    private float stepCount;
    private TextView textView;
    private float lastCount = 0;//上次关机后存储的值。
    private long lastTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        textView = findViewById(R.id.tv_step);
        stepCountSensor = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        stepDetectorSensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        accSensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        stepSensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        manager.registerListener(new SensorListener(), stepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(new SensorListener(), stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(new SensorListener(), accSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        } else {
//            Toast.makeText(this, "不支持计步器", Toast.LENGTH_LONG).show();
//        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(new MyBroadCast(), intentFilter);

//        handler.postAtTime(testRunnable, "action", SystemClock.uptimeMillis() + 1000);
//        handler.postAtTime(testRunnable2, "action", SystemClock.uptimeMillis() + 1000);
//
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                handler.removeCallbacksAndMessages("action");
//            }
//        }, 10000);
    }

    private class SensorListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                //TYPE_STEP_COUNTER event.values[0]为计步历史累加值
                // TYPE_STEP_DETECTOR event.values[0]=1.0f
                if (event.values != null) {
                    stepCount = event.values[0];
                    Log.e(TAG, "onSensorChanged: 当前步数：" + stepCount);
                    textView.setText("onSensorChanged: 当前步数：" + stepCount);
                } else {
                    Log.e(TAG, "event.values == null");
                }
            }else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){
                Log.e(TAG, "TYPE_STEP_DETECTOR");
            }else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                final float alpha = 0.8f;

                float x=event.values[0];
                float y=event.values[1];
                float z=event.values[2];
                Log.e(TAG, "x="+x+ " y="+y+" z="+z);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }


    public boolean isSupportStep() {
        List<Sensor> list = manager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : list) {
            Log.e(TAG, "initData: " + sensor.getName());
            if (sensor.getName().equals("step counter")) {
                return true;
            }
        }
        return false;
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
}
