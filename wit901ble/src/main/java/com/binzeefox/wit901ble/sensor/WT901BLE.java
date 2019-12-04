package com.binzeefox.wit901ble.sensor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

/**
 * 维特智能十轴传感器
 * <p>
 * 直接获取一个device。扫描交给其他类处理。该类只关注已知传感器的连接、绑定和获取信息
 *
 * @author binze
 * 2019/11/6 12:08
 */
public class WT901BLE implements WitSensor {
    private static final String TAG = "WT901BLE";

    public static final String READ_UUID = "0000ffe4-0000-1000-8000-00805f9a34fb"; //数据id
    public static final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";   //监听id
    public static final String WRITE_UUID = "0000ffe9-0000-1000-8000-00805f9a34fb";    //貌似是写入id
    public static final String SERVICE_UUID = "0000ffe5-0000-1000-8000-00805f9a34fb";  //服务id

    private Context mCtx;   //上下文实例
    private BluetoothAdapter mAdapter;  //蓝牙适配器
    private BluetoothDevice mDevice;    //该设备
    private BluetoothGatt mGatt = null; //服务
    private boolean recording = true;  //是否返回记录数据
    private boolean connected = false;  //是否已经连接
    private float[][] curData = null;
    private static float[][] referenceData = null;  //归零参考点

    private Callback callback = new Callback() {
        @Override
        public void onConnect(BluetoothGatt gatt) {

        }

        @Override
        public void onDisconnect(BluetoothGatt gatt) {

        }

        @Override
        public void onConnectFailed(BluetoothGatt gatt, int code) {

        }

        @Override
        public void onServiceReady(BluetoothGatt gatt) {

        }
    };  //状态回调
    private OnReceiveDataCallback dataCallback = new OnReceiveDataCallback() {
        @Override
        public void onReceive(BluetoothGatt mac, byte[] packBuffer) {
        }
    };   //接受数据回调

    /**
     * 静态方法 将返回数据处理包装
     *
     * @return 最外层三个值分别为x、y、z的数据，各个数据分别为 加速度m/s，角速度 °/s，角度 °
     * @author binze 2019/11/7 10:45
     */
    public static SensorData readRawData(byte[] packBuffer) {
        float[] xData, yData, zData;
        xData = yData = zData = new float[3];   //0:加速度 m/s2, 1:角速度 °/s, 2:角度 °

        float[] fData = new float[9];
        if (packBuffer != null && packBuffer.length == 20 && packBuffer[1] == 0x61) {
            for (int i = 0; i < 9; i++)
                fData[i] = (((short) packBuffer[i * 2 + 3]) << 8) | ((short) packBuffer[i * 2 + 2] & 0xff);
            for (int i = 0; i < 3; i++)
                fData[i] = (float) (fData[i] / 32768.0 * 16.0);
            for (int i = 3; i < 6; i++)
                fData[i] = (float) (fData[i] / 32768.0 * 2000.0);
            for (int i = 6; i < 9; i++)
                fData[i] = (float) (fData[i] / 32768.0 * 180.0);
            xData = new float[]{fData[0], fData[3], fData[6]};
            yData = new float[]{fData[1], fData[4], fData[7]};
            zData = new float[]{fData[2], fData[5], fData[8]};
        }
        return new SensorData(new float[][]{xData, yData, zData});
    }

    public static SensorData readData(byte[] packBuffer) {
        float[] xData, yData, zData;
        xData = yData = zData = new float[3];   //0:加速度 m/s2, 1:角速度 °/s, 2:角度 °

        float[] fData = new float[9];
        if (packBuffer != null && packBuffer.length == 20 && packBuffer[1] == 0x61) {
            for (int i = 0; i < 9; i++)
                fData[i] = (((short) packBuffer[i * 2 + 3]) << 8) | ((short) packBuffer[i * 2 + 2] & 0xff);
            for (int i = 0; i < 3; i++)
                fData[i] = (float) (fData[i] / 32768.0 * 16.0);
            for (int i = 3; i < 6; i++)
                fData[i] = (float) (fData[i] / 32768.0 * 2000.0);
            for (int i = 6; i < 9; i++)
                fData[i] = (float) (fData[i] / 32768.0 * 180.0);
            xData = new float[]{fData[0], fData[3], fData[6]};
            yData = new float[]{fData[1], fData[4], fData[7]};
            zData = new float[]{fData[2], fData[5], fData[8]};
        }

        if (referenceData != null)
            for (int i = 0; i < 3; i++) {
                xData[i] -= referenceData[0][i];
                yData[i] -= referenceData[1][i];
                zData[i] -= referenceData[2][i];
            }
        return new SensorData(new float[][]{xData, yData, zData});
    }

