package com.fabworks.macnica.uzuki;

import android.bluetooth.BluetoothGattCharacteristic;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fabworks.macnica.uzuki.sensor.Adxl345;
import com.fabworks.macnica.uzuki.sensor.Si1145;
import com.fabworks.macnica.uzuki.sensor.Si7013;
import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.KonashiManager;

import org.jdeferred.AlwaysCallback;
import org.jdeferred.DoneCallback;
import org.jdeferred.DonePipe;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import butterknife.Bind;
import butterknife.ButterKnife;
import info.izumin.android.bletia.BletiaException;

public class UzukiActivity extends AppCompatActivity implements View.OnClickListener {
    private final UzukiActivity self = this;

    private KonashiManager mKonashiManager;

    @Bind(R.id.btn_find)
    Button mFindButton;
    @Bind(R.id.btn_disconnect)
    Button mDisconnectButton;

    @Bind(R.id.read_x)
    TextView mXText;
    @Bind(R.id.read_y)
    TextView mYText;
    @Bind(R.id.read_z)
    TextView mZText;
    @Bind(R.id.read_proximity)
    TextView mProximityText;
    @Bind(R.id.image_weather)
    ImageView mWeatherImage;
    @Bind(R.id.read_humid)
    TextView mHumidText;
    @Bind(R.id.read_temperature)
    TextView mTemperatureText;
    @Bind(R.id.read_discomfort)
    TextView mDiscomfortText;
    @Bind(R.id.label_discomfort)
    TextView mDiscomfortLabel;

    private Handler mHandler = new Handler();
    private boolean posting;

