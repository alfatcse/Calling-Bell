package com.example.alfat.callingbell;

import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;

public class ClientActivity extends AppCompatActivity {
    static final int SocketServerPORT = 8080;

    LinearLayout loginPanel, chatPanel;
    Socket socket = null;

    MediaPlayer mp;
    Vibrator vr;

    EditText editTextUserName, editTextAddress;
    Button buttonConnect;
    TextView chatMsg, textPort;

    Button buttonSend;
    Button buttonDisconnect;

    String msgLog = "";

    ChatClientThread chatClientThread = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        setContentView(R.layout.activity_client);

        loginPanel = (LinearLayout)findViewById(R.id.loginpanel);
        chatPanel = (LinearLayout)findViewById(R.id.chatpanel);

        editTextUserName = (EditText) findViewById(R.id.username);
        editTextAddress = (EditText) findViewById(R.id.address);
        textPort = (TextView) findViewById(R.id.port);
        textPort.setText("Port: " + SocketServerPORT);
        buttonConnect = (Button) findViewById(R.id.connect);
        buttonDisconnect = (Button) findViewById(R.id.disconnect);
        chatMsg = (TextView) findViewById(R.id.server_msg);

        mp = MediaPlayer.create(this,R.raw.electricity);
        vr = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);

        //editTextSay = (EditText)findViewById(R.id.say);
        buttonSend = (Button)findViewById(R.id.send);

        buttonSend.setOnClickListener(buttonSendOnClickListener);
    }
    View.OnClickListener buttonDisconnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(chatClientThread==null){
                return;
            }
            chatClientThread.sendMsg("disconnect");
            chatClientThread.disconnect();
        }

    };

    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            /*if (editTextSay.getText().toString().equals("")) {
                return;
            }*/

            if(chatClientThread==null){
                return;
            }

            chatClientThread.sendMsg("acknowledged");
            buttonSend.setVisibility(View.GONE);
        }

    };

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            String textUserName = editTextUserName.getText().toString();
            if (textUserName.equals("")) {
                Toast.makeText(ClientActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String textAddress = editTextAddress.getText().toString();
            if (textAddress.equals("")) {
                Toast.makeText(ClientActivity.this, "Enter Address",
                        Toast.LENGTH_LONG).show();
                return;
            }

            msgLog = "";
            //String ct = DateFormat.getDateTimeInstance().format(new Date());
            //Toast.makeText(Client.this, ct, Toast.LENGTH_SHORT).show();
            chatMsg.setText(msgLog);
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);

            chatClientThread = new ChatClientThread(
                    textUserName, textAddress, SocketServerPORT);
            chatClientThread.start();
            chatMsg.setText("");
        }

    };

    private class ChatClientThread extends Thread {

        String name;
        String dstAddress;
        int dstPort;

        String msgToSend = "";
        boolean goOut = false;

        ChatClientThread(String name, String address, int port) {
            this.name = name;
            dstAddress = address;
            dstPort = port;
        }

        @Override
        public void run() {

            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(name);
                dataOutputStream.flush();

                while (!goOut) {
                    if (dataInputStream.available() > 0) {
                        msgLog = dataInputStream.readUTF();

                        if(msgLog.equals("calling")){
                            mp.start();
                            vr.vibrate(4000);

                            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(4000,VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                vr.vibrate(4000);
                            }*/


                            ClientActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    String ct = DateFormat.getDateTimeInstance().format(new Date());
                                    buttonSend.setVisibility(View.VISIBLE);
                                    chatMsg.append(ct + "::" + msgLog + "\n");
                                }
                            });
                        }

                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                ClientActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ClientActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                ClientActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ClientActivity.this, eString, Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                ClientActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        loginPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

        private void disconnect(){
            goOut = true;
        }
    }

    @Override
    public void onBackPressed() {

        if(chatClientThread != null){
            chatClientThread.sendMsg("disconnect");
            chatClientThread.disconnect();
        }
        finish();
    }
}
