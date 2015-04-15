package mi12.androidapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import java.util.Date;

/**
 * Created by quentin on 08/04/15.
 */
public class MyTime implements Runnable {
    private static final String TAG = Activity.class.getSimpleName();
    private TextView view, viewText;
    private Activity activity;
    private Thread thread, threadUpdateNtp;
    private SntpClient ntp_time;
    private String ntp_host = "fr.pool.ntp.org";
    private long reference_time_millis_offset = 0;

    private class UpdateTimeOnScreen implements Runnable {
        @Override
        public void run() {
            view.setText(Long.toString(getTimeMillis()));
            try {
                if (reference_time_millis_offset != 0) {
                    view.setTextColor(Color.rgb(0,150,0));
                    viewText.setTextColor(Color.rgb(0,150,0));
                    viewText.setText(new Date(getTimeMillis()).toString());
                } else {
                    view.setTextColor(Color.RED);
                    viewText.setTextColor(Color.RED);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            };
        }
    }

    private class GetNtpTime implements Runnable {
        @Override
        public void run() {
            if (ntp_time.requestTime(ntp_host, 1000)) {
                reference_time_millis_offset = ntp_time.getNtpTime() - ntp_time.getNtpTimeReference();
                Log.d(TAG, "NTP time reference : " + reference_time_millis_offset);
            }
            else {
                reference_time_millis_offset = 0;
                Log.e(TAG, "Failed to request NTP time");
            }
        }
    }

    MyTime(Activity _act, TextView vtime, TextView vtext) {
        activity = _act;
        view = vtime;
        viewText = vtext;
        view.setTextColor(Color.RED);
        viewText.setTextColor(Color.RED);
        ntp_time = new SntpClient();
        thread = new Thread(this);
        thread.start();
    }

    public boolean updateTime(String host) {
        if (threadUpdateNtp == null || !threadUpdateNtp.isAlive()) {
            ntp_host = host;
            threadUpdateNtp = new Thread(new GetNtpTime());
            threadUpdateNtp.start();
        }
        while (threadUpdateNtp.isAlive()) { SystemClock.sleep(10); }
        return (reference_time_millis_offset != 0);
    }

    public long getTimeMillis() {
        return SystemClock.elapsedRealtime() + reference_time_millis_offset;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            SystemClock.sleep(100);
            activity.runOnUiThread(new UpdateTimeOnScreen());
        }
    }
}