package quentin.myimu;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Observable;

/**
 * Created by quentin on 12/03/15.
 */


public class Communication {
    static private Communication com;
    private Socket socket;
    private Thread thread;
    private String host;
    private int port;
    private OutputStream outStream;
    private LinkedList<String> msgFifo = new LinkedList<String>();
    private static final String TAG = Communication.class.getSimpleName();
    private boolean isOpened = false;

    private Communication(String n_host, int n_port) {
        host = n_host;
        port = n_port;
        thread = new Thread(new Runnable() {
            public void run() {
                try {
                    socket = new Socket(host, port);
                    outStream = socket.getOutputStream();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                isOpened = true;
                while (!Thread.currentThread().isInterrupted()) {
                    String str = "";
                    try {
                        synchronized (msgFifo) {
                            str = msgFifo.removeFirst();
                        }
                    }
                    catch (NoSuchElementException e) {
                    }
                    try {
                        outStream.write(str.getBytes());
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Could not send : "+str);
                        e.printStackTrace();
                    }
                }
                try {
                    socket.close();
                }
                catch (IOException e) {}
            }
        });
        thread.start();
    }

    public static Communication getInstance(String n_host, int n_port) {
        if (com != null) {
            com.thread.interrupt();
        }
        com = new Communication(n_host, n_port);
        return com;
    }

    public void writeToSocket(String str) {
        synchronized (msgFifo) {
            msgFifo.add(str);
        }
    }

    public boolean isConnected() {
        return isOpened;
    }
}
