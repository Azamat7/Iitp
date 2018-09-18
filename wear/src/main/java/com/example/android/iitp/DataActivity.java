package com.example.android.iitp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataActivity extends WearableActivity implements Serializable, SensorEventListener{
    public static final float PEAK_THRESHOLD = 10;
    public static final long TRIM_THRESHOLD = 1500;
    private static final String TAG = "message";

    private Button startButton;

    private static SensorManager mSensorManager;

    private long endOfJump = 0;
    private long startOfJump = 0;

    private ArrayList<Float> generalAccelerationAlongX;
    private ArrayList<Float> generalAccelerationAlongY;
    private ArrayList<Float> generalAccelerationAlongZ;

    private ArrayList<Float> gravityX;
    private ArrayList<Float> gravityY;
    private ArrayList<Float> gravityZ;

    private ArrayList<Float> gyroscopeX;
    private ArrayList<Float> gyroscopeY;
    private ArrayList<Float> gyroscopeZ;

    private ArrayList<Float> accelerometerData;
    private ArrayList<Float> horizontalAccelerationData;
    private ArrayList<Long> timeData;

    private static long lastUpdate = System.currentTimeMillis();
    private boolean isDataRecording = false;

    private long tTarget;

    private long startTime;

    private long pingSent;
    private int totalPings = 0;

    private String wearID = "";

    //ArrayList<float[]> rotationMatrixData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        startButton = (Button) findViewById(R.id.startButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDataRecording) {
                    sendPing();
                } else {
                    startButton.setText("Start");
                    onStopButton();
                }
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        generalAccelerationAlongX = new ArrayList<>();
        generalAccelerationAlongY = new ArrayList<>();
        generalAccelerationAlongZ = new ArrayList<>();

        gravityX = new ArrayList<>();
        gravityY = new ArrayList<>();
        gravityZ = new ArrayList<>();

        gyroscopeX = new ArrayList<>();
        gyroscopeY = new ArrayList<>();
        gyroscopeZ = new ArrayList<>();

        accelerometerData = new ArrayList<>();
        horizontalAccelerationData = new ArrayList<>();
        timeData = new ArrayList<>();

        //rotationMatrixData = new ArrayList<>();

