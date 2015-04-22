package mi12.androidapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

public class Main extends Activity implements Observer {
    private static final String TAG = Activity.class.getSimpleName();
    private EditText editTextIP, editTextPort;
    private Communication communication;
    private MySensors sensors;
    private MyTime time;

    public Communication getCommunication() {
        return communication;
    }

    public MyTime getTime() {
        return time;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensors = new MySensors(this);
        time = new MyTime(this, (TextView)findViewById(R.id.textTimeValue), (TextView)findViewById(R.id.textTimeString));
        editTextIP = (EditText) findViewById(R.id.editTextIP);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
    }

    @Override
    protected void onStop() {
        if (communication != null) communication.Disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensors.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensors.onPause();
    }

    public void onConnect(View v) {
        if (communication != null && communication.isConnected()) {
            communication.Disconnect();
        } else {
            String host = editTextIP.getText().toString();
            int port = Integer.parseInt(editTextPort.getText().toString());
            Log.d(TAG, "Host:" + host + " Port:" + Integer.toString(port));
            if (!time.updateTime(host)) {
                communication = Communication.getInstance(host, port);
                communication.addObserver(this);
                communication.Connect();
            }
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof CommunicationState) {
            CommunicationState com_state = (CommunicationState)data;
            com_state.setTextView((TextView) findViewById(R.id.comState));
            runOnUiThread(com_state); // updates connect button
        }
    }
}