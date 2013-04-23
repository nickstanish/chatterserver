
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
/*
 * server to run in background
 * -output to logfile
 */
public class Server {
	private final static Logger LOGGER = Logger.getLogger(Server.class .getName());
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	
	private static int connectionId;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	private double version = 1.11;
	// to display time
	private int port;
	// the boolean that will be turned of to stop the server
	public volatile boolean keepGoing;

	/*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
	 */
	public Server(int port) {
		shutdownhook();
		this.port = port;
		// ArrayList for the Client list
		al = new ArrayList<ClientThread>();
	}
	/**
	 * 
	 * do stuff before exiting
	 */
	private void shutdownhook(){
		final Thread mainThread = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run(){
				log(Level.INFO,"Begin Shutdownhook...breaking connections, please allow 10 seconds for timeout");
				close();
				try {
					mainThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				log(Level.INFO,"Done.");
			}
		});
	}
	public void close(){
		
		for(int i = al.size(); --i >= 0;) {
			al.remove(i);
		}
		log(Level.INFO,"Closing");
		keepGoing = false;
	}
	public void start() {
		keepGoing = true;
		LOGGER.log(Level.INFO, "*************************************\n");
		LOGGER.log(Level.INFO, "Starting ChatterBox Server " + version);
		/* create socket server and wait for connection requests */
		try {
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
			//10 second time-out on blocking during accept
			serverSocket.setSoTimeout(10000);
			// infinite loop to wait for connections
			log(Level.INFO,"Server waiting for Clients on port " + port + ".");
			while(keepGoing) {
				
				Socket socket;
				try{
					socket = serverSocket.accept();  	// accept connection
				}
				catch(SocketTimeoutException ste){
					socket = null;
				}
				if(socket != null){
					try{
						socket.setKeepAlive(true);
					}
					catch(SocketException e){
						log(Level.SEVERE,e.toString());
						log(Level.SEVERE,"Unable to set keepalive true");
					}
					// if I was asked to stop
					if(!keepGoing)
						break;
					ClientThread t = new ClientThread(socket);  // make a thread of it
					al.add(t);									// save it in the ArrayList
					t.start();
				}
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
						log(Level.SEVERE,ioE.toString());
					}
				}
			}
			catch(Exception e) {
				log(Level.SEVERE,"Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
			log(Level.SEVERE,"Exception on new ServerSocket: " + e + "\n");
		}
		log(Level.OFF,"Terminated\n");
	}		
	/**
	 * log errors/messages
	 */
	private static void initLog() {
		File dir = new File("logs/");		
		LOGGER.setLevel(Level.ALL);
		try {
			dir.mkdirs();
			fileTxt = new FileHandler("logs/server.log",0,1,true);
			formatterTxt = new SimpleFormatter();
			fileTxt.setFormatter(formatterTxt);
			LOGGER.addHandler(fileTxt);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void log(Level level, String msg) {
		LOGGER.log(level,msg);
	}
	/**
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss and \n to the message
		String time = new SimpleDateFormat("h:mm:ss:").format(new Date());
		String messageOut = time + " " + message + "\n";
		//display(message);
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(new ChatMessage(ChatMessage.Type.MESSAGE, messageOut))) {
				al.remove(i);
			}
		}
	}	 
	private synchronized void broadcastExceptFor(int id, ChatMessage cm) {
		for(int i = al.size(); --i >= 0;) {
			if(al.get(i).id != id){
				ClientThread ct = al.get(i);
				// try to write to the Client if it fails remove it from the list
				if(!ct.writeMsg(cm)) {
					al.remove(i);
				}
			}
			
		}
	}
	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	
	/*
	 *  To run as a console application just open a console window and: 
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	public static void main(String[] args) {
		// start server on default port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		initLog();
		switch(args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					log(Level.INFO,"Invalid port number.");
					log(Level.INFO,"Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				log(Level.INFO,"Usage is: > java Server [portNumber]");
				return;
		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
		
	}
	/*
	 * pretty much does all the work in here
	 */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// my unique id (easier for disconnection)
		int id;
		// the Username of the Client
		String username;
		// the only type of message a will receive
		ChatMessage cm;
		// the date I connect
		Date date;
		SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss");

		// Constructor
		ClientThread(Socket socket) {
			// give each a unique id
			id = ++connectionId;
			this.socket = socket;
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				log(Level.INFO,username + " just connected at " + socket.getInetAddress());
				
			}
			catch (IOException e) {
				log(Level.SEVERE,"Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) {
				//required to make java happy
			}
            date = new Date();
		}
		// run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			broadcast(username + " connected");
			while(keepGoing) {
				// read a String (which is an object)
				try {
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					log(Level.SEVERE,username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the messaage part of the ChatMessage
				String message = cm.getMessage();
				// Switch on the type of message receive
				switch(cm.getType()) {

				case MESSAGE:
					broadcast(username + ": " + message);
					break;
				case LOGOUT:
					//display(username + " disconnected with a LOGOUT message.");
					keepGoing = false;
					break;
				case TYPING:
					broadcastExceptFor(id, cm);
					break;
				case WHOISIN:
					writeMsg(new ChatMessage(ChatMessage.Type.MESSAGE, "List of the users connected at " + dateFormat.format(new Date()) + "\n"));
					// scan al the users connected
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg(new ChatMessage(ChatMessage.Type.MESSAGE,(i+1) + ") " + ct.username + " online for " + (int)((new Date().getTime() - ct.date.getTime())/1000) + " seconds \n"));
					}
					break;
				}
			}
			// remove self from the arrayList containing the list of the
			// connected Clients
			broadcastExceptFor(id, new ChatMessage(ChatMessage.Type.MESSAGE, username + " disconnected"));
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
			try{
				socket.setKeepAlive(false);
			}
			catch(SocketException e){
				log(Level.SEVERE,e.toString());
				log(Level.SEVERE,"Unable to set keepalive false");
			}
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(ChatMessage message) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(message);
			}
			catch(IOException e) {
				log(Level.SEVERE,"Error sending message to " + username);
				log(Level.SEVERE,e.toString());
				return false;
			}
			return true;
		}
	}
}