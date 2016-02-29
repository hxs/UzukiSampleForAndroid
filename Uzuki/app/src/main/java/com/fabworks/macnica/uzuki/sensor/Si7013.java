package com.fabworks.macnica.uzuki.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.DonePipe;
import org.jdeferred.Promise;

import info.izumin.android.bletia.BletiaException;

/**
 * Created by e10dokup on 2016/02/22
 **/
public class Si7013 {
    private static final String TAG = Si7013.class.getSimpleName();
    private final Si7013 self = this;

    private static final int HUMID_TEMP_SENSOR_ADDRESS = 0x40;

    public static <D> DonePipe<D, BluetoothGattCharacteristic, BletiaException, Void>  initialize(final KonashiManager manager) {
        return new DonePipe<D, BluetoothGattCharacteristic, BletiaException, Void>() {
            @Override
            public Promise<BluetoothGattCharacteristic, BletiaException, Void> pipeDone(D result) {
                byte[] data = {(byte)0xfe};

                return manager.<BluetoothGattCharacteristic>i2cStartCondition()
                        .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data.length, data, (byte) HUMID_TEMP_SENSOR_ADDRESS))
                        .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe());
            }
        };
    }

    public static Promise<byte[], BletiaException, Void> readHumid(final KonashiManager manager) {
        byte[] data = {(byte)0xE5};

        return manager.<BluetoothGattCharacteristic>i2cStartCondition()
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data.length, data, (byte) HUMID_TEMP_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cReadPipe(2, (byte) HUMID_TEMP_SENSOR_ADDRESS));
    }

    public static Promise<byte[], BletiaException, Void> readTemperature(final KonashiManager manager) {
        byte[] data1 = {(byte)0xE0};
        byte[] data2 = {(byte)0xE3};

        return manager.<BluetoothGattCharacteristic>i2cStartCondition()
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data1.length, data1, (byte) HUMID_TEMP_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cWritePipe(data2.length, data2, (byte) HUMID_TEMP_SENSOR_ADDRESS))
                .then(manager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(manager.<BluetoothGattCharacteristic>i2cReadPipe(Konashi.I2C_DATA_MAX_LENGTH, (byte) HUMID_TEMP_SENSOR_ADDRESS));
    }
}