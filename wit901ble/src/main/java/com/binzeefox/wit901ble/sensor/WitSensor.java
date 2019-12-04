package com.binzeefox.wit901ble.sensor;

import android.bluetooth.BluetoothGatt;

/**
 * 维特传感器
 * <p>
 * {@link #connect()}   连接传感器
 * {@link #disconnect()}    断开连接
 * {@link #isConnected()}    是否连接
 * {@link #close()} 关闭并回收传感器
 * {@link #subscribe(OnReceiveDataCallback)}    注册监听
 * {@link #pause()}    暂停数据流
 * {@link #resume()}    继续数据流
 * {@link #calibration()}    校准归零
 * {@link OnReceiveDataCallback}    数据接收回调
 *
 * @author binze 2019/11/7 8:43
 */
public interface WitSensor {

    void connect();

    void disconnect();

    boolean isConnected();

    void close();

    void subscribe(OnReceiveDataCallback callback);

    void pause();

    void resume();

    void calibration();

    interface OnReceiveDataCallback {
        void onReceive(BluetoothGatt gatt, byte[] packBuffer);
    }
}
