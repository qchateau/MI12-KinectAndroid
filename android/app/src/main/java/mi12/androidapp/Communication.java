package mi12.androidapp;

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

public class Communication extends Observable {
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
                isOpened = false;
                setChanged();
                try {
                    socket = new Socket(host, port);
                    outStream = socket.getOutputStream();
                } catch (Exception e) {
                    e.printStackTrace();
                    notifyObservers(new CommunicationState(isOpened));
                    return;
                }
                isOpened = true;
                setChanged();
                while (isOpened && !Thread.currentThread().isInterrupted()) {
                    String str = "";
                    try {
                        synchronized (msgFifo) {
                            str = msgFifo.removeFirst();
                        }
                    } catch (NoSuchElementException e) {
                    }
                    try {
                        outStream.write(str.getBytes());
                    } catch (IOException e) {
                        Log.e(TAG, "Could not send : " + str);
                        e.printStackTrace();
                        isOpened = false;
                        setChanged();
                    }
                    notifyObservers(new CommunicationState(isOpened));
                }
                try {
                    Log.e(TAG, "Closing socket");
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close socket");
                    e.printStackTrace();
                }
            }
        });
    }

    public void Connect() {
        thread.start();
    }

    public void Disconnect() {
        isOpened = false;
        setChanged();
    }

    public static Communication getInstance(String n_host, int n_port) {
        if (com != null) {
            com.Disconnect();
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
