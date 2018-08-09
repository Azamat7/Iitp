package com.example.android.iitp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * This activity will be invoked when Camera mode button on the Main activity is pressed.
 * This Activity is used to (1) establish BT server to connect multiple jump devices (clients).
 * In this activity there will be a SPINNER populated with number of devices (jumpers) to connect.
 */


public class ServerConnectionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private Spinner spinner;
    private static final String TAG = "ServConnAct";
    private int nClients;
    protected Handler myHandler;

    // Debugging
    private static final boolean D = true;

    public static ServerDataModel mServerDataModel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D) Log.e(TAG, "++ ON CREATE ++");
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




        //Register to receive local broadcasts, which we'll be creating in the next step//

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
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





//Define a nested class that extends BroadcastReceiver//

    public class Receiver extends BroadcastReceiver {
        @Override

        public void onReceive(Context context, Intent intent) {

//Upon receiving each message from the wearable, display the following text//

            String message = intent.getStringExtra("message");
            mServerDataModel = new ServerDataModel(message);

        }
    }

    public void talkClick(View v) {
//Sending a message can block the main UI thread, so use a new thread//
        String message = "YAY";
        new NewThread("/my_path", message).start();

    }

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
                    Task<Integer> sendMessageTask =

//Send the message//

                            Wearable.getMessageClient(ServerConnectionActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {

//Block on a task and get the result synchronously//

                        Integer result = Tasks.await(sendMessageTask);

                        //if the Task fails, then…..//
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}