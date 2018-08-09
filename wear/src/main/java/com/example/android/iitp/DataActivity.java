package com.example.android.iitp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class DataActivity extends Activity implements Serializable, SensorEventListener{

    public static final float PEAK_THRESHOLD = 10;
    public static final long TRIM_THRESHOLD = 1500;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final String TAG = "message";

    private Button startButton;
    private Button stopButton;
    private BluetoothAdapter mBluetoothAdapter;
    //    private BluetoothConnectionService mBluetoothConnectionService;
    private BluetoothDevice mBluetoothDevice;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private boolean isScreenOn = true;


    private boolean isTargetTimeSent = false;
    private boolean isAccDataSent = false;
    private boolean isTimeDataSent = false;

    private static SensorManager mSensorManager;
    private static SensorEventListener sensorEventListener;

    public static long dataStartTimeInMillis;
    public static long dataStopTimeInMillis;
    public static long timeToSend;
    public static String ipAddress;

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

    private static File GeneralData;
    private static File VerticalAccelerationData;
    private static File HorizontalAccelerationData;
    private static File rotationMatrixFile;

    private static long lastUpdate = System.currentTimeMillis();
    private boolean isDataRecording = false;

    private long tTarget;

    private long startTime;

    ArrayList<float[]> rotationMatrixData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        startButton = (Button) findViewById(R.id.startButton);
//        stopButton = (Button) findViewById(R.id.stopButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDataRecording) {
                    startButton.setText("Stop");
//                    startButton.setBackgroundColor(Color.RED);
//                    startButton.setTextColor(Color.BLACK);
                    onStartButton();
                } else {
                    startButton.setText("Start");
//                    float[] colorList = {0x3F, 0x51, 0xB5};
//                    startButton.setBackgroundColor(Color.HSVToColor(colorList));
                    onStopButton();
                }
            }
        });
