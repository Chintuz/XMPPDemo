package com.xmpp.model;

import java.util.Random;

/**
 * Created by admin on 05/03/2016.
 */
public class ChatMessage {

    public String body, sender, receiver, senderName;
    public String Date, Time;
    public String msgid;
    public boolean isMine;// Did I send the message.
    public String imagePath = "";

    public ChatMessage(String Sender, String Receiver, String messageString, String iPath, String ID, boolean isMINE) {
        body = messageString;
        isMine = isMINE;
        sender = Sender;
        msgid = ID;
        receiver = Receiver;
        imagePath = iPath;
        senderName = sender;
    }

    public void setMsgID() {
        msgid += "-" + String.format("%02d", new Random().nextInt(100));
    }
}