    private double mRh;
    private double mTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uzuki);

        mXText = (TextView)findViewById(R.id.read_x);
        mYText = (TextView)findViewById(R.id.read_y);
        mZText = (TextView)findViewById(R.id.read_z);

        ButterKnife.bind(this);

        mFindButton.setOnClickListener(this);
        mDisconnectButton.setOnClickListener(this);

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
                disconnectKonashi();
            }
        }).start();
        super.onDestroy();
    }

    private void refreshViews() {
        boolean isReady = mKonashiManager.isReady();
        mFindButton.setVisibility(!isReady ? View.VISIBLE : View.GONE);
        mDisconnectButton.setVisibility(isReady ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_sensor).setVisibility(isReady ? View.VISIBLE : View.GONE);
    }

    private void disconnectKonashi() {
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

    private void readUzuki() {
        Adxl345.<BluetoothGattCharacteristic>readAccelerometer(mKonashiManager)
                .then(new DonePipe<byte[], byte[], BletiaException, Void>() {
                    @Override
                    public Promise<byte[], BletiaException, Void> pipeDone(byte[] result) {
                        byte[] xBytes = {result[0], result[1]};
                        short xValue = ByteBuffer.wrap(xBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
                        byte[] yBytes = {result[2], result[3]};
                        short yValue = ByteBuffer.wrap(yBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
                        byte[] zBytes = {result[4], result[5]};
                        short zValue = ByteBuffer.wrap(zBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
                        double x = (double)(xValue) / 256.0;
                        double y = (double)(yValue) / 256.0;
                        double z = (double)(zValue) / 256.0;
                        String xString = String.format("%.6f", x);
                        String yString = String.format("%.6f", y);
                        String zString = String.format("%.6f", z);
                        mXText.setText(xString);
                        mYText.setText(yString);
                        mZText.setText(zString);
                        mKonashiManager.i2cStopCondition();
                        return Si1145.readAmbientLight(mKonashiManager);
                    }
                })
                .then(new DonePipe<byte[], byte[], BletiaException, Void>() {
                    @Override
                    public Promise<byte[], BletiaException, Void> pipeDone(byte[] result) {
                        double value = (double)((result[0] & 0xff) + (result[1] & 0xff) * 256) / 100.0;
                        int drawableId;
                        if(value < 4) {
                            //曇り
                            drawableId = R.drawable.weather_cloudy;
                        } else if(value >= 4 || value < 10) {
                            //曇り・晴れ
                            drawableId = R.drawable.weather_sunny_cloud;
                        } else if(value >= 10 || value < 30) {
                            //晴れ
                            drawableId = R.drawable.weather_sunny;
                        } else {
                            //快晴
                            drawableId = R.drawable.weather_heavy_sunny;
                        }
                        mWeatherImage.setImageDrawable(ContextCompat.getDrawable(self, drawableId));
                        mKonashiManager.i2cStopCondition();
                        return Si1145.readProximity(mKonashiManager);
                    }
                })
                .then(new DonePipe<byte[], byte[], BletiaException, Void>() {
                    @Override
                    public Promise<byte[], BletiaException, Void> pipeDone(byte[] result) {
                        double value = (double)((result[0] & 0xff) + (result[1] & 0xff) * 256);
                        value = Math.log(value);
                        String proximityString = String.format("%.6f", value);
                        mProximityText.setText(proximityString);
                        mKonashiManager.i2cStopCondition();
                        return Si7013.readHumid(mKonashiManager);
                    }
                })
                .then(new DonePipe<byte[], byte[], BletiaException, Void>() {
                    @Override
                    public Promise<byte[], BletiaException, Void> pipeDone(byte[] result) {
                        double value = (double) ((result[1] & 0xff) + (result[0] & 0xff) * 256) * 125.0 / 65536.0 - 6.0;
                        mRh = value;
                        String humidString = String.format("%.1f", value);
                        mHumidText.setText(humidString);
                        mKonashiManager.i2cStopCondition();
                        return Si7013.readTemperature(mKonashiManager);
                    }
                })
                .then(new DoneCallback<byte[]>() {
                    @Override
                    public void onDone(byte[] result) {
                        double value = (double)((result[1] & 0xff) + (result[0] & 0xff) * 256) * 175.72 / 65536.0 - 46.85;
                        mTemp = value;
                        double dcValue = 0.81 * mTemp + 0.01 * mRh * (0.99 * mTemp - 14.3) + 46.3;
                        String temperatureString = String.format("%.1f", value);
                        mTemperatureText.setText(temperatureString);
                        setDiscomfortText((int)dcValue);
                        mKonashiManager.i2cStopCondition();
                    }
                })
                .fail(new FailCallback<BletiaException>() {
                    @Override
                    public void onFail(BletiaException result) {
                        Toast.makeText(self, result.toString(), Toast.LENGTH_SHORT).show();
                    }
                })
                .always(new AlwaysCallback<byte[], BletiaException>() {
                    @Override
                    public void onAlways(Promise.State state, byte[] resolved, BletiaException rejected) {
                        readUzuki();
                    }
                });
    }

    public void setDiscomfortText(int value) {
        int color;
        if (value<55){ // 寒い
            color = Color.CYAN;
        }
        else if(value>=55 && value<60){ // 肌寒い
            color = Color.BLUE;
        }
        else if(value>=60 && value<65) { // 何も感じない
            color = Color.GREEN;
        }
        else if (value>=65 && value<70) { // 快い
            color = Color.GREEN;
        }
        else if (value>=70 && value<75) { // 暑くない
            color = Color.YELLOW;
        }
        else if (value>=75 && value<80) { // やや暑い
            color = ContextCompat.getColor(this, R.color.orange);
        }
        else if (value>=80 && value<85) { // 暑くて汗がでる
            color = Color.RED;
        }
        else{ // 暑くてたまらない
            color = ContextCompat.getColor(this, R.color.purple);
        }
        mDiscomfortLabel.setTextColor(color);
        mDiscomfortText.setTextColor(color);
        mDiscomfortText.setText(String.valueOf(value));

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_find:
                mKonashiManager.find(this);
                break;
            case R.id.btn_disconnect:
                disconnectKonashi();
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
                    .then(Si1145.<BluetoothGattCharacteristic>setLed1Current(mKonashiManager))
//                    .then(Si7013.<BluetoothGattCharacteristic>initialize(mKonashiManager))
                    .then(mInitializeDoneCallback)
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

    DoneCallback<BluetoothGattCharacteristic> mInitializeDoneCallback = new DoneCallback<BluetoothGattCharacteristic>() {
        @Override
        public void onDone(BluetoothGattCharacteristic result) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    readUzuki();
                }
            });
            posting = true;
        }
    };
}