//        String root = Environment.getExternalStorageDirectory().toString();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
//        String currentDateAndTime = sdf.format(new Date());
//        File myDir = new File(root + "/MSD/" + currentDateAndTime);
//        myDir.mkdirs();

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

    }

    private void onStartButton() {
        if (!isDataRecording) {
            isDataRecording = true;

            // sending ping message for data recording start from Client to Server phone.
            String message = "Data Start Ping!"+" "+wearID;
            String datapath = "/my_path";
            new SendMessage(datapath, message).start();

            Toast.makeText(getApplicationContext(), "Data recording started!", Toast.LENGTH_SHORT).show();

            startTime = System.currentTimeMillis();

            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
            //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT), SensorManager.SENSOR_DELAY_FASTEST);
            //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE), SensorManager.SENSOR_DELAY_FASTEST);

        }
    }

    private void sendPing(){
        String msd = "Ping "+wearID;
        String datapath = "/my_path"+wearID;
        pingSent = System.currentTimeMillis();
        new SendMessage(datapath, msd).start();
    }

    private float accelerometer_x, accelerometer_y, accelerometer_z;
    private float gravity_x, gravity_y, gravity_z;
    private float gyroscope_x, gyroscope_y, gyroscope_z;
    private float verticalAcceleration;

    private float[] mGravity;
    private float[] mGeomagnetic;
    private float[] Rot;

    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            accelerometer_x = event.values[0];
            accelerometer_y = event.values[1];
            accelerometer_z = event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravity_x = -event.values[0];
            gravity_y = -event.values[1];
            gravity_z = -event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscope_x = event.values[0];
            gyroscope_y = event.values[1];
            gyroscope_z = event.values[2];
        }

        float accelerometer_norm = (float) Math.sqrt(accelerometer_x * accelerometer_x + accelerometer_y * accelerometer_y + accelerometer_z * accelerometer_z);
        float gravity_norm = (float) Math.sqrt(gravity_x * gravity_x + gravity_y * gravity_y + gravity_z * gravity_z);
        float cosine = (accelerometer_x * gravity_x + accelerometer_y * gravity_y + accelerometer_z * gravity_z) / (gravity_norm * accelerometer_norm);
        verticalAcceleration = accelerometer_norm * (-cosine);
        float horizontalAcceleration = (float) Math.sqrt(accelerometer_norm * accelerometer_norm - verticalAcceleration * verticalAcceleration);

        if (Float.isNaN(verticalAcceleration)) {
            verticalAcceleration = 0;
        }

        if (Float.isNaN(horizontalAcceleration)) {
            horizontalAcceleration = 0;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

//        if (mGravity != null && mGeomagnetic != null) {
//            Rot = new float[9];
//            float I[] = new float[9];
//            boolean success = SensorManager.getRotationMatrix(Rot, I, mGravity, mGeomagnetic);
//            if (success) {
//                //Log.e(TAG, "Matrix_R: " + Rot);
//            }
//        }

        if ((currentTime - lastUpdate) >= 10) {

            lastUpdate = currentTime;

            generalAccelerationAlongX.add(accelerometer_x);
            generalAccelerationAlongY.add(accelerometer_y);
            generalAccelerationAlongZ.add(accelerometer_z);

            gravityX.add(gravity_x);
            gravityY.add(gravity_y);
            gravityZ.add(gravity_z);

            gyroscopeX.add(gyroscope_x);
            gyroscopeY.add(gyroscope_y);
            gyroscopeZ.add(gyroscope_z);

            accelerometerData.add(verticalAcceleration);
            horizontalAccelerationData.add(horizontalAcceleration);
            timeData.add(currentTime - startTime);

            //Log.e(TAG, "R_matrix: " + Rot);
            //rotationMatrixData.add(Rot);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }

    private void onStopButton() {
        if (isDataRecording) {
            mSensorManager.unregisterListener(this);
            isDataRecording = false;

            startButton.setText("Blocked");
            startButton.setEnabled(false);

            Toast.makeText(getApplicationContext(), "Stopped!", Toast.LENGTH_SHORT).show();

            int startOfJumpIndex = 0, endOfJumpIndex = 0;

            float min = accelerometerData.get(0), max = accelerometerData.get(0), maxIntegration = accelerometerData.get(0);
            int min_index = 0, max_index = 0, maxIntegrationIndex = 0;

            for (int i = 0; i < accelerometerData.size(); i++) {
                if (accelerometerData.get(i) > max) {
                    max = accelerometerData.get(i);
                    max_index = i;
                }
            }

            long maxTime = timeData.get(max_index);
            int trimStartIndex = 0, trimEndIndex = 0;

            for (int i = max_index; i >= 0; i--) {
                if (maxTime - timeData.get(i) >= TRIM_THRESHOLD) {
                    trimStartIndex = i;
                    break;
                }
            }

            for (int i = max_index; i < accelerometerData.size(); i++) {
                if (timeData.get(i) - maxTime >= TRIM_THRESHOLD) {
                    trimEndIndex = i;
                    break;
                }
            }

            ArrayList<Float> accelerometerDataNew = new ArrayList<>(accelerometerData.subList(trimStartIndex, trimEndIndex));
            ArrayList<Long> timeDataNew = new ArrayList<>(timeData.subList(trimStartIndex, trimEndIndex));

//        for (int i = 0; i < max_index - 10; i++) {
//            if (accelerometerData.get(i) > min) {
//                min = accelerometerData.get(i);
//                min_index = i;
//            }
//        }

            // Get an array of peaks
            ArrayList<Integer> peaks = new ArrayList<Integer>();
            peaks = detectPeaks(accelerometerDataNew, PEAK_THRESHOLD);

            // Find a peak with maximum cumulative sum from the left
            float maxValueLeft = calculateCumulativeSumFromLeft(accelerometerDataNew, peaks.get(0));
            int maxCumulativeSumIndexLeft = peaks.get(0);
            for (int i : peaks) {
                if (maxValueLeft < calculateCumulativeSumFromLeft(accelerometerDataNew, i)) {
                    maxValueLeft = calculateCumulativeSumFromLeft(accelerometerDataNew, i);
                    maxCumulativeSumIndexLeft = i;
                }
            }


            // Find a start of jump index by looking for a maximum in the subarray of accelerometerData ([:maxCumulativeSumIndexLeft + 1])
            float helpMax = accelerometerDataNew.get(0);
            int helpMaxIndex = 0;
            for (int i = 0; i < maxCumulativeSumIndexLeft + 1; i++) {
                if (accelerometerDataNew.get(i) > helpMax) {
                    helpMax = accelerometerDataNew.get(i);
                    helpMaxIndex = i;
                }
            }
            startOfJumpIndex = helpMaxIndex;


            // Find a max point (which is the endOfJumpIndex) before the cumulative point from the left
            float helpMaxEnd = accelerometerDataNew.get(helpMaxIndex + 1);
            int helpMaxIndexEnd = helpMaxIndex + 1;
            for (int i = helpMaxIndex + 1; i < accelerometerDataNew.size(); i++) {
                if (helpMaxEnd < accelerometerDataNew.get(i)) {
                    helpMaxEnd = accelerometerDataNew.get(i);
                    helpMaxIndexEnd = i;
                }
            }
            endOfJumpIndex = helpMaxIndexEnd;


//            // Find a peak with maximum cumulative sum from the right
//            float maxValueRight = calculateCumulativeSumFromRight(accelerometerData, peaks.get(0));
//            int maxCumulativeSumIndexRight = peaks.get(0);
//            for (int i : peaks) {
//                if (maxValueRight < calculateCumulativeSumFromRight(accelerometerData, i)) {
//                    maxValueRight = calculateCumulativeSumFromRight(accelerometerData, i);
//                    maxCumulativeSumIndexRight = i;
//                }
//            }
//
//            // Find a start of jump index by looking for a maximum in the subarray of accelerometerData ([maxCumulativeSumIndexRight:])
//            helpMax = accelerometerData.get(accelerometerData.size() - 1);
//            helpMaxIndex = accelerometerData.size() - 1;
//            for (int i = accelerometerData.size() - 1; i >= maxCumulativeSumIndexRight; i--) {
//                if (accelerometerData.get(i) > helpMax) {
//                    helpMax = accelerometerData.get(i);
//                    helpMaxIndex = i;
//                }
//            }
//            endOfJumpIndex = helpMaxIndex;

            startOfJump = timeDataNew.get(startOfJumpIndex);
            endOfJump = timeDataNew.get(endOfJumpIndex);

            // sending target time (timeTosend) to Server.
            tTarget = (startOfJump + endOfJump) / 2;

            sendMSD();
        }

    }



    private void sendMSD() {

        String targetTimeString = Long.toString(tTarget);
        String accDataStringList = StringUtils.join(accelerometerData, ",");
        String horizontalAccDataStringList = StringUtils.join(horizontalAccelerationData, ",");

        String generalAccelerationAlongXList = StringUtils.join(generalAccelerationAlongX, ", ");
        String generalAccelerationAlongYList = StringUtils.join(generalAccelerationAlongY, ", ");
        String generalAccelerationAlongZList = StringUtils.join(generalAccelerationAlongZ, ", ");

        String gravityXList = StringUtils.join(gravityX, ", ");
        String gravityYList = StringUtils.join(gravityY, ", ");
        String gravityZList = StringUtils.join(gravityZ, ", ");

        String gyroscopeXList = StringUtils.join(gyroscopeX, ", ");
        String gyroscopeYList = StringUtils.join(gyroscopeY, ", ");
        String gyroscopeZList = StringUtils.join(gyroscopeZ, ", ");

        String timeDataStringList = StringUtils.join(timeData, ",");

        String jumpStartString = Long.toString(startOfJump);

        String jumpEndString = Long.toString(endOfJump);

        Log.e(TAG, "target Time: "+ targetTimeString);
        Log.e(TAG, "Size of accData: "+ accelerometerData.size());
        Log.e(TAG, "Size of timeData: "+ timeData.size());

        String delimiter = ":";

        String msd = targetTimeString + delimiter + accDataStringList + delimiter + horizontalAccDataStringList
                + delimiter + generalAccelerationAlongXList + delimiter + generalAccelerationAlongYList + delimiter
                + generalAccelerationAlongZList + delimiter + gravityXList + delimiter + gravityYList + delimiter
                + gravityZList + delimiter + gyroscopeXList + delimiter + gyroscopeYList + delimiter
                + gyroscopeZList + delimiter + timeDataStringList + delimiter + jumpStartString + delimiter + jumpEndString + delimiter +wearID+"#";

        Log.e(TAG, "msd string length: "+msd.length());

        String datapath = "/my_path";
        new SendMessage(datapath, msd).start();

        Log.e(TAG, "MSD has been sent!");
        Log.d("alpha57","Send the message");
    }


    private ArrayList<Integer> detectPeaks(ArrayList<Float> data, Float threshold) {
        ArrayList<Integer> indices = new ArrayList<Integer>();

        for (Float d : data) {
            if (threshold >= 0) {
                if (d >= threshold) {
                    indices.add(data.indexOf(d));
                }
            } else {
                if (d <= threshold) {
                    indices.add(data.indexOf(d));
                }
            }
        }

        return indices;
    }

    private float calculateCumulativeSumFromLeft(ArrayList<Float> data, int ind) {

        float sum = 0;

        for (int i = 0; i <= ind; i++) {
            sum += data.get(i);
        }

        return sum;
    }

    private float calculateCumulativeSumFromRight(ArrayList<Float> data, int ind) {

        float sum = 0;

        for (int i = data.size() - 1; i >= ind; i--) {
            sum += data.get(i);
        }

        return sum;
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //New message is received
            String message = intent.getStringExtra("message");
            long endTime = System.currentTimeMillis();
            Log.d("PingTest",String.valueOf(endTime - pingSent));


            if (wearID==""){
                wearID = message.split("_")[2];
            }else{
                if (wearID!=message.split("_")[2]){
                    return;
                }
            }


            totalPings++;
            if (totalPings<20){
                sendPing();
            }else{
                startButton.setText("Stop");
                onStartButton();
            }
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

        //Constructor for sending Sensor Data to the Data Layer
        SendMessage(String p, String m) {
            path = p;
            message = m;
        }

        public void run() {
            //Retrieve the connected devices
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                //Block on a task and get the result synchronously
                List<Node> nodes = Tasks.await(nodeListTask);
                for (Node node : nodes) {
                    //Send the message
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(DataActivity.this).sendMessage(node.getId(), path, message.getBytes());
                    Tasks.await(sendMessageTask);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}