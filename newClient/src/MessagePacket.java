/*
Patrick Nelson 2021
Java Multi-threaded ChatClient w/ GUI
*/

import java.io.Serializable;

public class MessagePacket implements Serializable {
    /* A serializable class for
    sending messages and routing data between client and server programs.
    */
    private String message;
    private String sender;
    private String sendTo;
    private int packetHeader;
    private boolean activeMessage;

    public MessagePacket(String message, String sender, String sendTo, int packetHeader) {
        this.message = message;
        this.sender = sender;
        this.sendTo = sendTo;
        this.packetHeader = packetHeader;
        this.activeMessage = false;
    }
    public String getMessage() { return this.message; }
    public String getSender() { return this.sender; }
    public String getSendTo() { return this.sendTo; }
    public int getPacketHeader() { return this.packetHeader; }
    public boolean getActiveMessage() { return this.activeMessage; }
    public void setActiveMessage(boolean tf) { this.activeMessage = tf; }
}
