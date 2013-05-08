
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
 * -output to logfile
 */
public class Server {
	private final static Logger LOGGER = Logger.getLogger(Server.class .getName());
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	
	private static int connectionId = 0;
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> connectedList;
	private double version = 2.00;
	/****************************************************
	 * v2.0 
	 * new protocol
	 * bug fixes and optimizations
	 * new commands
	 ****************************************************/
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
		connectedList = new ArrayList<ClientThread>();
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
		for(int i = 0; i < connectedList.size(); ++i) {
			ClientThread tc = connectedList.get(i);
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
					
					ClientThread t = new ClientThread(socket);  // make a thread of it
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
	private synchronized void broadcast(String message) {
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = connectedList.size(); --i >= 0;) {
			ClientThread ct = connectedList.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.send(message)) {
				connectedList.remove(i);
			}
		}
	}	 
	private synchronized void broadcastExceptFor(int id, String message) {
		for(int i = connectedList.size(); --i >= 0;) {
			if(connectedList.get(i).id != id){
				ClientThread ct = connectedList.get(i);
				// try to write to the Client if it fails remove it from the list
				if(!ct.send(message)) {
					connectedList.remove(i);
				}
			}
			
		}
	}
	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < connectedList.size(); ++i) {
			ClientThread ct = connectedList.get(i);
			// found it
			if(ct.id == id) {
				connectedList.remove(i);
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
		BufferedReader in;
		PrintWriter out;
		// my unique id (easier for disconnection)
		int id;
		// the Username of the Client
		String username = "";
		// the only type of message a will receive
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
				out = new PrintWriter(socket.getOutputStream(), true);
				in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.username = in.readLine();
				if(!validate(username, out)){
					remove(id);
					close();
					return;
				}
				
				log(Level.INFO,"connection accepted from " + socket.getInetAddress());
				this.start();
			}
			catch (IOException e) {
				log(Level.SEVERE,"Exception creating new Input/output Streams: " + e);
				return;
			}
		}
		// run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			//broadcast(username + " connected");
			while(keepGoing) {
				// read a String (which is an object)
				/*TODO: FUNCTify */
				try {
					String line = in.readLine();
					if(line == null) continue;
					System.out.println(line);
					switch(line.charAt(0)){
						case '0': // message
							broadcast(username + ": " + line.substring(1));
							break;
						case '1':
							keepGoing = false;
							//logout;
							break;
							/*
							 * commands flag to method to clean this up
							 */
						default:
							
							send("command not understood: " + line);
							break;
					}
					
					
				}
				catch (IOException e) {
					log(Level.SEVERE,username + " Exception reading Streams: " + e);
					break;				
				}
			}
			// remove self from the arrayList containing the list of the
			// connected Clients
			broadcastExceptFor(id, username + " disconnected");
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
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

		/*
		 * Write a String to the Client output stream
		 */
		private boolean send(String s){
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// add HH:mm:ss
			String time = new SimpleDateFormat("h:mm:ss:").format(new Date());
			out.println(time + s);
			return true;
		}

	}
}