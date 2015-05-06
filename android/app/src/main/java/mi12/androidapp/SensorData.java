package mi12.androidapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Created by quentin on 11/03/15.
 */
public class SensorData {
    private final long timeMillis;
    private float[] data;
    private final Sensor sensor;
    SensorData(float[] data, Sensor sensor, long timeMillis) {
        this.sensor = sensor;
        this.data = data;
        this.timeMillis = timeMillis;
    }

    public String getCSV() {
        String str = Long.toString(timeMillis);
        switch (sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                str += ",ORI";
                break;
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                str += ",ACCEL";
                break;
            default:
                str += ",UNKNOWN";
                break;
        }
        for (float d : data) {
            str += ","+Float.toString(d);
        }
        str += "\n";
        return str;
    }
}
