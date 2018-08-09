package com.example.android.iitp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String deviceUniqueID = UUID.randomUUID().toString();

    public static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private Button videoButton;
    private Button ipButton;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    public static Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        videoButton = (Button) findViewById(R.id.videoButton);
        ipButton = (Button) findViewById(R.id.ipButton);

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onVideoButton();
            }
        });

        ipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onIpButton();
            }
        });

        File path = getApplicationContext().getExternalFilesDir(null);
        File ipAddress = new File(path, "ipAddress.txt");

        if (!ipAddress.exists()) {
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(ipAddress, true), 1024);
                out.write("");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        mBluetoothAdapter.enable();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        mBluetoothAdapter.enable();
    }

    private void onVideoButton() {

        Intent serverConnectionIntent = new Intent(this, ServerConnectionActivity.class);
        startActivity(serverConnectionIntent);

    }

    private void onIpButton() {

    }
}