//        stopButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onStopButton();
//            }
//        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        mBluetoothAdapter.enable();
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
//        if (mBluetoothAdapter != null) {
//            if (mBluetoothAdapter.isEnabled()) {
//                Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
//
//                if (bondedDevices.size() > 0) {
//                    Object[] devices = (Object[]) bondedDevices.toArray();
//                    mBluetoothDevice = (BluetoothDevice) devices[0];
//                }
//            }
//        }
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mBluetoothConnectionService = new BluetoothConnectionService(getApplicationContext());
//                mBluetoothConnectionService.startClient(mBluetoothDevice, MY_UUID_INSECURE);
//            }
//        }, 1000);
//        mBluetoothConnectionService = new BluetoothConnectionService(this);
//        mBluetoothConnectionService.startClient(mBluetoothDevice, MY_UUID_INSECURE);
//        String message = "Started!";
//        mBluetoothConnectionService.write(message.getBytes(Charset.defaultCharset()));

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

        rotationMatrixData = new ArrayList<>();

        // -----------------------------------------------------------------
        File path = getApplicationContext().getExternalFilesDir(null);
        // -----------------------------------------------------------------
        File ipAddressFile = new File(path, "ipAddress.txt");

        try {
            FileReader fr = new FileReader(ipAddressFile);
            BufferedReader br = new BufferedReader(fr);
            try {
                ipAddress = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String root = Environment.getExternalStorageDirectory().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());
        File myDir = new File(root + "/MSD/" + currentDateAndTime);
        myDir.mkdirs();

        // -----------------------------------------------------------------
        GeneralData = new File(path, "GeneralData.txt");
        VerticalAccelerationData = new File(path, "VerticalAccelerationData.txt");
        HorizontalAccelerationData = new File(path, "HorizontalAccelerationData.txt");
        rotationMatrixFile = new File(myDir, "rotationMatrixFile.txt");
        // -----------------------------------------------------------------
    }

    private void onStartButton() {
        if (!isDataRecording) {
            isDataRecording = true;
//            String message = "Start Button Pressed!";
//            mBluetoothConnectionService.write(message.getBytes(Charset.defaultCharset()));
//            new TimeDataStart(this).execute();


            // sending ping message for data recording start from Client to Server phone.
            String message = "Data Start Ping!";
            byte[] send = message.getBytes();
            //ClientConnectionActivity.mClientChatService.write(send);

            Toast.makeText(getApplicationContext(), "Data recording started!", Toast.LENGTH_SHORT).show();

            startTime = System.currentTimeMillis();

//            sensorEventListener = new SensorEventListener() {
//                float accelerometer_x, accelerometer_y, accelerometer_z;
//                float gravity_x, gravity_y, gravity_z;
//                float gyroscope_x, gyroscope_y, gyroscope_z;
//                float verticalAcceleration;
//
//                float[] mGravity;
//                float[] mGeomagnetic;
//                float[] R;
//                @Override
//                public void onSensorChanged(SensorEvent event) {
//                    Log.d("wearDebug","Aitosha");
//
//                    long currentTime = System.currentTimeMillis();
//
//                    if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//                        accelerometer_x = event.values[0];
//                        accelerometer_y = event.values[1];
//                        accelerometer_z = event.values[2];
//                    }
//
//                    if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
//                        gravity_x = -event.values[0];
//                        gravity_y = -event.values[1];
//                        gravity_z = -event.values[2];
//                    }
//
//                    if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//                        gyroscope_x = event.values[0];
//                        gyroscope_y = event.values[1];
//                        gyroscope_z = event.values[2];
//                    }
//
//                    if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
//
//                        if (event.values[0] == 0 && isScreenOn == true) {
////                            turnOffScreen();
//                        }
//
//                        if (event.values[0] != 0 && isScreenOn == false) {
////                            turnOnScreen();
//                        }
//                    }
//
//                    float accelerometer_norm = (float) Math.sqrt(accelerometer_x * accelerometer_x + accelerometer_y * accelerometer_y + accelerometer_z * accelerometer_z);
//                    float gravity_norm = (float) Math.sqrt(gravity_x * gravity_x + gravity_y * gravity_y + gravity_z * gravity_z);
//                    float cosine = (accelerometer_x * gravity_x + accelerometer_y * gravity_y + accelerometer_z * gravity_z) / (gravity_norm * accelerometer_norm);
//                    verticalAcceleration = accelerometer_norm * (-cosine);
//                    float horizontalAcceleration = (float) Math.sqrt(accelerometer_norm * accelerometer_norm - verticalAcceleration * verticalAcceleration);
//
//
//                    if (Float.isNaN(verticalAcceleration)) {
//                        verticalAcceleration = 0;
//                    }
//
//                    if (Float.isNaN(horizontalAcceleration)) {
//                        horizontalAcceleration = 0;
//                    }
//
//                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//                        mGravity = event.values;
//                    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//                        mGeomagnetic = event.values;
//                    if (mGravity != null && mGeomagnetic != null) {
//                        R = new float[9];
//                        float I[] = new float[9];
//                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
//                        if (success) {
//                            Log.e(TAG, "Matrix_R: " + R);
//                        }
//                    }
//
//                    if ((currentTime - lastUpdate) >= 10) {
//
//                        lastUpdate = currentTime;
//
//                        generalAccelerationAlongX.add(accelerometer_x);
//                        generalAccelerationAlongY.add(accelerometer_y);
//                        generalAccelerationAlongZ.add(accelerometer_z);
//
//                        gravityX.add(gravity_x);
//                        gravityY.add(gravity_y);
//                        gravityZ.add(gravity_z);
//
//                        gyroscopeX.add(gyroscope_x);
//                        gyroscopeY.add(gyroscope_y);
//                        gyroscopeZ.add(gyroscope_z);
//
//                        accelerometerData.add(verticalAcceleration);
//                        horizontalAccelerationData.add(horizontalAcceleration);
//                        timeData.add(currentTime - startTime);
//
//                        Log.e(TAG, "R_matrix: " + R);
//                        rotationMatrixData.add(R);
//
//                        Log.d(TAG, "Time during accumulation: " + (currentTime - startTime));
//
////                        try {
////                            BufferedWriter out = new BufferedWriter(new FileWriter(VerticalAccelerationData, true), 1024);
////                            String entry = verticalAcceleration + " " + (currentTime - startTime) + "\n";
////                            out.write(entry);
////                            out.close();
////                        } catch (IOException e) {
////                            e.printStackTrace();
////                        }
//                    }
//
////                    accelerometerData.add(verticalAcceleration);
////                    timeData.add(currentTime - startTime);
//                }
//
//                @Override
//                public void onAccuracyChanged(Sensor sensor, int accuracy) {
//                }
//            };
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

        }
    }

    public void onSensorChanged(SensorEvent event) {
        float accelerometer_x = 0;
        float accelerometer_y = 0;
        float accelerometer_z = 0;
        float gravity_x = 0;
        float gravity_y = 0;
        float gravity_z = 0;
        float gyroscope_x = 0;
        float gyroscope_y = 0;
        float gyroscope_z = 0;
        float verticalAcceleration = 0;

        float[] mGravity = {0f};
        float[] mGeomagnetic = {0f};
        float[] R = {0f};

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

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {

            if (event.values[0] == 0 && isScreenOn == true) {
//                            turnOffScreen();
            }

            if (event.values[0] != 0 && isScreenOn == false) {
//                            turnOnScreen();
            }
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
        if (mGravity != null && mGeomagnetic != null) {
            R = new float[9];
            float I[] = new float[9];
//            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
//            if (success) {
//                Log.e(TAG, "Matrix_R: " + R);
//            }
        }

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

            Log.e(TAG, "R_matrix: " + R);
            rotationMatrixData.add(R);

            //Log.d(TAG, "Time during accumulation: " + (currentTime - startTime));

//                        try {
//                            BufferedWriter out = new BufferedWriter(new FileWriter(VerticalAccelerationData, true), 1024);
//                            String entry = verticalAcceleration + " " + (currentTime - startTime) + "\n";
//                            out.write(entry);
//                            out.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
        }

//                    accelerometerData.add(verticalAcceleration);
//                    timeData.add(currentTime - startTime);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }

    private void onStopButton() {
        if (isDataRecording) {
            isDataRecording = false;

            startButton.setText("Blocked");
            startButton.setEnabled(false);

            mSensorManager.unregisterListener(sensorEventListener);
//            new TimeDataStop(this).execute();
            Toast.makeText(getApplicationContext(), "Stopped!", Toast.LENGTH_SHORT).show();

            // -----------------------------------------------------------------
//            for (int i = 0; i < accelerometerData.size(); i++) {
//                try {
//                    BufferedWriter out = new BufferedWriter(new FileWriter(VerticalAccelerationData, true), 1024);
//                    String entry = accelerometerData.get(i) + " " + timeData.get(i) + "\n";
//                    Log.d(TAG, "Time after accumulation: " + timeData.get(i));
//                    out.write(entry);
//                    out.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            for (int i = 0; i < horizontalAccelerationData.size(); i++) {
//                try {
//                    BufferedWriter out = new BufferedWriter(new FileWriter(HorizontalAccelerationData, true), 1024);
//                    String entry = horizontalAccelerationData.get(i) + " " + timeData.get(i) + "\n";
//                    Log.d(TAG, "Time after accumulation: " + timeData.get(i));
//                    out.write(entry);
//                    out.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            for (int i = 0; i < rotationMatrixData.size(); i++) {
//                try {
//                    BufferedWriter out = new BufferedWriter(new FileWriter(rotationMatrixFile, true), 1024);
//                    String entry = "";
//                    if (rotationMatrixData.get(i) == null) {
//                        continue;
//                    }
//                    for (int j = 0; j < rotationMatrixData.get(i).length; j++) {
//                        entry += rotationMatrixData.get(i)[j] + " ";
//                    }
//                    entry += timeData.get(i) + "\n";
//                    out.write(entry);
//                    out.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

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

//
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

        String rotationMatrixList = StringUtils.join(rotationMatrixData, ", ");

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
                + gyroscopeZList + delimiter + rotationMatrixList + delimiter + timeDataStringList + delimiter + jumpStartString + delimiter + jumpEndString + "#";

        Log.e(TAG, "msd string length: "+msd.length());

        byte[] sendMSD = msd.getBytes();
        //ClientConnectionActivity.mClientChatService.write(sendMSD);

        Log.e(TAG, "MSD has been sent!");

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

    public void turnOnScreen(){
        // turn on screen
        Log.v("ProximityActivity", "ON!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
        mWakeLock.acquire();
        isScreenOn = true;
    }

    public void turnOffScreen(){
        // turn off screen
        Log.v("ProximityActivity", "OFF!");
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
        mWakeLock.acquire();
        isScreenOn = false;
    }




}
