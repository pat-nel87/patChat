/*
Patrick Nelson 2021
Java Multi-threaded ChatClient w/ GUI
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConversationManager implements Runnable {

    private JFrame conversationWindow;
    private JTextArea conversationDialogue;
    private JTextField conversationEntry;
    private Socket socket;
    private MessagePacket currentMessage;
    private final String otherClient;
    private final String myUsername;
    private boolean activeConversation;


    public void addMessage(String msg, String user) {
        String newMessage = (user + ": " + msg + "\n");
        conversationDialogue.append(newMessage);

    }
    public String getOtherClient() { return this.otherClient; }


    ClientConversationManager(String myUsername, String otherClient, Socket socket) {

        this.myUsername = myUsername;
        this.otherClient = otherClient;
        this.socket = socket;
        this.activeConversation = true;

        this.conversationWindow = new JFrame();
        this.conversationDialogue = new JTextArea();
        this.conversationEntry = new JTextField();
        conversationEntry.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        String temp = actionEvent.getActionCommand();
                        try {
                            conversationDialogue.append(myUsername + ": " + temp + "\n");
                            sendMessage(myUsername, temp, otherClient, 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        conversationEntry.setText("");
                    }
                }
        );
        conversationWindow.add(conversationDialogue);
        conversationWindow.add(conversationEntry, BorderLayout.SOUTH);
        conversationWindow.setTitle("Chatting with " + otherClient);
        conversationWindow.setSize(200, 200);
        conversationDialogue.setBackground(Color.BLACK);
        conversationEntry.setBackground(Color.BLACK);
        conversationEntry.setFont(new Font("Monospaced", Font.PLAIN, 12));
        conversationEntry.setForeground(Color.GREEN);
        conversationEntry.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        conversationDialogue.setFont(new Font("Monospaced", Font.PLAIN, 12));
        conversationDialogue.setForeground(Color.GREEN);
        conversationWindow.setVisible(true);

    }

    ClientConversationManager(String myUsername, String otherClient, Socket socket, MessagePacket currentMessage) {

        this.myUsername = myUsername;
        this.otherClient = otherClient;
        this.socket = socket;
        this.activeConversation = true;
        this.currentMessage = currentMessage;

        this.conversationWindow = new JFrame();
        this.conversationDialogue = new JTextArea();
        this.conversationEntry = new JTextField();
        conversationEntry.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        String temp = actionEvent.getActionCommand();
                        try {
                            conversationDialogue.append(myUsername + ": " + temp + "\n");
                            sendMessage(myUsername, temp, otherClient, 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        conversationEntry.setText("");
                    }
                }
        );
        conversationWindow.add(conversationDialogue);
        conversationWindow.add(conversationEntry, BorderLayout.SOUTH);
        conversationDialogue.setBackground(Color.BLACK);
        conversationEntry.setBackground(Color.BLACK);
        conversationEntry.setFont(new Font("Monospaced", Font.PLAIN, 12));
        conversationEntry.setForeground(Color.GREEN);
        conversationEntry.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        conversationDialogue.setFont(new Font("Monospaced", Font.PLAIN, 12));
        conversationDialogue.setForeground(Color.GREEN);
        conversationWindow.setVisible(true);
        conversationWindow.setTitle("Chatting with " + otherClient);
        conversationWindow.setSize(200, 200);
        conversationWindow.setVisible(true);

        addMessage(currentMessage.getMessage(), currentMessage.getSender());


    }

    public void setActiveConversation(boolean tf) { this.activeConversation = tf; }
    public boolean getActiveConversation() { return this.activeConversation; }

    @Override
    public void run() {

    }

        private void sendMessage (String userName, String message, String sendTo,int packetHeader) throws IOException {
        /* Sends Message to Server to be re routed to proper client.
         PacketHeader sends integers 1 or 2
         1 means message is for server
         2 means message is for another client
         Server will handle request accordingly
        */
            ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
            MessagePacket newMessage = new MessagePacket(message, userName, sendTo, packetHeader);
            newMessage.setActiveMessage(true);
            objOut.writeObject(newMessage);
            objOut.flush();
        }
    }
