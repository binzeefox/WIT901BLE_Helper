package com.binzeefox.wit901ble.scanner;

import android.bluetooth.le.ScanFilter;

import java.util.List;

/**
 * 扫描器接口
 * <p>
 * {@link #scan(List)}  扫描
 * {@link #stop()}  停止扫描
 *
 * @author binze
 * 2019/11/7 9:31
 */
public interface SensorScanner {

    void scan(List<ScanFilter> filters);

    void stop();
}
