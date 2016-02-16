package com.fabworks.macnica.uzuki;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import info.izumin.android.bletia.BletiaException;

public class UzukiActivity extends AppCompatActivity implements View.OnClickListener {
    private final UzukiActivity self = this;

    private KonashiManager mKonashiManager;

    private TextView mResultText;

    private Handler mHandler = new Handler();
    private boolean posting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uzuki);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        findViewById(R.id.btn_find).setOnClickListener(this);
        mResultText = (TextView) findViewById(R.id.text_read);

        mKonashiManager = new KonashiManager(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mKonashiManager.addListener(mKonashiListener);
        refreshViews();
    }

    @Override
    protected void onPause() {
        mKonashiManager.removeListener(mKonashiListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mKonashiManager.isConnected()){
                    mKonashiManager.reset()
                            .then(new DoneCallback<BluetoothGattCharacteristic>() {
                                @Override
                                public void onDone(BluetoothGattCharacteristic result) {
                                    mKonashiManager.disconnect();
                                }
                            });
                }
            }
        }).start();
        super.onDestroy();
    }

    private void refreshViews() {
        boolean isReady = mKonashiManager.isReady();
        findViewById(R.id.btn_find).setVisibility(!isReady ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_start).setVisibility(isReady ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_stop).setVisibility(isReady ? View.VISIBLE : View.GONE);
        mResultText.setVisibility(isReady ? View.VISIBLE : View.GONE);
    }

    private void readData() {

        byte[] data1 = {0x31, 0x0B};
        byte[] data2 = {0x2D, 0x08};
        byte[] data3 = {0x24, 0x20};
        byte[] data4 = {0x27, (byte)0xF0};
        byte[] data5 = {0x32};

        mKonashiManager.i2cStartCondition()
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cWritePipe(data1.length, data1, (byte) Adxl345.ACC_SENSOR_ADDRESS))
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cWritePipe(data2.length, data2, (byte) Adxl345.ACC_SENSOR_ADDRESS))
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cWritePipe(data3.length, data3, (byte) Adxl345.ACC_SENSOR_ADDRESS))
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cWritePipe(data4.length, data4, (byte) Adxl345.ACC_SENSOR_ADDRESS))
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cWritePipe(data5.length, data5, (byte) Adxl345.ACC_SENSOR_ADDRESS))
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStopConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cStartConditionPipe())
                .then(mKonashiManager.<BluetoothGattCharacteristic>i2cReadPipe(6, (byte) Adxl345.ACC_SENSOR_ADDRESS))
                .then(new DoneCallback<byte[]>() {
                    @Override
                    public void onDone(byte[] result) {
                        double x = (double) ((((int) result[1]) << 8) | result[0]) / 256.0;
                        double y = (double) ((((int) result[3]) << 8) | result[2]) / 256.0;
                        double z = (double) ((((int) result[5]) << 8) | result[4]) / 256.0;
                        mResultText.setText("x:" + x + " y:" + y + " z:" + z);
                        mKonashiManager.i2cStopCondition();
                    }
                })
                .fail(new FailCallback<BletiaException>() {
                    @Override
                    public void onFail(BletiaException result) {
                        Toast.makeText(self, result.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_find:
                mKonashiManager.find(this);
                break;
            case R.id.btn_start:
                if(!posting) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            readData();
                            mHandler.postDelayed(this, 1000);
                        }
                    }, 1000);
                    posting = true;
                }

                break;
            case R.id.btn_stop:
                if(posting) {
                    mHandler.removeCallbacksAndMessages(null);
                }
                break;
        }
    }

    private final KonashiListener mKonashiListener = new KonashiListener() {
        @Override
        public void onConnect(KonashiManager manager) {
            refreshViews();
            mKonashiManager.i2cMode(Konashi.I2C_ENABLE_100K);
        }

        @Override
        public void onDisconnect(KonashiManager manager) {
            refreshViews();
        }

        @Override
        public void onError(KonashiManager manager, BletiaException e) {

        }

        @Override
        public void onUpdatePioOutput(KonashiManager manager, int value) {

        }

        @Override
        public void onUpdateUartRx(KonashiManager manager, byte[] value) {

        }

        @Override
        public void onUpdateBatteryLevel(KonashiManager manager, int level) {

        }

        @Override
        public void onUpdateSpiMiso(KonashiManager manager, byte[] value) {

        }
    };
}
