package com.uzitech.utvremotereceiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    ServerSocket serverSocket;

    Button connect;

    Thread serverThread;

    int port;
    String ip;

    ArrayList<String> systemInput;

    TextView debug_textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect = findViewById(R.id.connect_btn);
        final EditText port_no = findViewById(R.id.port_number);

        final SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        if(preferences.contains("LastPort")){
            port_no.setText(preferences.getString("LastPort", String.valueOf(R.string.default_port)));
        }

        systemInput = new ArrayList<>();

        loadSystemInputs();

        serverThread = new Thread(new InitiateServer());

        setConnectionStatus();
        getIPAddress();

        //debugging tools
        debug_textView = findViewById(R.id.debug_box);


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (serverSocket == null || !serverThread.isAlive()) {
                    if (port_no.getText().toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Enter a port number", Toast.LENGTH_SHORT).show();
                        port_no.requestFocus();
                    } else {
                        port_no.setEnabled(false);
                        port = Integer.parseInt(port_no.getText().toString());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("LastPort", String.valueOf(port));
                        editor.apply();
                        serverThread.start();
                    }
                } else {
                    try {
                        port_no.setEnabled(true);
                        serverSocket.close();
                        serverThread = new Thread(new InitiateServer());
                        setConnectionStatus();
                    } catch (IOException e) {
                        e.printStackTrace();
                        debug_textView.setText(e.toString());
                    }
                }
            }
        });
    }

    private void loadSystemInputs() {
        systemInput.add("BTN_PWR");
        systemInput.add("BTN_MUTE");
        systemInput.add("BTN_HOME");
    }

    void broadcastFunction(String input) {
        if (!systemInput.contains(input)) {
            Intent intent = new Intent();
            intent.setAction("utv.uzitech.remote_input");
            intent.putExtra("Remote_Input", input);
            sendBroadcast(intent);
        } else {
            performSystemInput(input);
        }
    }

    private void performSystemInput(String input) {
        try {
            switch (input) {
                case "BTN_HOME":
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                    break;
                case "BTN_PWR":
                    Toast.makeText(getApplicationContext(), "SCREEN ACTIVITY", Toast.LENGTH_SHORT).show();
                    break;
                case "BTN_MUTE":
                    Toast.makeText(getApplicationContext(), "VOLUME ACTIVITY", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    class InitiateServer extends Thread {

        String message;

        @Override
        public void run() {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                serverSocket = new ServerSocket(port);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        debug_textView.setText("Receiver Ready");
                        setConnectionStatus();
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(
                            socket.getInputStream());
                    dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());

                    final String messageFromClient;

                    messageFromClient = dataInputStream.readUTF();

                    message = messageFromClient;

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            debug_textView.setText(message);
                            broadcastFunction(message);
                        }
                    });

                    String msgReply = "Received";
                    dataOutputStream.writeUTF(msgReply);

                }
            } catch (final IOException e) {
                e.printStackTrace();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        debug_textView.setText(e.toString());
                    }
                });

            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        debug_textView.setText(e.toString());
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        debug_textView.setText(e.toString());
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        debug_textView.setText(e.toString());
                    }
                }
            }
        }
    }

    private void getIPAddress() {
        TextView ip_address = findViewById(R.id.ip_address);

        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress.getHostAddress();
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ip = e.toString();
        }

        ip_address.setText(ip);
    }

    private void setConnectionStatus() {
        final FrameLayout connection_status = findViewById(R.id.connection_status);
        int drawable;

        if (serverSocket == null || !serverThread.isAlive()) {
            drawable = R.drawable.connection_off;
            connect.setText(R.string.button_status_off);
        } else {
            drawable = R.drawable.connection_on;
            connect.setText(R.string.button_status_on);
        }

        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            connection_status.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), drawable));
        } else {
            connection_status.setBackground(ContextCompat.getDrawable(getApplicationContext(), drawable));
        }
    }

    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }
}