package com.example.xchanger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity {

    Button btnonoff,btndiscover, btnsend;
    ListView listView;
    TextView rdmsgbox, connectionstatus;
    EditText wrmsg;
    WifiManager wifiManager;
    WifiP2pManager mmanager;
    WifiP2pManager.Channel mchannel;
    BroadcastReceiver mreciever;
    IntentFilter mintentfilter;

    List<WifiP2pDevice> peers =new ArrayList<WifiP2pDevice>();
    String[] devicenamearray;
    WifiP2pDevice[] devicearray;
    static final int message_read =1;
    Serverclass serverclass;
    Clientclass clientclass;
    SendRecieve sendRecieve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialwork();

        exqlistener();


    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what){
                case message_read:
                    byte[] readbuff = (byte[]) msg.obj;
                    String tempmsg = new String(readbuff,0,msg.arg1);
                    rdmsgbox.setText(tempmsg);
                    break;

            }
            return true;
        }
    });

    private void exqlistener() {

        btnonoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnonoff.setText("ON");
                }
                else{
                    wifiManager.setWifiEnabled(true);
                    btnonoff.setText("OFF");
                }

            }
        });

        btndiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mmanager.discoverPeers(mchannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionstatus.setText("Discovery started");
                    }

                    @Override
                    public void onFailure(int reason) {

                        connectionstatus.setText("Discovery starting failed");
                    }
                });
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = devicearray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                mmanager.connect(mchannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"connected to " + device.deviceName,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {

                        Toast.makeText(getApplicationContext(),"not connected",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = wrmsg.getText().toString();
                sendRecieve.write(msg.getBytes());
            }
        });
    }


    private void initialwork() {

        btnonoff = findViewById(R.id.onOff);
        btndiscover = findViewById(R.id.discover);
        btnsend = findViewById(R.id.sendButton);
        listView = findViewById(R.id.peerListView);
        rdmsgbox = findViewById(R.id.readMsg);
        connectionstatus = findViewById(R.id.connectionStatus);
        wrmsg = findViewById(R.id.writeMsg);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mmanager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mchannel = mmanager.initialize(this,getMainLooper(),null);

        mreciever = new WifiDirectBroadcastReceiver(mmanager,mchannel,this);
        mintentfilter = new IntentFilter();
        mintentfilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mintentfilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mintentfilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mintentfilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerlist) {
            if (!peerlist.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerlist.getDeviceList());

                devicenamearray = new String[peerlist.getDeviceList().size()];
                devicearray = new WifiP2pDevice[peerlist.getDeviceList().size()];
                int index = 0;

                for (WifiP2pDevice device : peerlist.getDeviceList()){
                    devicenamearray[index]=device.deviceName;
                    devicearray[index]=device;

                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,devicenamearray);
                listView.setAdapter(adapter);

            }
            if (peers.size()==0){
                Toast.makeText(getApplicationContext(),"no device found",Toast.LENGTH_SHORT).show();
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupowneraddress = info.groupOwnerAddress;
            if (info.groupFormed && info.isGroupOwner){
                connectionstatus.setText("host");
                serverclass = new Serverclass();
                serverclass.start();
            }
            else if (info.groupFormed){
                connectionstatus.setText("client");
                clientclass = new Clientclass(groupowneraddress);
                clientclass.start();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mreciever,mintentfilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mreciever);
    }

    public class Serverclass extends Thread{

        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public  class Clientclass extends Thread{
        Socket socket;
        String hostadd;

        public Clientclass(InetAddress hostaddress){
            hostadd = hostaddress.getHostAddress();
            socket =  new Socket();

        }

        @Override
        public void run() {
            super.run();
            try {
                socket.connect(new InetSocketAddress(hostadd,8888),1000);
                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendRecieve extends  Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendRecieve (Socket skt){
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket!=null){
                try {
                    bytes = inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(message_read,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
