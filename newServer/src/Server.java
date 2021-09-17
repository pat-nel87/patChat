/*
Patrick Nelson 2021
Java Multi-threaded Chat Server w/ GUI
*/
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.net.*;

// Server class
public class Server extends JFrame {

    private ArrayList<ClientManager> clientList;
    public ServerSocket serverSocket;
    private int clientNumber;
    private JFrame serverWindow;
    private JTextArea serverDialogue;
    private JTextField serverEntryField;
    private UserSessionManager usersOnline;

    public Server()
    {
        serverWindow = new JFrame();
        serverDialogue = new JTextArea();
        serverEntryField = new JTextField();
        serverEntryField.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        writeToUsers(actionEvent.getActionCommand());
                        serverEntryField.setText("");
                    }
                }
        );
        serverWindow.setTitle("Server Operations");
        serverWindow.add(serverDialogue);
        serverWindow.add(serverEntryField, BorderLayout.SOUTH);
        serverWindow.setSize(500,150);
        serverWindow.setVisible(true);
        serverWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverWindow.setLocationRelativeTo(null);
        serverDialogue.setBackground(Color.BLACK);
        serverDialogue.setForeground(Color.GREEN);
        serverDialogue.setFont(new Font("Monospaced", Font.PLAIN, 12));
        serverEntryField.setBackground(Color.BLACK);
        serverEntryField.setForeground(Color.GREEN);
        serverEntryField.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        serverEntryField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        clientList = new ArrayList<ClientManager>();
        clientNumber = 0;

        try {
            serverSocket = new ServerSocket(8818);
            serverDialogue.append("SERVER IS NOW LISTENING ON PORT 8818 \n");
            usersOnline = new UserSessionManager(new ArrayList<>());
            while (true) {
                Socket connection = null;
                try {
                   connection = serverSocket.accept();
                   serverDialogue.append("Client has connected " + connection.getLocalSocketAddress() + "\n");
                   serverDialogue.append("Client remote socket address :" + connection.getRemoteSocketAddress() + "\n");
                   String userName = "Client" + clientNumber;
                   String message = ("\n You are Client #" + clientNumber);
                   sendMessagePacket(message, "Server", userName, 1, connection);
                   ClientManager clientConnection = new ClientManager(userName, connection, connection.getOutputStream(), connection.getInputStream());
                   Thread t = new Thread(clientConnection);
                   clientList.add(clientConnection);

                   clientConnection.setUserList(usersOnline);
                   t.start();

                   clientNumber++;
                   updateClientLists();
                   connection.getOutputStream().flush();
                } catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessagePacket(String message, String userName, String sendTo, int packetHeader, Socket socket) throws IOException {
        /* creates and sends a messagePacket to desired location  */
        MessagePacket newMessage = new MessagePacket(message, userName, sendTo, packetHeader);
        ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
        objOut.writeObject(newMessage);
        objOut.flush();
    }

    public void updateClientLists() throws IOException {
        /* Method to send updated list of clients online to
        clients every time a new connection is made
        using properties of a HashSet to prevent adding new users
        */

        ArrayList temp = new ArrayList();
        Set<ClientManager> temp2 = new HashSet<>();
        for (ClientManager clients : clientList) {
                if (clients.loggedOn == false){
                    clientList.remove(clients);
                }

                if (temp2.add(clients) == true) {
                    temp.add(clients.userName);
                }
        }
        usersOnline.setUsersList(temp);
        sendClientLists();
    }

    private void sendClientLists() throws IOException {
        //method to update clientLists maintained by clientManagers
        //methods to filter out duplicates don't seem to be working in most cases on the client side
        //I think the issue lies with the way the list is added to the GUI on client program.
        //will revisit.
        for (ClientManager clients : clientList) {
            if (clients.loggedOn == true) {
                clients.setClientsList(clientList);
                try {
                    ObjectOutputStream objOut = new ObjectOutputStream(clients.socket.getOutputStream());
                    objOut.writeObject(usersOnline);
                    objOut.flush();
                } catch (SocketException s) {
                    clientList.remove(clients);
                }
            }
        }
    }

    private void writeToUsers(String actionCommand) {
        /* iterates through clientlist to
     send message to every client on list.
      - will need a if clients.loggedon = true
      to make sure it doesn't send messages to clients who left the chat
     */
        for (ClientManager clients : clientList) {
            Writer writer = new OutputStreamWriter(clients.dataOutputStream);
            try {
                writer.write("\n" + actionCommand + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();

    }
}