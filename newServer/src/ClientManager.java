/*
Patrick Nelson 2021
Java Multi-threaded Chat Server w/ GUI
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ClientManager implements Runnable {
    /* A class to be run as a thread
       by server class, representing
       each client Connection and
       managing the I/O with
       each connection.
    */
    final String userName;
    final DataInputStream dataInputStream;
    final DataOutputStream dataOutputStream;
    Socket socket;
    boolean loggedOn;
    private UserSessionManager usersOnline;
    private JFrame serverMessageFrame;
    private JTextArea serverMessageDiag;
    private JTextField Response;
    private ArrayList<ClientManager> clientsList;

    public ClientManager(String userName, Socket socket, OutputStream out, InputStream in ) {
        this.userName = userName;
        this.socket = socket;
        this.dataOutputStream = new DataOutputStream(out);
        this.dataInputStream = new DataInputStream(in);
        this.loggedOn = true;
        this.serverMessageFrame = new JFrame();
        serverMessageFrame.setSize(200, 200);
        serverMessageFrame.setVisible(false);
        this.serverMessageDiag = new JTextArea();
        this.Response = new JTextField();
        serverMessageDiag.setBackground(Color.BLACK);
        serverMessageDiag.setForeground(Color.GREEN);
        serverMessageDiag.setFont(new Font("Monospaced", Font.PLAIN, 12));
        Response.setBackground(Color.BLACK);
        Response.setForeground(Color.GREEN);
        Response.setFont(new Font("Monospaced", Font.PLAIN, 12));
        Response.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        Response.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        String message =actionEvent.getActionCommand();
                        serverMessageDiag.append("Server: " + message + "\n");
                        try {
                            sendMessagePacket(message, "Server", userName, 1, socket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Response.setText("");

                    }
                }
        );
        serverMessageFrame.add(serverMessageDiag);
        serverMessageFrame.add(Response, BorderLayout.SOUTH);
        serverMessageFrame.setTitle("Message from " + userName);
        serverMessageDiag.append(" \n");
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                /* objIn may encounter a MessagePacket
                or a UserSessionManager Object containing
                a list of users, so it checks for either instanceof
                */
               try {
                   ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
                   Object newObj = objIn.readObject();
                   if (newObj instanceof MessagePacket) {
                       MessagePacket newMessage = (MessagePacket) newObj;
                       handleMessagePacket(newMessage);
                   }
                   if (newObj instanceof UserSessionManager) {
                       setUserList((UserSessionManager) newObj);
                   }
               }  catch (EOFException e) {
                loggedOn = false;
                socket.close();
                Thread.currentThread().join();
            }
            } catch (EOFException e) {
                e.printStackTrace();
                break;
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
                continue;
            }
        }
        try {
            this.socket.close();
        } catch (IOException e) {
            System.out.println("Client closed");
        }
    }

    public void handleMessagePacket(MessagePacket messageIn) throws IOException {
        /*
        routes the messages to the receiver via
        the packetHeader integer
        case 1, send message directly to server
        case 2, sends message through server from client to another client.
        */
        switch (messageIn.getPacketHeader()) {
            case 1:
            {
                serverMessageFrame.setVisible(true);
                serverMessageDiag.append(messageIn.getSender() + ": " + messageIn.getMessage() + "\n");
                break;
            }
            case 2:
            {
                clientToClient(messageIn);
                break;
            }
        }
    }

    private void clientToClient(MessagePacket messageIn) throws IOException {
        /* Handles messages between clients by finding the right client to sendTo
        in the clientList
        */
        String sendTo = messageIn.getSendTo();
        for (ClientManager clients : clientsList ) {
            String clientCheck = (String) clients.userName;
            if (clientCheck.equals(sendTo)) {
                    sendMessagePacket(messageIn.getMessage(), messageIn.getSender(), sendTo, 2, clients.socket);
            }
        }
    }

    public void sendMessagePacket(String message, String userName, String sendTo, int packetHeader, Socket socket) throws IOException {
        /* creates and sends a messagePacket to desired location  */
        MessagePacket newMessage = new MessagePacket(message, userName, sendTo, packetHeader);
        try{
        ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
        objOut.writeObject(newMessage);
        objOut.flush(); }
        catch (SocketException s) {
            serverMessageDiag.append("Client Connection Lost");
        }
    }
    // Standard setters/getters for values that need to be modified in this class
    public void setUserList(UserSessionManager usersOnline ) { this.usersOnline = usersOnline; }
    public UserSessionManager getUserList() { return this.usersOnline; }

    public void setClientsList(ArrayList clientsList) { this.clientsList = clientsList; }
 }
