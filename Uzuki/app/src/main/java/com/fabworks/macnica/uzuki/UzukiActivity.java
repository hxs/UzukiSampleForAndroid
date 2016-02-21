package com.fabworks.macnica.uzuki;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fabworks.macnica.uzuki.sensor.Adxl345;
import com.fabworks.macnica.uzuki.sensor.Si1145;
import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;

import info.izumin.android.bletia.BletiaException;

public class UzukiActivity extends AppCompatActivity implements View.OnClickListener {
    private final UzukiActivity self = this;

    private KonashiManager mKonashiManager;

    private TextView mXText;
    private TextView mYText;
    private TextView mZText;
    private TextView mAmbientLightText;
    private TextView mProximityText;
    private TextView mHumidText;
    private TextView mTemperatureText;

    private Handler mHandler = new Handler();
    private boolean posting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uzuki);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        findViewById(R.id.btn_find).setOnClickListener(this);
        mXText = (TextView)findViewById(R.id.read_x);
        mYText = (TextView)findViewById(R.id.read_y);
        mZText = (TextView)findViewById(R.id.read_z);
        mAmbientLightText = (TextView)findViewById(R.id.read_ambient_light);
        mProximityText = (TextView)findViewById(R.id.read_proximity);
        mHumidText = (TextView)findViewById(R.id.read_humid);
        mTemperatureText = (TextView)findViewById(R.id.read_temperature);

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
                    if(posting) {
                        mHandler.removeCallbacksAndMessages(null);
                    }
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
        mXText.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mYText.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mZText.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mAmbientLightText.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mProximityText.setVisibility(isReady ? View.VISIBLE : View.GONE);
    }

    private void readUzuki() {
        Adxl345.<BluetoothGattCharacteristic>readAccelerometer(mKonashiManager)
                .then(new DonePipe<byte[], byte[], BletiaException, Void>() {
                    @Override
                    public Promise<byte[], BletiaException, Void> pipeDone(byte[] result) {
                        int x = (result[1] << 8 | result[0]) >> 4;
                        int y = (result[3] << 8 | result[2]) >> 4;
                        int z = (result[5] << 8 | result[4]) >> 4;
                        mXText.setText(getString(R.string.label_x) + x);
                        mYText.setText(getString(R.string.label_y) + y);
                        mZText.setText(getString(R.string.label_z) + z);
                        mKonashiManager.i2cStopCondition();
                        return Si1145.readAmbientLight(mKonashiManager);
                    }
                })
                .then(new DonePipe<byte[], byte[], BletiaException, Void>() {
                    @Override
                    public Promise<byte[], BletiaException, Void> pipeDone(byte[] result) {
                        int value = (result[1] << 8 | result[0]);
                        mAmbientLightText.setText(getString(R.string.label_ambient_light) + value);
                        mKonashiManager.i2cStopCondition();
                        return Si1145.readProximity(mKonashiManager);
                    }
                })
                .then(new DoneCallback<byte[]>() {
                    @Override
                    public void onDone(byte[] result) {
                        int value = (result[1] << 8 | result[0]);
                        mProximityText.setText(getString(R.string.label_proximity) + value);
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
                            readUzuki();
                            mHandler.postDelayed(this, 1000);
                        }
                    }, 1000);
                    posting = true;
                }

                break;
            case R.id.btn_stop:
                if(posting) {
                    mHandler.removeCallbacksAndMessages(null);
                    posting = false;
                }
                break;
        }
    }

    private final KonashiListener mKonashiListener = new KonashiListener() {
        @Override
        public void onConnect(KonashiManager manager) {
            refreshViews();
            mKonashiManager.i2cMode(Konashi.I2C_ENABLE_100K)
                    .then(Adxl345.<BluetoothGattCharacteristic>initialize(mKonashiManager))
                    .then(Si1145.<BluetoothGattCharacteristic>initialize(mKonashiManager))
                    .fail(new FailCallback<BletiaException>() {
                        @Override
                        public void onFail(BletiaException result) {
                            Toast.makeText(self, result.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
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
