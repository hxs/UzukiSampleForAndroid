package com.fabworks.macnica.uzuki.shield;

import android.bluetooth.BluetoothGattCharacteristic;

import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.Promise;

import info.izumin.android.bletia.BletiaException;

/**
 * Created by e10dokup on 2016/02/16
 **/
public class Uzuki {
    private static final String TAG = Uzuki.class.getSimpleName();
    private final Uzuki self = this;

    public static final int ACC_SENSOR_ADDRESS = 0x1D;
    
    public static Promise<byte[], BletiaException, Void> readAccelerometer(KonashiManager manager) {
        byte[] data1 = {0x31, 0x0B};
        byte[] data2 = {0x2D, 0x08};
        byte[] data3 = {0x24, 0x20};
        byte[] data4 = {0x27, (byte)0xF0};
        byte[] data5 = {0x32};


        return manager.<BluetoothGattCharacteristic>i2cWrite(data1.length, data1, (byte) ACC_SENSOR_ADDRESS)
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data2.length, data2, (byte) ACC_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data3.length, data3, (byte) ACC_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data4.length, data4, (byte) ACC_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data5.length, data5, (byte) ACC_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cReadPipe(6, (byte) ACC_SENSOR_ADDRESS));
    }
}