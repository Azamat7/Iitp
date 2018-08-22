package com.example.android.iitp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class VideoHighFPSActivity extends AppCompatActivity {

    // Activity which starts when you press "CAMERA MODE" button on the MainActivity Screen

    public static Bitmap action;
    public static SensorManager mSensorManager;
    private CaptureHighSpeedVideoMode mCaptureHighSpeedVideoMode;
    private SensorDataModel mSensorDataModel;
    private long dataStartTimeInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_high_fps);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCaptureHighSpeedVideoMode = CaptureHighSpeedVideoMode.newInstance();

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mCaptureHighSpeedVideoMode)
                    .commit();
        }

        //Register to receive local broadcasts, which we'll be creating in the next step//
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        VideoHighFPSActivity.Receiver messageReceiver = new VideoHighFPSActivity.Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    //Define a nested class that extends BroadcastReceiver
    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Upon receiving each message from the wearable, check it's type
            String message = intent.getStringExtra("message");
            if (message.equals("Data Start Ping!")){ //Data start time
                dataStartTimeInMillis = System.currentTimeMillis();
            }else { //Sensor data
                mSensorDataModel = new SensorDataModel(message,dataStartTimeInMillis);
                mCaptureHighSpeedVideoMode.saveToFiles(mSensorDataModel);
            }
        }
    }

    // To send message from phone to smartwatch

    class NewThread extends Thread {
        String path;
        String message;

        //Constructor for sending information to the Data Layer//
        NewThread(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {
            //Retrieve the connected devices, known as nodes//
            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    //Send the message//
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(VideoHighFPSActivity.this).sendMessage(node.getId(), path, message.getBytes());
                    Tasks.await(sendMessageTask);
                }
            } catch (Exception e) {
                e.printStackTrace(); }
        }
    }
}