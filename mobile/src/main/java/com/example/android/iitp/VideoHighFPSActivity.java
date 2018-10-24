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
import android.util.Log;

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
    private long dataStartTimeInMillisWear1;
    private long dataStartTimeInMillisWear2;
    private String wear1 = "";
    private String wear2 = "";
    private int nClients;
    private int wearCame = 0;

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

        Intent intent = getIntent();
        nClients = intent.getExtras().getInt("nClients");

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

            //Log.d("upgrade01",message);
            Log.e("MESSGGE",message);

            if (message.split(" ")[0].equals("Ping")) {
                Log.d("upgrade01",message.split(" ")[1]);
                String datapath;
                if (wearCame==nClients){
                    datapath = "/my_path_"+message.split(" ")[1];
                }else if (wear1.equals("")){
                    wear1 = message.split(" ")[1];
                    datapath = "/my_path_"+wear1;
                    wearCame += 1;
                }else{
                    wear2 = message.split(" ")[1];
                    datapath = "/my_path_"+wear2;
                    wearCame += 1;
                }
                new NewThread(datapath, message).start();
            }else if (message.split(" ")[0].equals("Data")){ //Data start time
                if (message.split(" ")[3].equals(wear1)) {
                    dataStartTimeInMillisWear1 = System.currentTimeMillis();
                }else{
                    dataStartTimeInMillisWear2 = System.currentTimeMillis();
                }
            }else { //Sensor data

                mCaptureHighSpeedVideoMode.setClients(nClients);

                String[] elements = message.split(":");
                String wearType = elements[15];
                wearType = wearType.substring(0,wearType.length() - 1);
                if (wearType.equals(wear1)){
                    mSensorDataModel = new SensorDataModel(message,dataStartTimeInMillisWear1);
                    mCaptureHighSpeedVideoMode.setSensorDataModel1(mSensorDataModel);
                }else{
                    mSensorDataModel = new SensorDataModel(message,dataStartTimeInMillisWear2);
                    mCaptureHighSpeedVideoMode.setSensorDataModel2(mSensorDataModel);
                }
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