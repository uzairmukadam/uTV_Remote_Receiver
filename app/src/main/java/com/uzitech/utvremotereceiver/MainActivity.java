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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    EditText port_no;
    Button connect;
    Intent serviceIntent;
    static boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        port_no = findViewById(R.id.port_number);
        connect = findViewById(R.id.connect_btn);
        serviceIntent = new Intent(MainActivity.this, InputService.class);

        getIPAddress();

        final SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        if (preferences.contains("LastPort")) {
            port_no.setText(preferences.getString("LastPort", String.valueOf(R.string.default_port)));
        }
        setConnectionStatus();

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRunning) {
                    if (port_no.getText().toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Enter a port number", Toast.LENGTH_SHORT).show();
                        port_no.requestFocus();
                    } else {
                        int port = Integer.parseInt(port_no.getText().toString());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("LastPort", String.valueOf(port));
                        editor.apply();
                        serviceIntent.putExtra("port_no", port);
                        InputService.enqueueWork(MainActivity.this, serviceIntent);
                        isRunning = true;
                    }
                } else {
                    isRunning = false;
                }
                setConnectionStatus();
            }
        });
    }

    private void getIPAddress() {
        TextView ip_address = findViewById(R.id.ip_address);
        String ip = null;
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

        if (!isRunning) {
            drawable = R.drawable.connection_off;
            connect.setText(R.string.button_status_off);
            port_no.setEnabled(true);
        } else {
            drawable = R.drawable.connection_on;
            connect.setText(R.string.button_status_on);
            port_no.setEnabled(false);
        }

        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            connection_status.setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), drawable));
        } else {
            connection_status.setBackground(ContextCompat.getDrawable(getApplicationContext(), drawable));
        }
    }
}