package quentin.myimu;

import static android.os.SystemClock.elapsedRealtimeNanos;

import android.app.Activity;
import android.os.SystemClock;
import android.widget.TextView;

/**
 * Created by quentin on 08/04/15.
 */
public class MyTime implements Runnable {
    private TextView view;
    private Activity activity;
    private Thread thread;

    private class UpdateTime implements Runnable {
        @Override
        public void run() {
            view.setText(Long.toString(getTimeMillis()));
        }
    }

    MyTime(Activity _act, TextView v) {
        activity = _act;
        this.view = v;
        thread = new Thread(this);
        thread.start();
    }

    public long getTimeMillis() {
        return elapsedRealtimeNanos() / 1000000;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            SystemClock.sleep(100);
            activity.runOnUiThread(new UpdateTime());
        }
    }
}