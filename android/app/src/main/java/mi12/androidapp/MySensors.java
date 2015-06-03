package mi12.androidapp;

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
    private float [] acc_vals, ori_vals, corrected_acc_vals;

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

        acc_vals = new float[3];
        corrected_acc_vals = new float[3];
        ori_vals = new float[3];

        mSensorManager = (SensorManager) main.getSystemService(Context.SENSOR_SERVICE);
        Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

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
        float[] vals;
        if (event.sensor == Accelerometer) {
            acc_vals = event.values;
            float[] rotMat = new float[9];
            MyTime time = main.getTime();
            SensorManager.getRotationMatrixFromVector(rotMat, ori_vals);
            corrected_acc_vals[0] = rotMat[0] * acc_vals[0] + rotMat[1] * acc_vals[1] + rotMat[2] * acc_vals[2];
            corrected_acc_vals[1] = rotMat[3] * acc_vals[0] + rotMat[4] * acc_vals[1] + rotMat[5] * acc_vals[2];
            corrected_acc_vals[2] = rotMat[6] * acc_vals[0] + rotMat[7] * acc_vals[1] + rotMat[8] * acc_vals[2];
            vals = corrected_acc_vals;

            Communication communication = main.getCommunication();
            if (communication != null && communication.isConnected()){
                communication.writeToSocket(new SensorData(vals, event.sensor, time.getTimeMillis()).getCSV());
            }

            textViewAx.setText(String.format("%.4f", vals[0]));
            textViewAy.setText(String.format("%.4f", vals[1]));
            textViewAz.setText(String.format("%.4f", vals[2]));
        }
        else if (event.sensor == Orientation) {
            ori_vals = event.values;
            vals = ori_vals;
            textViewOx.setText(String.format("%.4f", vals[0]));
            textViewOy.setText(String.format("%.4f", vals[1]));
            textViewOz.setText(String.format("%.4f", vals[2]));
        }
        else {
            return;
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