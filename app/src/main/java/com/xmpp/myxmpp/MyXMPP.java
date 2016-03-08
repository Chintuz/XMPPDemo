package com.xmpp.myxmpp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.xmpp.Utils.CommonMethods;
import com.xmpp.demo.R;
import com.xmpp.fragment.Chats;
import com.xmpp.model.ChatMessage;
import com.xmpp.service.MyService;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MyXMPP {

    public static boolean connected = false;
    public boolean loggedin = false;
    public static boolean isconnecting = false;
    public static boolean isToasted = true;
    private boolean chat_created = false;
    private String serverAddress;
    public static XMPPTCPConnection connection;
    public static String loginUser;
    public static String passwordUser;
    Gson gson;
    MyService context;
    public static MyXMPP instance = null;
    public static boolean instanceCreated = false;

    public org.jivesoftware.smack.chat.Chat Mychat;
    ChatManagerListenerImpl mChatManagerListener;
    MMessageListener mMessageListener;
    String text = "";
    String mMessage = "", mReceiver = "";

    public MyXMPP(MyService context, String serverAdress, String logiUser, String passwordser) {
        this.serverAddress = serverAdress;
        this.loginUser = logiUser;
        this.passwordUser = passwordser;
        this.context = context;
        init();
    }

    public static MyXMPP getInstance(MyService context, String server, String user, String pass) {
        if (instance == null) {
            instance = new MyXMPP(context, server, user, pass);
            instanceCreated = true;
        }
        return instance;
    }

    static {
        try {
            Class.forName("org.jivesoftware.smack.ReconnectionManager");
        } catch (ClassNotFoundException ex) {
            // problem loading reconnection manager
        }
    }

    public void init() {
        gson = new Gson();
        mMessageListener = new MMessageListener(context);
        mChatManagerListener = new ChatManagerListenerImpl();
        initialiseConnection();
    }

    private void initialiseConnection() {
        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setServiceName(serverAddress);
        config.setHost(serverAddress);
        config.setConnectTimeout(100000);
        config.setPort(5222);
        config.setDebuggerEnabled(true);
        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);
        connection = new XMPPTCPConnection(config.build());
        connection.setPacketReplyTimeout(100000);
        XMPPConnectionListener connectionListener = new XMPPConnectionListener();
        connection.addConnectionListener(connectionListener);
        FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
        manager.addFileTransferListener(new MyFileTransferListener());
    }

    public void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                connection.disconnect();
            }
        }).start();
    }

    public void connect(final String caller) {

        AsyncTask<Void, Void, Boolean> connectionThread = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected synchronized Boolean doInBackground(Void... arg0) {
                if (connection.isConnected())
                    return false;
                isconnecting = true;
                if (isToasted)
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {

                            Toast.makeText(context, caller + "=>connecting....", Toast.LENGTH_LONG).show();
                        }
                    });
                Log.d("Connect() Function", caller + "=>connecting....");

                try {
                    connection.connect();
                    DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(connection);
                    dm.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
                    dm.addReceiptReceivedListener(new ReceiptReceivedListener() {

                        @Override
                        public void onReceiptReceived(String fromJid, String toJid, String receiptId, Stanza receipt) {
                            Log.v("syso", "from_Server" + fromJid + toJid + receiptId + receipt);
                        }
                    });
                    connected = true;

                } catch (IOException e) {
                    if (isToasted)
                        new Handler(Looper.getMainLooper())
                                .post(new Runnable() {

                                    @Override
                                    public void run() {

                                        Toast.makeText(context, "(" + caller + ")" + "IOException: ", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    Log.e("(" + caller + ")", "IOException: " + e.getMessage());
                } catch (SmackException e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(context, "(" + caller + ")" + "SMACKException: ", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("(" + caller + ")", "SMACKException: " + e.getMessage());
                } catch (XMPPException e) {
                    if (isToasted)

                        new Handler(Looper.getMainLooper())
                                .post(new Runnable() {

                                    @Override
                                    public void run() {

                                        Toast.makeText(context, "(" + caller + ")" + "XMPPException: ", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    Log.e("connect(" + caller + ")",
                            "XMPPException: " + e.getMessage());

                }
                return isconnecting = false;
            }
        };
        connectionThread.execute();
    }

    public void login() {

        try {
            connection.login(loginUser, passwordUser);
            Presence presence = new Presence(Presence.Type.available);
            connection.sendStanza(presence);
            Log.i("LOGIN", "Yey! We're connected to the Xmpp server!");
            Toast.makeText(context, "Yey! We're connected to the Xmpp server!", Toast.LENGTH_LONG).show();

        } catch (XMPPException | SmackException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }

    }

    private class ChatManagerListenerImpl implements ChatManagerListener {


        @Override
        public void chatCreated(Chat chat, boolean createdLocally) {
            if (!createdLocally)
                chat.addMessageListener(mMessageListener);
        }
    }

    public void sendMessage(ChatMessage chatMessage) {
        String body = gson.toJson(chatMessage);

        if (!chat_created) {
            Mychat = ChatManager.getInstanceFor(connection).createChat(chatMessage.receiver + "@" + context.getString(R.string.server), mMessageListener);
            chat_created = true;
        }
        final Message message = new Message();
        message.setBody(body);
        message.setStanzaId(chatMessage.msgid);
        message.setType(Message.Type.chat);

        try {
            if (connection.isAuthenticated()) {

                Mychat.sendMessage(message);

            } else {

                login();
            }
        } catch (SmackException.NotConnectedException e) {
            Log.e("xmpp.SendMessage()", "msg Not sent!-Not Connected!");

        } catch (Exception e) {
            Log.e("SendMessage()-Exception", "msg Not sent!" + e.getMessage());
        }

    }

    public void sendImageFile(String filenameWithPath) {
        FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
        OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer("chikuz@xmpp.jp/Smack");
        File file = new File(filenameWithPath);
        try {
            transfer.sendFile(file, "test_file");
        } catch (SmackException e) {
            e.printStackTrace();
        }
        while (!transfer.isDone()) {
            if (transfer.getStatus().equals(FileTransfer.Status.error)) {
                System.out.println("ERROR!!! " + transfer.getError());
            } else if (transfer.getStatus().equals(FileTransfer.Status.cancelled)
                    || transfer.getStatus().equals(FileTransfer.Status.refused)) {
                System.out.println("Cancelled!!! " + transfer.getError());
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (transfer.getStatus().equals(FileTransfer.Status.refused) || transfer.getStatus().equals(FileTransfer.Status.error)
                || transfer.getStatus().equals(FileTransfer.Status.cancelled)) {
            System.out.println("refused cancelled error " + transfer.getError());
        } else {
            System.out.println("Success");
        }
    }

    public class MyFileTransferListener implements FileTransferListener {

        @Override
        public void fileTransferRequest(final FileTransferRequest request) {
            new Thread() {
                @Override
                public void run() {
                    IncomingFileTransfer transfer = request.accept();
                    Random random = new Random();
                    File mf = Environment.getExternalStorageDirectory();
                    File file = new File(mf.getAbsoluteFile() + "/DCIM/mycamera/" + transfer.getFileName());
                    try {
                        transfer.recieveFile(file);
                        final ChatMessage chatMessage = new ChatMessage("user1", "user2", "", "", transfer.getFileName() + random.nextInt(1000), true);
                        chatMessage.setMsgID();
                        chatMessage.body = "";
                        chatMessage.Date = CommonMethods.getCurrentDate();
                        chatMessage.isMine = false;
                        chatMessage.Time = CommonMethods.getCurrentTime();
                        chatMessage.imagePath = file.getAbsolutePath();
                        processMessagee(chatMessage);
                        while (!transfer.isDone()) {
                            try {
                                Thread.sleep(1000L);
                            } catch (Exception e) {
                                Log.e("error", e.getMessage());
                            }
                            if (transfer.getStatus().equals(FileTransfer.Status.error)) {
                                Log.e("ERROR!!! ", transfer.getError() + "");
                            }
                            if (transfer.getException() != null) {
                                transfer.getException().printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("error", e.getMessage());
                    }
                }

                ;
            }.start();
        }
    }

    public class XMPPConnectionListener implements ConnectionListener {


        @Override
        public void connected(final XMPPConnection connection) {

            Log.d("xmpp", "Connected!");
            connected = true;
            if (!connection.isAuthenticated()) {
                login();
            }
        }

        @Override
        public void connectionClosed() {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(context, "ConnectionCLosed!", Toast.LENGTH_SHORT).show();
                    }
                });
            Log.d("xmpp", "ConnectionCLosed!");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void connectionClosedOnError(Exception arg0) {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "ConnectionClosedOn Error!!", Toast.LENGTH_SHORT).show();
                    }
                });
            Log.d("xmpp", "ConnectionClosedOn Error!");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectingIn(int arg0) {
            Log.d("xmpp", "Reconnectingin " + arg0);
            loggedin = false;
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "ReconnectionFailed!", Toast.LENGTH_SHORT).show();
                    }
                });
            Log.d("xmpp", "ReconnectionFailed!");
            connected = false;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void reconnectionSuccessful() {
            if (isToasted)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(context, "REConnected!", Toast.LENGTH_SHORT).show();
                    }
                });
            Log.d("xmpp", "ReconnectionSuccessful");
            connected = true;
            chat_created = false;
            loggedin = false;
        }

        @Override
        public void authenticated(XMPPConnection arg0, boolean arg1) {
            Log.d("xmpp", "Authenticated!");
            loggedin = true;

            ChatManager.getInstanceFor(connection).addChatListener(mChatManagerListener);
            chat_created = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }).start();
            if (isToasted)

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }


    private void processMessagee(final ChatMessage chatMessage) {

        chatMessage.isMine = false;
        Chats.chatlist.add(chatMessage);
        new Handler(Looper.getMainLooper()).post(new Runnable() {

            @Override
            public void run() {
                Chats.chatAdapter.notifyDataSetChanged();
            }
        });
    }

    private class MMessageListener implements ChatMessageListener {

        public MMessageListener(Context contxt) {
        }

        @Override
        public void processMessage(final org.jivesoftware.smack.chat.Chat chat, final Message message) {
            Log.i("MyXMPP_MESSAGE_LISTENER", "Xmpp message received: '" + message);

            if (message.getType() == Message.Type.chat && message.getBody() != null) {
                final ChatMessage chatMessage = gson.fromJson(message.getBody(), ChatMessage.class);
                processMessagee(chatMessage);
            }
        }

      /*  private void processMessage(final ChatMessage chatMessage) {

            chatMessage.isMine = false;
            Chats.chatlist.add(chatMessage);
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    Chats.chatAdapter.notifyDataSetChanged();
                }
            });
        }*/

    }
}