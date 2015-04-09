package quentin.MI12;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

/**
 * Created by quentin on 11/03/15.
 */
public class SensorData {
    private final long timeMillis;
    private final float[] data;
    private final Sensor sensor;
    SensorData(SensorEvent event, long timeMillis) {
        this.sensor = event.sensor;
        this.data = event.values;
        this.timeMillis = timeMillis;
    }

    public String getCSV() {
        String str = Long.toString(timeMillis);
        switch (sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                str += ",ORI";
                break;
            case Sensor.TYPE_ACCELEROMETER:
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
