package com.binzeefox.wit901ble.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.binzeefox.wit901ble.sensor.WT901BLE;
import com.binzeefox.wit901ble.utils.BleUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

/**
 * 封装Ble蓝牙设备扫描的ListView, 只会扫描
 * @author binze
 * 2019/11/7 9:46
 */
public class BleScannerListView extends ListView implements SensorScanner {
    private static final String TAG = "BleScannerListView";
    private static final int TAG_TYPE = View.generateViewId();

    private List<ScanResult> mData = new ArrayList<>();    //数据
    private final ListAdapter mAdapter = new ListAdapter();  //适配器
    private final BluetoothLeScanner mScanner;  //扫描器
    private final ScanListener scanListener = new ScanListener();   //扫描监听
    private boolean scanning = false; //是否在扫描
    private OnClickDeviceListener listener = new OnClickDeviceListener() {
        @Override
        public void onClick(BluetoothDevice device) {
        }
    };  //点击监听器

    public BleScannerListView(Context context) {
        this(context, null);
    }
    public BleScannerListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public BleScannerListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        mScanner = adapter.getBluetoothLeScanner();
        setAdapter(mAdapter);
    }

    /**
     * 返回是否在扫描中
     * @author binze 2019/11/7 9:43
     */
    public boolean isScanning() {
        return scanning;
    }

    /**
     * 设置点击监听器
     * @author binze 2019/11/7 9:53
     */
    public void setOnDeviceClickListener(@NonNull OnClickDeviceListener listener){
        this.listener = listener;
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BleScannerListView.this.listener.onClick(mData.get(position).getDevice());
            }
        });
    }

    /**
     * 清除记录
     *
     * @author binze 2019/11/7 9:37
     */
    public void clear() {
        mData.clear();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 扫描(无参)
     * @author binze 2019/11/7 9:49
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN})
    public void scan(){
        mScanner.startScan(scanListener);
        scanning = true;
    }

    /**
     * 带过滤扫描
     * @author binze 2019/11/7 12:26
     */
    @Override
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN})
    public void scan(List<ScanFilter> filters) {
        mScanner.startScan(filters
                , new ScanSettings.Builder().build(), scanListener);
        scanning = true;
    }

    /**
     * 仅扫描传感器
     * @author binze 2019/12/4 10:49
     */
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN})
    public void scanSensor(){
        List<ScanFilter> filters = WT901BLE.getFilters();
        scan(filters);
    }

    /**
     * 停止扫描
     * @author binze 2019/11/7 12:26
     */
    @Override
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_ADMIN})
    public void stop() {
        mScanner.stopScan(scanListener);
        scanning = false;
    }

    /**
     * 点击监听器
     * @author binze 2019/11/7 9:52
     */
    public interface OnClickDeviceListener{
        void onClick(BluetoothDevice device);
    }

    /**
     * 内部适配器
     *
     * @author binze 2019/11/7 9:28
     */
    protected class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH})
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mData.isEmpty()) {
                if (convertView != null && "nodata".equals(convertView.getTag(TAG_TYPE)))
                    convertView = null;
                return getItemView(position, convertView, parent);
            }
            @SuppressLint("ViewHolder") View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            view.setTag(TAG_TYPE, "nodata");

            TextView textView = view.findViewById(android.R.id.text1);
            textView.setGravity(View.TEXT_ALIGNMENT_CENTER | Gravity.CENTER_VERTICAL);
            textView.setText("未发现设备");
            return view;
        }

        /**
         * 真实的getView业务实现
         *
         * @author binze 2019/11/7 9:20
         */
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH})
        private View getItemView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            } else {
                view = convertView;
            }
            TextView nameField, macField;
            nameField = view.findViewById(android.R.id.text1);
            macField = view.findViewById(android.R.id.text2);
            BluetoothDevice device = mData.get(position).getDevice();
            String name = device.getName();
            if (TextUtils.isEmpty(name)) {
                BleUtil.BleAdvertisedData advertisedData = BleUtil
                        .parseAdertisedData(Objects.requireNonNull(mData.get(position).getScanRecord()).getBytes());
                name = advertisedData.getName();
            }
            //FIXME 获取的所有传感器名称均为空
            nameField.setText(TextUtils.isEmpty(name) ? "未知设备" : name);
            macField.setText(device.getAddress());
            return view;
        }
    }

    /**
     * 扫描监听器
     *
     * @author binze 2019/11/7 9:35
     */
    protected class ScanListener extends ScanCallback {

        ScanListener() {
            super();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!mData.contains(result)) {
                mData.add(result);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: errorCode = " + errorCode);
        }
    }
}
