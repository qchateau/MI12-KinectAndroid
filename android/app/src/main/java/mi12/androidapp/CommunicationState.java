package mi12.androidapp;

import android.graphics.Color;
import android.widget.TextView;

/**
 * Created by quentin on 08/04/15.
 */

public class CommunicationState implements Runnable {
    public boolean isConnected;

    private TextView view;

    CommunicationState(boolean connected) {
        isConnected = connected;
    }

    public void setTextView(TextView v) {
        this.view = v;
    }

    @Override
    public void run() {
        if (isConnected) {
            view.setText("Connected");
            view.setTextColor(Color.rgb(0,150,0));
        }
        else {
            view.setText("Disconnected");
            view.setTextColor(Color.RED);
        }
    }
}