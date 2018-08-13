package com.example.android.iitp;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.android.iitp.CaptureHighSpeedVideoMode;
import com.example.android.iitp.R;

import java.util.UUID;

public class VideoHighFPSActivity extends AppCompatActivity {

    // Activity which starts when you press "CAMERA MODE" button on the MainActivity Screen

    public static Bitmap action;
    public static SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_high_fps);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CaptureHighSpeedVideoMode.newInstance())
                    .commit();
        }
    }
}