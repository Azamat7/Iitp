package com.example.android.iitp;

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

public class MainActivity extends AppCompatActivity {

    private Button videoButton;
    private Button ipButton;
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void onVideoButton() {

        Intent serverConnectionIntent = new Intent(this, ServerConnectionActivity.class);
        startActivity(serverConnectionIntent);

    }

    private void onIpButton() {

    }
}
