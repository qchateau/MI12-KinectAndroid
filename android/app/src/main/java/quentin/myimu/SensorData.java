package quentin.myimu;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import static android.os.SystemClock.elapsedRealtimeNanos;

/**
 * Created by quentin on 11/03/15.
 */
public class SensorData {
    private final long time;
    private final float[] data;
    private final Sensor sensor;
    SensorData(SensorEvent event) {
        this.sensor = event.sensor;
        this.data = event.values;
        this.time = elapsedRealtimeNanos();
    }

    public String getCSV() {
        String str = Long.toString(time/1000);
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
