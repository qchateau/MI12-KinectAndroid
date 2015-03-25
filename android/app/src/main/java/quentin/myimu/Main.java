package quentin.myimu;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URI;

public class Main extends Activity {
    private static final String TAG = Activity.class.getSimpleName();
    private EditText editTextURI;
    private Communication communication;
    private MySensors sensors;

    private class MySensors implements SensorEventListener {
        private TextView textViewAx, textViewAy, textViewAz, textViewOx, textViewOy, textViewOz;
        private SensorManager mSensorManager;
        private Sensor Accelerometer, Orientation;

        MySensors() {
            textViewAx = (TextView) findViewById(R.id.textViewAx);
            textViewAy = (TextView) findViewById(R.id.textViewAy);
            textViewAz = (TextView) findViewById(R.id.textViewAz);
            textViewOx = (TextView) findViewById(R.id.textViewOx);
            textViewOy = (TextView) findViewById(R.id.textViewOy);
            textViewOz = (TextView) findViewById(R.id.textViewOz);

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

            TextView accelSampleRate = (TextView) findViewById(R.id.textViewAccelSampleRateValue);
            accelSampleRate.setText(Integer.toString(1000000/(Accelerometer.getMinDelay()))+" Hz");

            TextView oriSampleRate = (TextView) findViewById(R.id.textViewOriSampleRateValue);
            oriSampleRate.setText(Integer.toString(1000000/(Orientation.getMinDelay()))+" Hz");
        }

        @Override
        public final void onSensorChanged(SensorEvent event) {
            if (event.sensor == Accelerometer) {
                textViewAx.setText(String.format("%.4f", event.values[0]));
                textViewAy.setText(String.format("%.4f", event.values[1]));
                textViewAz.setText(String.format("%.4f", event.values[2]));
            }
            else if (event.sensor == Orientation) {
                textViewOx.setText(String.format("%.4f", event.values[0]));
                textViewOy.setText(String.format("%.4f", event.values[1]));
                textViewOz.setText(String.format("%.4f", event.values[2]));
            }
            if (communication != null && communication.isConnected()){
                communication.writeToSocket(new SensorData(event).getCSV());
            }
        }

        @Override
        public final void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something here if sensor accuracy changes.
        }

        protected void onResume() {
            mSensorManager.registerListener(this, Accelerometer, Accelerometer.getMinDelay());
            mSensorManager.registerListener(this, Orientation, Orientation.getMinDelay());
        }

        protected void onPause() {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensors = new MySensors();
        editTextURI = (EditText) findViewById(R.id.editTextURI);
    }

    @Override
    protected void onStop() {
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
        URI uri;
        try {
            uri = new URI("my://" + editTextURI.getText());
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String host = uri.getHost();
        int port = uri.getPort();
        Log.d(TAG, "Host:"+host+" Port:"+Integer.toString(port));
        communication = Communication.getInstance(host, port);
    }
}