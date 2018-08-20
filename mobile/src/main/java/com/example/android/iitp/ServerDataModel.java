package com.example.android.iitp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerDataModel {

    private long dataStartTimeInMillis;
    private long timeToSend;

    private String mIncomingData;

    private boolean isMSDReceived = false;
    private boolean isPingReceived = false;

    private ArrayList<String> generalAccDataAlongX;
    private ArrayList<String> generalAccDataAlongY;
    private ArrayList<String> generalAccDataAlongZ;

    private ArrayList<String> gravityX;
    private ArrayList<String> gravityY;
    private ArrayList<String> gravityZ;

    private ArrayList<String> gyroscopeX;
    private ArrayList<String> gyroscopeY;
    private ArrayList<String> gyroscopeZ;

    private ArrayList<String> accDataArrayList;
    private ArrayList<String> horizontalAccDataArrayList;
    private ArrayList<String> timeDataArrayList;
    private long timeJumpStart;
    private long timeJumpEnd;

    public ServerDataModel(String incomingData){
        this.mIncomingData = incomingData;
        convertData();
    }

    public long getDataStartTime() {
        return dataStartTimeInMillis;
    }

    public long getTargetTime() {
        return timeToSend;
    }

    public long getTimeJumpStart() {
        return timeJumpStart;
    }

    public long getTimeJumpEnd() {
        return timeJumpEnd;
    }

    public ArrayList<String> getAccData(){
        return accDataArrayList;
    }

    public ArrayList<String> getHorAccData() {return horizontalAccDataArrayList;}

    public ArrayList<String> getGeneralAccDataAlongX() {return generalAccDataAlongX;}

    public ArrayList<String> getGeneralAccDataAlongY() {return generalAccDataAlongY;}

    public ArrayList<String> getGeneralAccDataAlongZ() {return generalAccDataAlongZ;}

    public ArrayList<String> getGravityX() {return gravityX;}

    public ArrayList<String> getGravityY() {return gravityY;}

    public ArrayList<String> getGravityZ() {return gravityZ;}

    public ArrayList<String> getGyroscopeX() {return gyroscopeX;}

    public ArrayList<String> getGyroscopeY() {return gyroscopeY;}

    public ArrayList<String> getGyroscopeZ() {return gyroscopeZ;}

    public ArrayList<String> getTimeData(){
        return timeDataArrayList;
    }

    public boolean getIsTimeReceived() {
        return isMSDReceived;
    }



    private void convertData() {

        String[] elements = mIncomingData.split(":");

        String strTargetTime = elements[0];
        String strAccData = elements[1];
        String strHorAccData = elements[2];
        String strGeneralAccDataX = elements[3];
        String strGeneralAccDataY = elements[4];
        String strGeneralAccDataZ = elements[5];

        String strGravityX = elements[6];
        String strGravityY = elements[7];
        String strGravityZ = elements[8];

        String strGyroscopeX = elements[9];
        String strGyroscopeY = elements[10];
        String strGyroscopeZ = elements[11];

        String strTimeData = elements[12];
        String strJumpStart = elements[13];
        String strJumpEnd = elements[14];
        strJumpEnd = strJumpEnd.substring(0, strJumpEnd.length() - 1);

        timeToSend = Long.valueOf(strTargetTime);
        timeToSend += dataStartTimeInMillis;

        accDataArrayList = new ArrayList<String>(Arrays.asList(strAccData.split(",")));
        horizontalAccDataArrayList = new ArrayList<String>(Arrays.asList(strHorAccData.split(",")));
        generalAccDataAlongX = new ArrayList<String>(Arrays.asList(strGeneralAccDataX.split(",")));
        generalAccDataAlongY = new ArrayList<String>(Arrays.asList(strGeneralAccDataY.split(",")));
        generalAccDataAlongZ = new ArrayList<String>(Arrays.asList(strGeneralAccDataZ.split(",")));

        gravityX = new ArrayList<String>(Arrays.asList(strGravityX.split(",")));
        gravityY = new ArrayList<String>(Arrays.asList(strGravityY.split(",")));
        gravityZ = new ArrayList<String>(Arrays.asList(strGravityZ.split(",")));

        gyroscopeX = new ArrayList<String>(Arrays.asList(strGyroscopeX.split(",")));
        gyroscopeY = new ArrayList<String>(Arrays.asList(strGyroscopeY.split(",")));
        gyroscopeZ = new ArrayList<String>(Arrays.asList(strGyroscopeZ.split(",")));

        timeDataArrayList = new ArrayList<String>(Arrays.asList(strTimeData.split(",")));

        timeJumpStart = Long.valueOf(strJumpStart);
        timeJumpStart += dataStartTimeInMillis;

        timeJumpEnd = Long.valueOf(strJumpEnd);
        timeJumpEnd += dataStartTimeInMillis;

        Log.e("saveData", "target Time: " + strTargetTime);
        Log.e("saveData: ", "accData size: " + accDataArrayList.size());
        Log.e("saveData: ", "timeData size: " + timeDataArrayList.size());
    }
}
