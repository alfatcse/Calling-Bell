package com.example.alfat.callingbell;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button serverbtn, clientbtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WifiManager onwifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        onwifi.setWifiEnabled(true);

        serverbtn=(Button)findViewById(R.id.serverbtn);
        clientbtn=(Button)findViewById(R.id.clientbutton);
    }
    public  void serverclick(View view)
    {
        Intent i=new Intent(this,ServerActivity.class);
        startActivity(i);
    }
    public void clientclick(View view)
    {
        Intent i=new Intent(this,ClientActivity.class);
        startActivity(i);

    }
}