    /**
     * 获取过滤器
     *
     * @author binze 2019/11/7 12:14
     */
    public static List<ScanFilter> getFilters() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                .build();
        filters.add(filter);
        return filters;
    }

    /**
     * 构造器
     *
     * @param context 上下文
     * @param device  传感器设备
     */
    public WT901BLE(Context context, BluetoothDevice device) {
        mCtx = context.getApplicationContext();
        BluetoothManager manager = (BluetoothManager) mCtx.getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = manager.getAdapter();
        mDevice = device;
    }

    /**
     * 构造器
     *
     * @param context 上下文
     * @param mac     传感器物理地址
     */
    public WT901BLE(Context context, String mac) {
        mCtx = context.getApplicationContext();
        BluetoothManager manager = (BluetoothManager) mCtx.getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = manager.getAdapter();
        mDevice = mAdapter.getRemoteDevice(mac);
    }

    /**
     * 获取Gatt回调，为继承类做的方法
     *
     * @author binze 2019/11/8 9:12
     */
    protected GattCallback getGattCallback() {
        return new GattCallback();
    }

    /**
     * 获取设备实例
     * @author binze 2019/11/14 9:58
     */
    public BluetoothDevice getDevice(){
        return mDevice;
    }

    /**
     * 连接传感器
     *
     * @author binze 2019/11/7 9:05
     */
    @Override
    public void connect() {
        if (mGatt != null) mGatt.connect();
        else mDevice.connectGatt
                (mCtx, true, new GattCallback(), BluetoothDevice.TRANSPORT_LE);
    }

    /**
     * 断开连接
     *
     * @author binze 2019/11/7 9:05
     */
    @Override
    public void disconnect() {
        if (mGatt == null) return;
        mGatt.disconnect();
        connected = false;
    }

    /**
     * 是否连接
     *
     * @author binze 2019/11/8 9:46
     */
    @Override
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH})
    public boolean isConnected() {
        if (connected) return true;
        else return mGatt != null && mGatt.getConnectionState(mDevice) == STATE_CONNECTED;
    }

    /**
     * 回收
     *
     * @author binze 2019/11/7 9:05
     */
    @Override
    public void close() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
        connected = false;
        mGatt = null;
        mDevice = null;
    }

    /**
     * 注册并监听数据变化
     *
     * @param callback 接受数据回调
     * @author binze 2019/11/7 8:56
     */
    @Override
    public void subscribe(@NonNull OnReceiveDataCallback callback) {
        if (mGatt == null) {
            Log.w(TAG, "subscribe: Gatt is null !!!");
            return;
        }
        BluetoothGattService service = mGatt.getService(UUID.fromString(SERVICE_UUID));
        BluetoothGattCharacteristic characteristic
                = service.getCharacteristic(UUID.fromString(READ_UUID));
        mGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DESCRIPTOR_UUID));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(descriptor);
        dataCallback = callback;
    }

    /**
     * 暂停记录
     *
     * @author binze 2019/11/7 10:48
     */
    @Override
    public void pause() {
        recording = false;
    }

    /**
     * 恢复记录
     *
     * @author binze 2019/11/7 10:48
     */
    @Override
    public void resume() {
        recording = true;
    }

    /**
     * 校准
     * <p>
     * 校准结果通过{@link #readData(byte[])} 表现
     *
     * @author binze 2019/11/11 8:21
     */
    @Override
    public void calibration() {
        referenceData = curData;
    }

    /**
     * 获取归零数据
     *
     * @author binze 2019/11/11 8:31
     */
    public float[][] getReferenceData() {
        return referenceData;
    }

    /**
     * 注册监听器
     *
     * @author binze 2019/11/7 8:46
     */
    public void setCallback(@NonNull Callback callback) {
        this.callback = callback;
    }

    /**
     * 私有 gatt回调
     *
     * @author binze 2019/11/7 8:35
     */
    protected class GattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == GATT_SUCCESS) {
                Log.d(TAG, "onConnectionStateChange \nSuccess: " +
                        "\ngatt = " + gatt +
                        "\nstatus = " + status +
                        "\nnewState = " + newState);
                if (newState == STATE_CONNECTED) {
                    gatt.discoverServices();
                    connected = true;
                    callback.onConnect(gatt);
                } else if (newState == STATE_DISCONNECTED) {
                    connected = false;
                    callback.onDisconnect(gatt);
                }
            } else {
                connected = false;
                callback.onConnectFailed(gatt, status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != GATT_SUCCESS) return;
            mGatt = gatt;
            callback.onServiceReady(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (recording) {
                curData = readData(characteristic.getValue()).getData();
                dataCallback.onReceive(gatt, characteristic.getValue());
            }
        }
    }

    /**
     * 状态回调
     *
     * @author binze 2019/11/7 8:50
     */
    public interface Callback {
        void onConnect(BluetoothGatt gatt);

        void onDisconnect(BluetoothGatt gatt);

        void onConnectFailed(BluetoothGatt gatt, int code);

        void onServiceReady(BluetoothGatt gatt);
    }

    /**
     * 封装的数据库数据类
     *
     * @author binze
     * 2019/11/8 10:16
     */
    public static class SensorData {

        private long timeStamp;
        private float[][] data;

        public SensorData(@NonNull float[][] data) {
            timeStamp = new Date().getTime();
            this.data = data;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public float[][] getData() {
            return data;
        }
    }
}
