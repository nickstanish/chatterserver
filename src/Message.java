import java.util.Date;

import javax.swing.text.SimpleAttributeSet;


public class Message {

  public enum Type {
    Message, Typing, Contacts, Logout, Command, Error;
  }

  public Type type;
  public ExpandedStyle style;

  public String message, username;
  public String[] contacts;
  public Date sentDate;

  public boolean typing;

  /**
   * Standard Message
   * 
   * @param to: category to receive
   * @param message: string to send
   * @param recipient: null if all
   * @param from: creators username
   * 
   */
  public Message() {
    sentDate = new Date();
  }

  public Message(String message, SimpleAttributeSet style, String[] recipient) {
    this();
    this.message = message;
    this.type = Type.Message;
    this.style = new ExpandedStyle(style);
    this.contacts = recipient;
  }

  /**
   * Typing Message
   * 
   * @param to: group to receive
   * @param typing, true if user is typing
   * @param message: text user is typing if realtime is on, else null
   * @param recipient: null if all
   * @param from: username
   */
  public Message(boolean typing, String message, String[] recipient) {
    this();
    this.message = message;
    this.typing = typing;
    this.type = Type.Typing;
    this.contacts = recipient;
  }

  /**
   * Logout, contacts Request
   * 
   * @param type: Logout, Contacts request
   */
  public Message(Type type) {
    this();
    this.type = type;
  }

  /**
   * Command Message. Simple Message., Contacts return
   * 
   * @param message
   */
  public Message(Type type, String message) {
    this();
    this.type = type;
    this.message = message;
  }
}
