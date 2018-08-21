package com.example.android.iitp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * This activity will be invoked when Camera mode button on the Main activity is pressed.
 * This Activity is used to (1) establish BT server to connect multiple jump devices (clients).
 * In this activity there will be a SPINNER populated with number of devices (jumpers) to connect.
 */


public class ServerConnectionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private Spinner spinner;
    private int nClients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connection);

        spinner = (Spinner) findViewById(R.id.clients_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.clients_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    // when item from Spinner is selected
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        int n = Integer.parseInt(spinner.getSelectedItem().toString());
        Log.e("onItemSelected: ",""+n);
        nClients = n;

    }

    // When nothing from spinner is selected
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        Toast.makeText(getApplicationContext(), "You shall state how many people jump!", Toast.LENGTH_SHORT).show();
    }

    // When OK button is pressed
    public void okButtonPressed(View view) {
        Intent videoIntent = new Intent(this, VideoHighFPSActivity.class);
        startActivity(videoIntent);
    }
}