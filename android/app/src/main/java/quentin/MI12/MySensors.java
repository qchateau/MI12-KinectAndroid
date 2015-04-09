package quentin.MI12;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

/**
 * Created by quentin on 08/04/15.
 */
public class MySensors implements SensorEventListener {
    private TextView textViewAx, textViewAy, textViewAz, textViewOx, textViewOy, textViewOz;
    private SensorManager mSensorManager;
    private Sensor Accelerometer, Orientation;
    private Main main;

    MySensors(Main _main) {
        main = _main;
        textViewAx = (TextView) main.findViewById(R.id.textViewAx);
        textViewAy = (TextView) main.findViewById(R.id.textViewAy);
        textViewAz = (TextView) main.findViewById(R.id.textViewAz);
        textViewOx = (TextView) main.findViewById(R.id.textViewOx);
        textViewOy = (TextView) main.findViewById(R.id.textViewOy);
        textViewOz = (TextView) main.findViewById(R.id.textViewOz);
        TextView accelSampleRate = (TextView) main.findViewById(R.id.textViewAccelSampleRateValue);
        TextView oriSampleRate = (TextView) main.findViewById(R.id.textViewOriSampleRateValue);

        mSensorManager = (SensorManager) main.getSystemService(Context.SENSOR_SERVICE);
        Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        try {
            accelSampleRate.setText(Integer.toString(1000000 / (Accelerometer.getMinDelay())) + " Hz");
        }
        catch (ArithmeticException e) {
            accelSampleRate.setText("0 Hz");
        }
        try {
            oriSampleRate.setText(Integer.toString(1000000 / (Orientation.getMinDelay())) + " Hz");
        }
        catch (ArithmeticException e) {
            oriSampleRate.setText("0 Hz");
        }
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
        Communication communication = main.getCommunication();
        MyTime time = main.getTime();
        if (communication != null && communication.isConnected()){
            communication.writeToSocket(new SensorData(event, time.getTimeMillis()).getCSV());
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