package com.uzitech.utvremotereceiver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class InputService extends JobIntentService {

    public static final String TAG = "InputService";

    ServerSocket serverSocket;
    ArrayList<String> systemInput;

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, InputService.class, 123, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        systemInput = new ArrayList<>();
        systemInput.add("BTN_PWR");
        systemInput.add("BTN_MUTE");
        systemInput.add("BTN_HOME");
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork");

        Socket socket = null;
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;

        int port = intent.getIntExtra("port_no", 8080);

        try {
            serverSocket = new ServerSocket(port);
            Log.d(TAG, "Receiver Ready");

            while (MainActivity.isRunning) {
                socket = serverSocket.accept();
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                final String messageFromClient;

                messageFromClient = dataInputStream.readUTF();

                performInput(messageFromClient);
                Log.d(TAG, messageFromClient);

                String msgReply = "Received";
                dataOutputStream.writeUTF(msgReply);

            }
            super.onDestroy();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }

            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

    private void performInput(String input) {
        if (MainActivity.isRunning) {
            if (!systemInput.contains(input)) {
                Intent intent = new Intent();
                intent.setAction("utv.uzitech.remote_input");
                intent.putExtra("Remote_Input", input);
                sendBroadcast(intent);
            } else {
                performSystemInput(input);
            }
        }
    }

    private void performSystemInput(String input) {
        if (input.equals("BTN_HOME")) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        } else {
            Log.d(TAG, input);
        }
    }

    @Override
    public void onDestroy() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
        super.onDestroy();
    }
}
