package com.example.alfat.callingbell;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class ServerActivity extends AppCompatActivity {
    static final int SocketServerPORT = 8080;

    TextView infoIp, infoPort, clients;//,chatMsg
    Spinner spUsers;
    ArrayAdapter<ChatClient> spUsersAdapter;
    Button btnSentTo;

    String msgLog = "", cname;

    List<ChatClient> userList;

    ServerSocket serverSocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);
        //chatMsg = (TextView) findViewById(R.id.chatmsg);

        spUsers = (Spinner) findViewById(R.id.spusers);
        userList = new ArrayList<ChatClient>();
        spUsersAdapter = new ArrayAdapter<ChatClient>(
                ServerActivity.this, android.R.layout.simple_spinner_item, userList);
        spUsersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUsers.setAdapter(spUsersAdapter);
        clients = (TextView)findViewById(R.id.clients);

        btnSentTo = (Button)findViewById(R.id.sentto);
        btnSentTo.setOnClickListener(btnSentToOnClickListener);

        infoIp.setText(getIpAddress());
        WifiManager onwifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        onwifi.setWifiEnabled(true);
        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();
    }
    View.OnClickListener btnSentToOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChatClient client = (ChatClient)spUsers.getSelectedItem();
            if(client != null){
                String ct = DateFormat.getDateTimeInstance().format(new Date());
                //String dummyMsg = ct+"::"+"Calling..."+"\n";
                String dummyMsg = "calling";
                client.chatThread.sendMsg(dummyMsg);
                //msgLog += "- Dummy message to " + client.name + "\n";
                //chatMsg.setText(msgLog);

            }else{
                Toast.makeText(ServerActivity.this, "No client connected", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                ServerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("Port Address: " + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    final ChatClient client = new ChatClient();
                    userList.add(client);

                    ConnectThread connectThread = new ConnectThread(client, socket);
                    connectThread.start();
                    //cname = client.name;
                    //broadcastMsg(client.toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spUsersAdapter.notifyDataSetChanged();
                            //Toast.makeText(ServerActivity.this, client.name, Toast.LENGTH_SHORT).show();

                            //clients.append(client.toString()+" connected.\n");
                            //Toast.makeText(MainActivity.this, client.toString(), Toast.LENGTH_SHORT).show();

                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private class ConnectThread extends Thread {

        Socket socket;
        ChatClient connectClient;
        String msgToSend = "";

        ConnectThread(ChatClient client, Socket socket){
            connectClient = client;
            this.socket= socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String n = dataInputStream.readUTF();

                connectClient.name = n;

                /*msgLog += connectClient.name + " connected@" +
                        connectClient.socket.getInetAddress() +
                        ":" + connectClient.socket.getPort() + "\n";*/
                String ct = DateFormat.getDateTimeInstance().format(new Date());
                msgLog = ct+"::"+connectClient.name + " Connected\n";
                ServerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        clients.append(msgLog);
                    }
                });

                //dataOutputStream.writeUTF("Welcome " + n + "\n");
                //dataOutputStream.flush();

                //broadcastMsg(n + " join our chat.\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String newMsg = dataInputStream.readUTF();


                        //msgLog = n + ": " + newMsg+"\n";
                        if(newMsg.equals("disconnect")){
                            msgLog = newMsg+"\n";

                            ServerActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    spUsersAdapter.notifyDataSetChanged();
                                    String ct = DateFormat.getDateTimeInstance().format(new Date());
                                    clients.append(ct+"::"+connectClient.name + " Disconnected\n");
                                }
                            });
                            userList.remove(connectClient);
                        }

                        if(newMsg.equals("acknowledged")){
                            msgLog = newMsg;
                            ServerActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    String ct = DateFormat.getDateTimeInstance().format(new Date());
                                    clients.append(ct+"::"+connectClient.name+" Acknowledged\n");
                                }
                            });
                        }



                        //broadcastMsg(n + ": " + newMsg);
                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);
                        dataOutputStream.flush();
                        msgToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
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

                userList.remove(connectClient);

                ServerActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        spUsersAdapter.notifyDataSetChanged();
                        clients.append(connectClient.name + " removed.");
                        //Toast.makeText(ServerActivity.this,
                        //        connectClient.name + " removed.", Toast.LENGTH_LONG).show();

                        //msgLog += "-- " + connectClient.name + " leaved\n";
                        /*ServerActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                clients.append(connectClient.name + " removed.");
                            }
                        });*/

                        //broadcastMsg("-- " + connectClient.name + " leaved\n");
                    }
                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg){
        for(int i=0; i<userList.size(); i++){
            userList.get(i).chatThread.sendMsg(msg);
            msgLog += "- send to " + userList.get(i).name + "\n";
        }

        /*MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                chatMsg.setText(msgLog);
            }
        });*/
    }

    private String getIpAddress() {
        String ip = "";
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
                        ip += "IP: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    class ChatClient {
        String name;
        Socket socket;
        ConnectThread chatThread;

        @Override
        public String toString() {
            return name;/* + ": " + socket.getInetAddress().getHostAddress();*/
        }
    }
}
