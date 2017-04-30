package seng302.server;

import seng302.server.messages.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

class StreamingServerSocket {
    private ServerSocketChannel socket;
    private SocketChannel client;
    private List<Socket> clients;
    private short seqNum;
    private boolean isServerStarted;

    StreamingServerSocket(int port) throws IOException{
        socket = ServerSocketChannel.open();
        socket.socket().bind(new InetSocketAddress("localhost", port));
        clients = new ArrayList<>();
        //socket.setSoTimeout(10000);
        seqNum = 0;
        isServerStarted = false;
    }

    void start(){
        ServerThread.serverLog("Listening For Connections",0);
        try {
            client = socket.accept();
        } catch (IOException e) {
            e.getMessage();
        }
        if (client.socket() == null){
            start();
        }
        else{
            isServerStarted = true;
            ServerThread.serverLog("client connected from " + client.socket().getInetAddress(),0);
        }
    }

    void send(Message message) throws IOException{
        if (client == null){
            return;
        }

        //DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
        //System.out.println(client);
        message.send(client);


        seqNum++;
    }

    public short getSequenceNumber(){
        return seqNum;
    }

    public boolean isStarted(){
        return isServerStarted;
    }
}
