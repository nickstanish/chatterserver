// TODO: clean this up

import java.io.Serializable;
import java.util.Date;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 * if we were to switch to a different language for our clients then we would have to take
 * the time to send a stream of bytes
 */
public class ChatMessage implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	// The different types of message sent by the Client
	// WHOISIN to receive the list of the users connected
	// MESSAGE an ordinary message
	// LOGOUT to disconnect from the Server
	// TYPING to display is typing message on other client
	public static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, TYPING = 3, PEER = 4;
	private int type;
	private String peerhost, peerport, peerlocalport, peerlocalhost;
	private Date sent;
	private String message, to, from;
	private boolean isTyping;
	private String username;
	
	// constructor
	public ChatMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
	public ChatMessage(String to, int type, String message, String from){
		sent = new Date();
		this.to = to;
		this.from = from;
		this.message = message;
		this.type = type;
	}
	public ChatMessage(int type, boolean isTyping, String message) {
		this.type = type;
		this.isTyping = isTyping;
		this.message = message;
	}
	public ChatMessage(int type, String host, String port, String lhost, String lport) {
		this.type = type;
		this.peerhost = host;
		this.peerport = port;
		this.peerlocalhost = lhost;
		this.peerlocalport = lport;
	}
	// getters
	public int getType() {
		return type;
	}
	public String[] getPeerConnection(){
		return new String[]{peerhost, peerport, peerlocalhost, peerlocalport};
	}
	public Date getSentDate(){
		return sent;
	}
	public String getMessage() {
		return message;
	}
	public boolean getTyping() {
		return isTyping;
	}
	public String getUsername(){
		return this.username;
	}
	public String getTo(){
		return to;
	}
	public String getFrom(){
		return from;
	}
	public void setUsername(String username){
		this.username = username;
	}
}

