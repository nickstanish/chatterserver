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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.gson.Gson;

public class Server {
  private final static Logger LOGGER = Logger.getLogger(Server.class.getName());
  private static FileHandler fileTxt;
  private static SimpleFormatter formatterTxt;
  private int port;
  private static int connectionId = 0;
  private Map<String, Client> clientsList;
  private double version = 2.02;
  private final static String USAGE = "ChatterServer: \n\tUsage is: $> java Server [portNumber]\n";
  /****************************************************
   * v2.02 efficiency and housekeeping
   ****************************************************/
  public volatile boolean keepGoing;

  public Server(int port) {
    shutdownhook();
    this.port = port;
    this.clientsList = new HashMap<String, Client>();
  }

  /**
   * Validates username currently just checks to see if user is already taken
   * 
   * @param username
   * @param out
   * @return
   */
  private boolean validateUser(String username, PrintWriter out) {
    if (clientsList.containsKey(username)) {
      out.println("0 username taken");
      return false;
    }
    out.println("1");
    return true;
  }

  /**
   * 
   * do stuff before exiting
   */
  private void shutdownhook() {
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        log(Level.INFO, "Please wait... cleaning up");
        close();
        try {
          mainThread.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        log(Level.INFO, "Done.");
      }
    });
  }

  public void close() {
    for (Client client : clientsList.values()) {
      try {
        client.in.close();
        client.out.close();
        client.socket.close();
      } catch (IOException e) {
        log(Level.SEVERE, e.toString());
      }
    }
    log(Level.INFO, "Closing");
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
      // 10 second time-out on blocking during accept
      serverSocket.setSoTimeout(10000);
      // infinite loop to wait for connections
      log(Level.INFO, "Server waiting for Clients on port " + port + ".");
      while (keepGoing) {
        Socket socket;
        try {
          socket = serverSocket.accept(); // block and wait to accept a connection
        } catch (SocketTimeoutException ste) {
          socket = null;
        }
        if (socket != null) {
          try {
            socket.setKeepAlive(true);
          } catch (SocketException e) {
            log(Level.SEVERE, e.toString());
            log(Level.SEVERE, "Unable to set keepalive true");
          }
          // if I was asked to stop
          if (!keepGoing)
            break;
          try {
            Client client = new Client(socket); // make a thread of it
            clientsList.put(client.username, client);
          } catch (Exception e) {
            log(Level.WARNING, "Could not connect user");
          }

        }
      }
      // I was asked to stop
      try {
        serverSocket.close();
        close();
      } catch (Exception e) {
        log(Level.SEVERE, "Exception closing the server and clients: " + e);
      }
    }
    // something went bad
    catch (IOException e) {
      log(Level.SEVERE, "Exception on new ServerSocket: " + e + "\n");
    }
    log(Level.OFF, "Terminated\n");
  }

  /**
   * log errors/messages
   */
  private static void initLog() {
    File dir = new File("logs/");
    LOGGER.setUseParentHandlers(false);
    LOGGER.setLevel(Level.ALL);
    try {
      dir.mkdirs();
      fileTxt = new FileHandler("logs/server.log", 0, 1, true);
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
    LOGGER.log(level, msg);
  }

  /*
   * > java Server > java Server [portNumber] If the port number is not specified 1500 is used
   */
  public static void main(String[] args) {
    // start server on default port 1500 unless a PortNumber is specified
    int portNumber = 1500;
    initLog();
    switch (args.length) {
      case 1:
        try {
          portNumber = Integer.parseInt(args[0]);
        } catch (Exception e) {
          log(Level.INFO, "Invalid port number.");
          log(Level.INFO, "Usage is: > java Server [portNumber]");
          return;
        }
      case 0:
        break;
      default:
        log(Level.INFO, "Usage is: > java Server [portNumber]");
        return;
    }
    // create a server object and start it
    Server server = new Server(portNumber);
    server.start();

  }

  /**
   * pretty much does all the work in here
   */
  class Client extends Thread {
    // the socket where to listen/talk
    boolean keepGoing = true;
    private Gson gson = new Gson();
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

    // ArrayList<Client> recents = new ArrayList<Client>();

    public Client(Socket socket) throws Exception {
      // give each a unique id
      this.date = new Date();
      this.socket = socket;
      try {
        // create output first
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.username = in.readLine();
        if (!validateUser(username, out)) {
          disconnect();
          throw new Exception("Username taken");
        }
        this.id = connectionId++;
        log(Level.INFO, "connection accepted from " + socket.getInetAddress());
        this.start();
      } catch (IOException e) {
        log(Level.SEVERE, "Exception creating new Input/output Streams: " + e);
        return;
      }
    }

    /**
     * Creates a contacts list message
     * 
     * @return
     */
    public Message createContactsMessage() {
      String csv = ""; // comma separated values
      for (String username : clientsList.keySet()) {
        csv += username + ",";
      }
      username.replaceFirst(",$", ""); // remove last comma
      return new Message(Message.Type.Contacts, csv);
    }

    private void messageLoop(Message message, String line) {
      switch (message.type) {
        case Message: // message
        case Typing: // typing just relays to client to be processed
          if (message.contacts == null) {
            // broadcast
            for (Client c : clientsList.values()) {
              c.send(message);
            }

          } else {
            for (String to : message.contacts) {
              sendTo(message, to);
            }
          }

          break;
        case Logout: // logout;
          keepGoing = false;
          break;
        case Command:
          /*
           * for later use
           */
          break;
        case Contacts: // contacts
          send(createContactsMessage());
          break;
        default:
          send(new Message(Message.Type.Error, "command not understood: " + line));
          break;
      }
    }

    @Override
    public void run() {
      // broadcastExceptFor(true, id, '0', username + " connected");
      while (keepGoing) {
        try {
          String line = in.readLine(); // blocking
          if (line == null)
            continue;
          Message message = gson.fromJson(line, Message.class);
          // sign message so usernames can't be spoofed by client
          message.username = username;
          messageLoop(message, line);

        } catch (IOException e) {
          log(Level.SEVERE, username + " Exception reading Streams: " + e);
          break;
        }
      }
      disconnect();
    }

    private void remove(Client c) {
      synchronized (clientsList) {
        clientsList.remove(c.username);
      }
    }

    private void sendTo(Message message, String to) {
      Client client = null;
      synchronized (clientsList) {
        client = clientsList.get(to);
      }
      if (client != null) {
        // try to write to the Client if it fails remove it from the list
        if (!client.send(message)) {
          client.disconnect();
        }
      } else {
        send(new Message(Message.Type.Error, "User not found"));
      }

    }

    /**
     * Try to close everything related to this client
     */
    private void disconnect() {
      remove(this);
      try {
        if (socket != null)
          socket.setKeepAlive(false);
      } catch (SocketException e) {
        log(Level.SEVERE, e.toString());
        log(Level.SEVERE, "Unable to set keepalive false");
      }
      // try to close the connection
      try {
        if (out != null)
          out.close();
      } catch (Exception e) {
      }
      try {
        if (in != null)
          in.close();
      } catch (Exception e) {
      };
      try {
        if (socket != null)
          socket.close();
      } catch (Exception e) {
      }
    }

    /**
     * Write a String to the Client output stream
     */
    private boolean send(Message message) {
      if (!socket.isConnected()) {
        disconnect();
        return false;
      }
      out.println(gson.toJson(message));
      return true;
    }

  }
}
