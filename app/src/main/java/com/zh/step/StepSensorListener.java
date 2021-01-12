package com.zh.step;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * @Author: zh
 * @Time 2021/1/8 0008 14:20
 * @Email: zhaohang@zhizhangyi.com
 * @Describe:
 */
public class StepSensorListener implements SensorEventListener {
    //存放三轴数据
    float[] oriValues = new float[3];
    final int ValueNum = 4;
    //用于存放计算阈值的波峰波谷差值
    float[] tempValue = new float[ValueNum];
    //是否上升的标志位
    boolean isDirectionUp = false;
    int tempCount = 0;
    //持续上升次数
    int continueUpCount = 0;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;
    //当前传感器的值
    float gravityNew = 0;
    //上次传感器的值
    float gravityOld = 0;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    final float InitialValue = (float) 1.3;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //此次波峰的时间
    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
    //当前的时间
    long timeOfNow = 0;
    //初始阈值
    float ThreadValue = (float) 2.0;
    //波峰波谷时间差
    int TimeInterval = 250;

    private StepCountListener stepCountListener;

    public void initListener(StepCountListener stepCountListener) {
        this.stepCountListener = stepCountListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) { //step counter
            //event.values[0]为计步历史累加值
            if (event.values != null) {
                stepCountListener.stepCount(event.values[0], Sensor.TYPE_STEP_COUNTER);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) { //step detector
            //event.values[0]为1.0f
            if (event.values != null && event.values[0] == 1.0f) {
                stepCountListener.stepCount(event.values[0], Sensor.TYPE_STEP_DETECTOR);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { //加速度
            for (int i = 0; i < 3; i++) {
                oriValues[i] = event.values[i];
            }
            gravityNew = (float) Math.sqrt(oriValues[0] * oriValues[0]
                    + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);
            detectorNewStep(gravityNew);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     * 检测步子，并开始计步
     * 1.传入sersor中的数据
     * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
     * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
     * */
    public void detectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (detectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();
                if (timeOfNow - timeOfLastPeak >= TimeInterval
                        && (peakOfWave - valleyOfWave >= ThreadValue)) {
                    timeOfThisPeak = timeOfNow;
                    stepCountListener.stepCount(1, Sensor.TYPE_ACCELEROMETER);
                }
                if (timeOfNow - timeOfLastPeak >= TimeInterval
                        && (peakOfWave - valleyOfWave >= InitialValue)) {
                    timeOfThisPeak = timeOfNow;
                    ThreadValue = peakValleyThread(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    /*
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于20
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     * */
    public boolean detectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 || oldValue >= 20)) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    /*
     * 阈值的计算
     * 1.通过波峰波谷的差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中
     * 3.在将数组传入函数averageValue中计算阈值
     * */
    public float peakValleyThread(float value) {
        float tempThread = ThreadValue;
        if (tempCount < ValueNum) {
            tempValue[tempCount] = value;
            tempCount++;
        } else {
            tempThread = averageValue(tempValue, ValueNum);
            for (int i = 1; i < ValueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[ValueNum - 1] = value;
        }
        return tempThread;

    }

    /*
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     * */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / ValueNum;
        if (ave >= 8)
            ave = (float) 4.3;
        else if (ave >= 7 && ave < 8)
            ave = (float) 3.3;
        else if (ave >= 4 && ave < 7)
            ave = (float) 2.3;
        else if (ave >= 3 && ave < 4)
            ave = (float) 2.0;
        else {
            ave = (float) 1.3;
        }
        return ave;
    }
}
