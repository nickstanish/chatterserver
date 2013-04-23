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

	/**
	 * 
	 */
	private static final long serialVersionUID = -5441462942685503565L;
	
	public enum Type{
		WHOISIN, MESSAGE, LOGOUT, TYPING, PEER
	}
	private Type type;
	private Date sent;
	private String message, to, from;
	private boolean isTyping;
	
	public ChatMessage(Type type, String message) {
		sent = new Date();
		this.type = type;
		this.message = message;
	}
	public ChatMessage(String to, Type type, String message, String from){
		sent = new Date();
		this.to = to;
		this.from = from;
		this.message = message;
		this.type = type;
	}
	public ChatMessage(Type type, boolean isTyping, String message, String from) {
		this.type = type;
		this.isTyping = isTyping;
		this.message = message;
		this.from = from;
	}
	// getters
	public Type getType() {
		return type;
	}
	public Date getSent(){
		return sent;
	}
	public String getMessage() {
		return message;
	}
	public boolean getTyping() {
		return isTyping;
	}
	public String getTo(){
		return to;
	}
	public String getFrom(){
		return from;
	}
}

