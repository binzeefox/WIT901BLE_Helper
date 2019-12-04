# WIT901BLE_Helper
扫描和链接WIT901BLE 传感器的安卓工具库
作者非维特智能员工，项目应用到的东西也通过公开途径获得，且非盈利用途。

## 权限
``` <uses-permission android:name="android.permission.BLUETOOTH" /> ```\
``` <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" /> ```

``` <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> ```\
``` <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /> ```

## 使用

- 扫描需要开启定位，否则无法扫描Ble设备

- 已经打包Jitpack依赖，详见下面引用

## 包

- scanner 内含一个封装了蓝牙扫描器的ListView，确保蓝牙开启且位置服务开启的情况下即可直接使用，详情请阅读源码注释

- sensor 传感器包，包含一个封装维特智能芯片的抽象类、接口等，目前只实现了一个WT901BLE04传感器的封装。在获取到设备实例后，可以直接用MAC或者实例 实例化一个传感器实例

- utils 工具包，，不过貌似没什么用


## 引用

[![](https://jitpack.io/v/binzeefox/WIT901BLE_Helper.svg)](https://jitpack.io/#binzeefox/WIT901BLE_Helper)\
``` implementation 'com.github.binzeefox:WIT901BLE_Helper:Tag' ```
