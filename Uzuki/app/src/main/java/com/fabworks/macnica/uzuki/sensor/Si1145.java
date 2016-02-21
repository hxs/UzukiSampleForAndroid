package com.fabworks.macnica.uzuki.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.DonePipe;
import org.jdeferred.Promise;

import info.izumin.android.bletia.BletiaException;

/**
 * Created by e10dokup on 2016/02/17
 **/
public class Si1145 {
    private static final String TAG = Si1145.class.getSimpleName();
    private final Si1145 self = this;

    public static final int PROX_LIGHT_UV_SENSOR_ADDRESS = 0x60;

    public static <D> DonePipe<D, BluetoothGattCharacteristic, BletiaException, Void> initialize(final KonashiManager manager) {
        return new DonePipe<D, BluetoothGattCharacteristic, BletiaException, Void>() {
            @Override
            public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(D result) {
                byte[] data1 = {0x07, 0x17}; //
                byte[] data2 = {0x13, 0x00};
                byte[] data3 = {0x14, 0x02};
                byte[] data4 = {0x15, (byte)0x89};
                byte[] data5 = {0x16, 0x29};
                byte[] data6 = {0x17, (byte)(0x80 | 0x20 | 0x10 | 0x01)};
                byte[] data7 = {0x18, (byte)(0xA0 | 0x01)};

                return manager.<BluetoothGattCharacteristic>i2cStartCondition()
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data1.length, data1, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data2.length, data2, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data3.length, data3, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data4.length, data4, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data5.length, data5, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data6.length, data6, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data7.length, data7, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe());
            }
        };
    }

    public static Promise<byte[], BletiaException, Void> readAmbientLight(final KonashiManager manager) {
        byte[] data1 = {0x18, 0x06};
        byte[] data2 = {0x22};

        return manager.<BluetoothGattCharacteristic>i2cStartCondition()
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data1.length, data1, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data2.length, data2, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cReadPipe(6, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS));
    }

    public static Promise<byte[], BletiaException, Void> readProximity(final KonashiManager manager) {
        byte[] data1 = {0x18, 0x05};
        byte[] data2 = {0x26};

        return manager.<BluetoothGattCharacteristic>i2cStartCondition()
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data1.length, data1, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data2.length, data2, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cReadPipe(6, (byte) PROX_LIGHT_UV_SENSOR_ADDRESS));
    }
}