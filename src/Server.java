
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
 * output to logfile
 */
public class Server {
	private final static Logger LOGGER = Logger.getLogger(Server.class .getName());
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	private int port;
	private static int connectionId = 0;
	// List of clients
	private ArrayList<Client> connectedList;
	private double version = 2.00;
	/****************************************************
	 * v2.0 
	 * new protocol
	 * bug fixes and optimizations
	 * new commands
	 * cleanup
	 ****************************************************/
	public volatile boolean keepGoing;

	/*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
	 */
	public Server(int port) {
		shutdownhook();
		this.port = port;
		// ArrayList for the Client list
		connectedList = new ArrayList<Client>();
	}
	private boolean validate(String s, PrintWriter out){
		/*
		 * check if username is taken
		 */
		for(int i = 0; i < connectedList.size(); i++){
			if(s.equalsIgnoreCase(connectedList.get(i).username)){
				out.println("0 username taken");
				return false;
			}
		}
		out.println("1");
		return true;
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
		for(int i = 0; i < connectedList.size(); i++) {
			Client tc = connectedList.get(i);
			try {
				tc.in.close();
				tc.out.close();
				tc.socket.close();
			}
			catch(IOException ioE) {
				log(Level.SEVERE,ioE.toString());
			}
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
					
					Client t = new Client(socket);  // make a thread of it
					connectedList.add(t);						// save it in the ArrayList
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
			// I was asked to stop
			try {
				serverSocket.close();
				close();
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
	private void broadcast(boolean b, char code, String message) {
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		synchronized(connectedList){
			for(int i = connectedList.size() - 1; i >= 0; i--){
				Client ct = connectedList.get(i);
				// try to write to the Client if it fails remove it from the list
				if(!ct.send(b, code, message)) {
					connectedList.remove(i);
				}
			}
		}
		
	}	 
	private void broadcastExceptFor(boolean b, int id, char code, String message) {
		synchronized(connectedList){
			for(int i = connectedList.size() - 1; i >= 0; i--) {
			if(connectedList.get(i).id != id){
				Client ct = connectedList.get(i);
				// try to write to the Client if it fails remove it from the list
				if(!ct.send(b, code, message)) {
					connectedList.remove(i);
				}
			}
			
			}
		}
		
	}
	// for a client who logoff using the LOGOUT message
	private void remove(int id) {
		// scan the array list until we found the Id
		synchronized(connectedList){
			for(int i = 0; i < connectedList.size(); i++) {
			Client ct = connectedList.get(i);
			// found it
			if(ct.id == id) {
				connectedList.remove(i);
				return;
			}
		}
		}
		
	}
	private void remove(Client c) {
		// scan the array list until we found the Id
		synchronized(connectedList){
			for(int i = 0; i < connectedList.size(); i++) {
			Client ct = connectedList.get(i);
			// found it
			if(ct == c) {
				connectedList.remove(i);
				return;
			}
		}
		}
		
	}
	/*
	 * > java Server
	 * > java Server [portNumber]
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
	public static String booleanValue(boolean b){
		if (b) return "on";
		else   return "off";
	}
	public static String keyUpdatify(String message, String s) {
		if(s.charAt(0) == '+'){
			message += s.substring(1);
		}
		else if(s.charAt(0) == '-'){
			try{
				int remove = Integer.parseInt(s.substring(1));
				if(message.length() >= remove){
					message  = message.substring(0, message.length() - remove);
				}
				else{
					message = "";
				}
			}
			catch(NumberFormatException nfr){
				log(Level.SEVERE, nfr.toString());
			}
		}
		return message;
	}
	/**
	 * pretty much does all the work in here
	 */
	class Client extends Thread {
		// the socket where to listen/talk
		boolean keepGoing = true;
		Socket socket;
		BufferedReader in;
		PrintWriter out;
		Date date;
		String message = "";
		boolean realtime = false;
		// my unique id (easier for disconnection)
		int id;
		// the Username of the Client
		String username = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss");

		public Client(Socket socket) {
			// give each a unique id
			this.date = new Date();
			this.socket = socket;
			try{
				// create output first
				out = new PrintWriter(socket.getOutputStream(), true);
				in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.username = in.readLine();
				if(!validate(username, out)){
					remove(this);
					disconnect();
					return;
				}
				this.id = connectionId++;
				log(Level.INFO,"connection accepted from " + socket.getInetAddress());
				this.start();
			}
			catch (IOException e) {
				log(Level.SEVERE,"Exception creating new Input/output Streams: " + e);
				return;
			}
		}
		// run forever
		public void sendContacts(){
			String csv = ""; //comma separated values
            for(int i = 0; i < connectedList.size(); i++) {
            	csv += connectedList.get(i).username + ",";
            }
			send(false, 'c',csv);
		}
		public void run() {
			//broadcastExceptFor(true, id, '0', username + " connected");
			while(keepGoing) {
				// read a String (which is an object)
				/*TODO: FUNCTify */
				try {
					String line = in.readLine(); //blocking
					if(line == null) continue;
					System.out.println(line);
					switch(line.charAt(0)){
						case '0': // message
							broadcast(true, '0', username + ": " + line.substring(1));
							break;
						case '1': //logout;
							keepGoing = false;
							break;
							/*
							 * commands flag to method to clean this up
							 */
						case '2': // isTyping
							if(line.charAt(1) == '0'){ // done typing
								message = "";
								broadcastExceptFor(false, id, '2', "0");
							}
							else{ //is typing
								if(realtime){
									message = keyUpdatify(message, line.substring(2));
									broadcastExceptFor(false, id, '2', "1" + username + ": " + message);
								}
								else broadcastExceptFor(false, id, '2', "1" + username + " is typing.");
							}
							break;
						case '3': // enable/disable realtime
							realtime = line.charAt(1) == '1';
							send(true, '0', "Realtime " + booleanValue(realtime));
							break;
						case '4': //whoisin
							whoisin();
							break;
						case 'c':
							//contacts
							sendContacts();
							break;
						default:
							send(false, 'E', "command not understood: " + line);
							break;
					}
					
					
				}
				catch (IOException e) {
					log(Level.SEVERE,username + " Exception reading Streams: " + e);
					break;				
				}
			}
			disconnect();
		}

		// try to close everything
		private void disconnect() {
			remove(id);
			try{	
				if(socket != null) socket.setKeepAlive(false);
			}
			catch(SocketException e){
				log(Level.SEVERE,e.toString());
				log(Level.SEVERE,"Unable to set keepalive false");
			}
			// try to close the connection
			try {
				if(out != null) out.close();
			}
			catch(Exception e) {}
			try {
				if(in != null) in.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}
		private void whoisin(){
			send(false, '0', "List of the users connected at " + dateFormat.format(new Date()));
            // scan al the users connected
            for(int i = 0; i < connectedList.size(); i++) {
            	Client c = connectedList.get(i);
                send(false, '0', c.username + " online for " + (int)((new Date().getTime() - c.date.getTime())/1000) + " seconds");
            }
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean send(boolean includeDate, char code, String s){
			if(!socket.isConnected()) {
				disconnect();
				return false;
			}
			// add HH:mm:ss
			String time = "";
			if(includeDate) time = new SimpleDateFormat("h:mm:ss: ").format(new Date());
			out.println(code + time + s);
			return true;
		}

	}
	
}
