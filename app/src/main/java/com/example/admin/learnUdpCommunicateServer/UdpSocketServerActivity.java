package com.example.admin.learnUdpCommunicateServer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author zhou.jn
 */
public class UdpSocketServerActivity extends Activity {
    private static final String TAG = "SocketAutoConnectServer";
    private static String IP;
    private static int BROADCAST_PORT = 9999;
    private static String BROADCAST_IP = "255.255.255.255";
    private InetAddress inetAddress = null;
    private BroadcastThread broadcastThread;
    private DatagramSocket sendSocket = null;
    private DatagramSocket receiveSocket = null;
    private Button sendUDPBrocast;
    private volatile boolean isRuning = true;
    private TextView ipInfo;
    private Button btn_send;
    private EditText et_sendInfo;
    private String sendContent;
    private TextView tv_receive;
    private List<String> ipList = new ArrayList<>();
    private Button btnClear;
    private ReceiveThread receiveThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initView();
        initIp();
        initThread();
        try {
            inetAddress = InetAddress.getByName(BROADCAST_IP);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initThread() {
        broadcastThread = new BroadcastThread();
        broadcastThread.start();
        receiveThread = new ReceiveThread();
        receiveThread.start();
    }

    private void initIp() {
        //Wifi状态判断
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            IP = getIpString(wifiInfo.getIpAddress());
            ipInfo.append(IP);
            System.out.println("IP IP:" + IP);
        }
    }

    private void initView() {
        ipInfo = (TextView) findViewById(R.id.ip_info);
        sendUDPBrocast = (Button) findViewById(R.id.sendUDPBrocast);
        tv_receive = findViewById(R.id.tv_receive);
        et_sendInfo = findViewById(R.id.et_sendContent);
        btn_send = findViewById(R.id.btn_sendInfo);
        try {
            receiveSocket = new DatagramSocket(BROADCAST_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        btn_send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToThread(broadcastThread.mhandler);
            }
        });
        btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                tv_receive.setText("");
            }
        });
        sendUDPBrocast.setOnClickListener(new SendUDPBrocastListener());
    }

    private void sendMessageToThread(Handler mhandler) {
        Message msg = Message.obtain();
        sendContent = et_sendInfo.getText().toString();
        msg.obj = sendContent;
        msg.what = 1;
        mhandler.sendMessage(msg);
    }


    /**
     * 将获取到的int型ip转成string类型
     */
    private String getIpString(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }

    @SuppressLint("HandlerLeak")
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: {
                    if (!msg.obj.equals(IP)) {
                        if (!isExistIp(msg.obj.toString())) {
                            ipList.add(msg.obj.toString());
                        }
                        tv_receive.append(msg.obj.toString() + " 接收到信息 " + "\n");
                    }
                }
                break;
                default:
                    break;
            }
        }

    };

    public class SendUDPBrocastListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (isRuning) {
                isRuning = false;
                sendUDPBrocast.setText("发送广播");
                System.out.println("现在停止广播..");
            } else {
                isRuning = true;
                sendUDPBrocast.setText("停止广播");
                System.out.println("现在发送广播..");
            }
        }
    }

    public class BroadcastThread extends Thread {
        private Handler mhandler = null;

        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            Looper.prepare();
            mhandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    String message = (String) msg.obj;
                    byte[] data = message.getBytes();
                    DatagramPacket dpSend = null;
                    dpSend = new DatagramPacket(data, data.length, inetAddress, BROADCAST_PORT);
                    try {
                        double start = System.currentTimeMillis();
                        for (int i = 0 ; i < 15; i ++) {
                            sendSocket = new DatagramSocket();
                            sendSocket.send(dpSend);
                            sendSocket.close();
                            Thread.sleep(80);
                            Log.i(TAG, "sendMessage: data " + new String(data));
                        }
                        double end = System.currentTimeMillis();
                        double times = end - start;
                        Log.i(TAG, "receive: executed time is : "+ times +"ms");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Looper.loop();
        }
    }

    private boolean isExistIp(String revIp) {
        if (ipList != null && ipList.size() > 0) {
            for (String ip : ipList) {
                if (ip != revIp) {
                    return false;
                }
            }
        }
        return false;
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            while (true) {
                if (isRuning) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket dpReceive = null;
                    ipList.clear();
                    dpReceive = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        receiveSocket.receive(dpReceive);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String recIp = dpReceive.getAddress().toString().substring(1);
                    if (dpReceive != null) {
                        Message revMessage = Message.obtain();
                        revMessage.what = 1;
                        revMessage.obj = recIp;
                        Log.i(TAG, "handleMessage: receive ip" + recIp);
                        myHandler.sendMessage(revMessage);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRuning = false;
        receiveSocket.close();
        System.out.println("UDP Server程序退出,关掉socket,停止广播");
        finish();
    }
}