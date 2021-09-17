/*
Patrick Nelson 2021
Java Multi-threaded ChatClient w/ GUI
*/
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Client extends JFrame {

    final static int ServerPort = 8818;
    private String serverIP;
    private BufferedOutputStream outputStream;
    private BufferedInputStream inputStream;
    private JFrame clientWindow;
    private JPanel clientPanel;
    private JList clientList;
    private DefaultListModel<String> clientListModel;
    private JTextArea clientTextArea;
    private JTextField clientTextEntry;
    private Socket clientSocket;
    private JFrame newMessageFrame;
    private JTextArea newMessageDiag;
    private JTextField newMessageEntry;
    private String myUserName;
    private UserSessionManager myUserSession;
    private ArrayList<ClientConversationManager> conversations;

    public Client(String serverIP) throws IOException {

        this.serverIP = serverIP;
        this.clientWindow = new JFrame();
        this.clientTextArea = new JTextArea();
        this.clientTextEntry = new JTextField();
        clientTextEntry.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        messageAll(actionEvent.getActionCommand());
                        clientTextEntry.setText("");
                    }
                }
        );
        this.clientPanel = new JPanel();
        this.clientWindow.setTitle("User List");
        this.clientListModel = new DefaultListModel<>();
        this.clientList = new JList<>(clientListModel);
        clientList.setBackground(Color.BLACK);
        clientList.setForeground(Color.GREEN);
        clientList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.conversations = new ArrayList<>();
        clientList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    String temp = (String) clientList.getSelectedValue();
                    startConversation(temp);
                }
            }
        });
        this.clientPanel.setBackground(Color.BLACK);
        this.clientPanel.add(new JScrollPane(clientList), BorderLayout.SOUTH);
        this.clientWindow.add(clientPanel);
        this.clientWindow.setSize(500, 200);
        this.clientWindow.setVisible(true);
        this.clientWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.clientWindow.setLocationRelativeTo(null);
        this.newMessageFrame = new JFrame();
        this.newMessageDiag = new JTextArea();
        this.newMessageEntry = new JTextField();
        this.newMessageFrame.add(newMessageDiag);
        this.newMessageFrame.add(newMessageEntry, BorderLayout.SOUTH);
        this.newMessageFrame.setTitle("Talking to Server");
        this.newMessageFrame.setLocationRelativeTo(clientWindow);
        newMessageEntry.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        String message = actionEvent.getActionCommand();
                        newMessageDiag.append(myUserName + ": " + message + "\n");
                        newMessageEntry.setText("");
                        try {
                            sendMessage(getMyUserName(), message, "Server", 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        this.newMessageFrame.setSize(200, 200);
        this.newMessageFrame.setVisible(false);
        this.clientSocket = new Socket(serverIP, ServerPort);
        newMessageDiag.setBackground(Color.BLACK);
        newMessageEntry.setBackground(Color.BLACK);
        newMessageEntry.setFont(new Font("Monospaced", Font.PLAIN, 12));
        newMessageEntry.setForeground(Color.GREEN);
        newMessageEntry.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        newMessageDiag.setFont(new Font("Monospaced", Font.PLAIN, 12));
        newMessageDiag.setForeground(Color.GREEN);

        String test1 = "Client";
        String test2 = "New Connection";
        String test3 = "Server";
        sendMessage(test1, test2, test3, 1);

        while (true) {

            try {
                ObjectInputStream objIn = new ObjectInputStream(clientSocket.getInputStream());
                Object newObj = objIn.readObject();
                if (newObj instanceof MessagePacket) {
                    MessagePacket newMessage = (MessagePacket) newObj;
                    handleMessagePacket(newMessage);
                }
                if (newObj instanceof UserSessionManager) {
                    System.out.println("User list received");
                    setMyUserSession((UserSessionManager) newObj);
                    updateClientList();
                }
            } catch (EOFException ex) {
                String errorMsg = ("Server Connection Lost!");
                ErrorMethod(errorMsg);
                break;
            } catch (StreamCorruptedException s) {
                s.printStackTrace();
                continue;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();

            }
        }
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            System.out.println("Client closed");
        }
    }

    private void ErrorMethod(String errorAlert) {
        //UI for Error Messages to inform User.
        JFrame error = new JFrame();
        error.setTitle(errorAlert);
        error.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        error.setSize(500, 200);
        error.setLocationRelativeTo(clientPanel);
        JTextArea errorMessage = new JTextArea();
        errorMessage.append(errorAlert);
        JButton exit = new JButton("Exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(-1);
            }
        });
        error.add(errorMessage);
        error.add(exit, BorderLayout.SOUTH);
        error.setVisible(true);
    }

    private void startConversation(String otherClient) {
        ClientConversationManager newConversation = new ClientConversationManager(myUserName ,otherClient, clientSocket);
        conversations.add(newConversation);
        Thread t = new Thread(newConversation);
        t.start();
    }

    private void incomingConversation(String otherClient, MessagePacket messageIn) {
        ClientConversationManager newConversation = new ClientConversationManager(myUserName ,otherClient, clientSocket, messageIn);
        conversations.add(newConversation);
        Thread t = new Thread(newConversation);
        t.start();
    }

    private void updateClientList() {
        ArrayList temp = myUserSession.getUsersList();
        Set<String> temp2 = new HashSet<>();
        for (int i = 0; i < temp.size(); i++) {

                clientListModel.addElement((String) temp.get(i));
                System.out.println((String) temp.get(i));
            }
        }

    private void messageAll(String actionCommand) {
        DataOutputStream out = new DataOutputStream(outputStream);
        Writer writer = new OutputStreamWriter(out);
        try {
            writer.write(actionCommand + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String userName, String message, String sendTo, int packetHeader) throws IOException {
        /* Sends Message to Server to be re routed to proper client.
         PacketHeader sends integers 1 or 2
         1 means message is for server
         2 means message is for another client
         Server will handle request accordingly
        */
        ObjectOutputStream objOut = new ObjectOutputStream(clientSocket.getOutputStream());
        MessagePacket newMessage = new MessagePacket(message, userName, sendTo, packetHeader);
        objOut.writeObject(newMessage);
        objOut.flush();
    }

    public void handleMessagePacket(MessagePacket messageIn) {
        /*
        routes the messages to the reciever via
        the packetHeader integer
        case 1, message is from Server
        case 2, message is from other client.
        */
        switch (messageIn.getPacketHeader()) {
            case 1: {
                if (messageIn.getActiveMessage() == false) {
                    messageIn.setActiveMessage(true);
                    setMyUserName(messageIn.getSendTo());
                }
                newMessageFrame.setVisible(true);
                newMessageDiag.append(messageIn.getSender() + ": " + messageIn.getMessage() + "\n");
                break;
            }
            case 2: {
                handleConversation(messageIn);
                break;
            }
        }
    }

    public void handleConversation(MessagePacket newMessage) {
        /*
         Handles conversations, routing messages to the correct pre-existing conversations between clients
         */
        System.out.println("Inside handleConversation");
        System.out.println(conversations.size());
        if (conversations.size() > 0) {
            for (ClientConversationManager convos : conversations) {
                System.out.println(convos.getActiveConversation());
                if (convos.getOtherClient().equals(newMessage.getSender())) {
                    if (convos.getActiveConversation()) {
                        System.out.println(newMessage.getActiveMessage());
                        convos.addMessage(newMessage.getMessage(), newMessage.getSender());
                    }
                }
            }
        } else {
           incomingConversation(newMessage.getSender(), newMessage);
        }
    }

    public void setMyUserName(String name) { this.myUserName = name; }
    public String getMyUserName() { return this.myUserName; }

    private void setMyUserSession(UserSessionManager userSession) { this.myUserSession = userSession; }
    public UserSessionManager getMyUserSession() { return this.myUserSession; }

    public static void main(String[] args) throws IOException {
        try {
            Client newClient = new Client("localhost");
        } catch (ConnectException ex) {
            System.out.println("SERVER NOT FOUND");
            System.exit(-1);
        }
    }
}