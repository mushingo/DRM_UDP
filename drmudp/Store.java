package drmudp;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.Paths;
import java.nio.file.Files;

/* The Store performs intermediation for client processes so they can request 
** lists of items held by the store and facilitate the buying of those items
** by the clients by checking financial info  with the Bank server and 
** retrieving content from the Content Server.
**
** 
** The Store Server takes three command line arguments. The first is the 
** port the server is to listen to for incoming connections. The third is the 
** nameServer port which is used to register the Store Server's 
** ip/port/hostname details. The second is the path to a file containing details
** about the store stock.
** 
*/
public class Store {
  //Exit Status codes
  private static final int BAD_ARGS = 1;
  private static final int SOCKET_FAILURE = 4;
  private static final int REGISTRATION_FAILURE = 2;  
  private static final int LOOKUP_FAILURE  = 5;  
  private static final int NAMESERVER_CONNECT_FAIL  = 6;

  //Instance variables  
  private ServerMap servers = null; //map of servers 
  private DatagramSocket serverSocket = null; 
  InetAddress clientIPAddress;
  int clientPort;
  int stockPort;
  
  private String message; //message read from remote client process

  private Stock stock; //The stock content info read from file
  
  /* Creates a new Store Object using the command line arguments.
  **
  ** @param args The arguments supplied on the command line. This should be a 
  ** a port for the for the server to listen for connections, the stock file 
  ** path and the NameServer port.
  */    
  public static void main (String[] args) {
    new Store(args);
  }
  
  /* Creates a new Store Object which registers its details with the nameServer
  ** and loads up and stores the stock details from the supplied file. The
  ** server then looks up the Bank and Content server details and connects to
  ** these servers then starts listening for new connections from clients and 
  ** connects and replies to incoming messages. 
  **
  ** @param args The arguments supplied on the command line. This should be a 
  ** a port for the for the server to listen for connections, the stock file 
  ** path and the NameServer port.
  */  
  public Store(String[] args) {
    int nameServerPort;
    
    if (args.length != 3) {
      exit(BAD_ARGS);  
    }
    
    if ((stockPort = check_valid_port(args[0])) < 0) {
      exit(BAD_ARGS);  
    }
    
    if ((nameServerPort = check_valid_port(args[2])) < 0) {
      exit(BAD_ARGS);  
    }
    
    String currentDir = new File("").getAbsolutePath();
    String path = currentDir + ComsFormat.fileSep + args[1];  
    
    try {
      stock = new Stock(Files.readAllLines(Paths.get(path))); 
      
    } catch (IOException e) {
      System.out.println("Could not find \""  + args[1] 
          + "\" in directory: " + currentDir);
      exit(BAD_ARGS);
    }
    
    listen(stockPort);
    System.err.print("Store waiting for incoming messages\n");
    
    try {
      servers = new ServerMap(nameServerPort);
      servers.register(ComsFormat.store_hostname, stockPort
          , ComsFormat.store_ip);
    } catch ( RegistrationException e){
      exit(REGISTRATION_FAILURE);
    }
    
    try {
      servers.add_server(ComsFormat.content_hostname);
      servers.add_server(ComsFormat.bank_hostname);
    } catch (LookupException e){
      System.err.print(e.getMessage() + ComsFormat.separator 
          + "has not registered\n");
      exit(LOOKUP_FAILURE);
    } catch (NameServerContactException e){
      System.err.print("Could not contact NameServer\n");
      exit(NAMESERVER_CONNECT_FAIL);
    }    
	
	//Processes messages
        while (true) {
          try {
            get_message();
          } catch (IOException e) {
            continue;
          }
          process_message();
      }
  
  }
  
  /*  Create a new datagram socket from which to receive datagrams from clients.
  ** @port the port to listen on
  */   
  private void listen(int port) {
    try {
      serverSocket = new DatagramSocket(port);
    } catch (SocketException e) {
      exit(SOCKET_FAILURE);
    }
  }

  /* Read in a datagram from a client. Extract IP, Port and message.
  */
  private void get_message() throws IOException {
    byte[] receiveData = new byte[1024];
    int dataLength = receiveData.length;
    DatagramPacket receivePacket = new DatagramPacket(receiveData, dataLength); 
    
    serverSocket.receive(receivePacket);         
    
    String line = new String(receivePacket.getData());
    line = line.trim();
    System.out.println("Message from Client: " + line);
    clientIPAddress = receivePacket.getAddress();
    clientPort = receivePacket.getPort();
    
    message = line;

  }
  
  
  /* If the message is a valid list request the Store replies with the list, if 
  ** it is a valid buy request the store processes the buy request and sends the
  ** result, otherwise the message is ignored.
  */
  private void process_message() {
    if (message.equals(ComsFormat.listRequest)){
      send_list();
    } 
      
    String[] messageParts = message.split(ComsFormat.separator);
      
    if (messageParts.length == 3 
      && (messageParts[0].equals(ComsFormat.buyRequest))) {       
      process_buy_request(messageParts);
    }

    return;
  }
  
  /* Send a formatted Stock list to the client processes connected to store.
  */
  private void send_list() {
    reply(ComsFormat.listStart + ComsFormat.newline + stock.toString() 
        + ComsFormat.newline + ComsFormat.listEnd);
  }
  
  /* Send a datagram to the client as a reply.
   * 
  ** @param reply the message to reply with
  */
  private void reply(String reply) {
    byte[] sendData = new byte[1024];
    sendData = reply.getBytes();
        
    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
        clientIPAddress, clientPort);
    
    try {
      serverSocket.send(sendPacket);
      System.out.println("Message to Client:" + reply + ComsFormat.newline);
    } catch (IOException e) {
      System.err.println("Unable to send reply");
      return;
    }
    
  }
  
  /* Extract item requested to buy and check it's valid. Check financial info
  ** with Bank Server, attempt to retrieve content from Content Server. If any 
  ** of the checks or attempts fail reply to client processes with a transaction 
  ** fail message. Otherwise if everything succeeds send the content to client.
  **
  ** @param messageParts the buy message received broken into an array of words
  */
  private void process_buy_request(String[] messageParts) {
    long creditCard = 0;
    long itemId = 0;
    float itemPrice = 0;
    String bankMsg = "";
    String bankReply = "";
    String contentMsg = "";
    String content = "";
      
    try {
      creditCard = Long.parseLong(messageParts[1]);
      itemId = Long.parseLong(messageParts[2]);
      itemPrice = stock.get_price(itemId);
    } catch (NumberFormatException e) {
      transaction_fail(itemId);
      return;
    } 
    if (itemPrice < 0) {
      transaction_fail(itemId);
      return;
    }
        
    bankMsg = itemId + ComsFormat.separator + itemPrice + ComsFormat.separator 
	    + creditCard;
    
    try {
      System.out.println("Message to bank: " + bankMsg);
      bankReply = servers.get_server(ComsFormat.bank_hostname).
          send_message(bankMsg, ComsFormat.retry, ComsFormat.sendTimeout, 
          ComsFormat.receiveTimeout);
      System.out.println("Message from bank: " + bankReply);    
    } catch (IOException e) {
        transaction_fail(itemId);
    }
      
    contentMsg = ComsFormat.request_content + ComsFormat.separator + itemId;
      
    if (bankReply.equals(ComsFormat.purchase_success)) {
      content = get_content(contentMsg);
      System.out.println(content);
      if (content.equals("")) {
        transaction_fail(itemId);
        return;
      }
      reply(content);
    } else if (bankReply.equals(ComsFormat.purchase_fail)) {
      transaction_fail(itemId);
    }
  }  
  
  /* Attempt to retrieve content from Content server for a given item. If the 
  ** attempt fails return an empty string as the content.
  **
  ** @param message the message used to request content item
  ** @return the content, blank if attempt to retive fails
  */
  private String get_content(String contentMsg) {
    String contentReply = "";
    try {     
      System.out.println("Message to content: " + contentMsg);
      contentReply = servers.get_server(ComsFormat.content_hostname).
          send_message(contentMsg, ComsFormat.retry, 
          ComsFormat.sendTimeout, ComsFormat.receiveTimeout); 
      System.out.println("Message from content: " + contentReply);  
    } catch (IOException e) {
      return contentReply;
    }
    return contentReply;
    
  }
  
  /* Send a message indicating that the item buy request has failed. 
  **
  ** @param itemId the ID of the item that the buy attempted failed on
  */  
  private void transaction_fail(long itemId) {
    reply(itemId + ComsFormat.separator + ComsFormat.transaction_fail);
  }
  
  /* Checks that the supplied port is a number within the valid port range 
  ** > 0 < 65535 and if so returns an int representing that port. 
  ** Otherwise returns -1.
  **
  ** @param porArg the argument to check and convert
  ** @return The port if it's within the valid range, -1 otherwise
  */   
  private int check_valid_port(String portArg) { 
    
    try {
      Integer.parseInt(portArg);
    } catch (NumberFormatException e) {
      return -1;    
    }
    int port = Integer.parseInt(portArg);
    
    if (port < 1 || port > 65535) {
      return -1;
    }
    
    return port;
  }
  
  /* Exit the Store server with the appropriate error message and status.
  **
  ** @param status the exit status to exit with
  */    
  private void exit(int status) {
    switch (status) {
      case BAD_ARGS: 
        System.err.print("Invalid command line arguments for Store\n");
        System.exit(BAD_ARGS);
      case REGISTRATION_FAILURE: 
        System.err.print("Registration with NameServer failed\n");
        System.exit(REGISTRATION_FAILURE);
      case LOOKUP_FAILURE:
        System.exit(LOOKUP_FAILURE);
      case SOCKET_FAILURE:
        System.err.print("Socket couldn't be opened or could not bind to port: "
            + stockPort + "\n");
        System.exit(SOCKET_FAILURE);
    }
  }
}

/* A class used to store the stock information read from the stock file.
*/
class Stock {
  //A map of the itemId to the content. Tree map is used to preserve ordering
  private TreeMap<Long, Float> stockMap;
  
  public Stock(List<String> stock) {
    stockMap = new TreeMap<Long, Float>();
    for (int i = 0; i < stock.size(); i++) {
      String stockParts[] = stock.get(i).split(ComsFormat.separator);
      long itemId = Long.parseLong(stockParts[0]);
      float itemPrice = Float.parseFloat(stockParts[1]);
      stockMap.put(itemId, itemPrice);
    }
  }
  
  /* Return the price of the given stock item.
  **
  ** @param itemId the ID of the stock item to look up
  ** @return the price of the given stock item
  */ 
  public float get_price (long itemId) {
    float noItem = -1;
    return stockMap.getOrDefault(itemId, noItem);
  }
  
  /* Return the string representation of the entire stock.
  **
  ** @return the string representation of the entire stock.
  */  
  public String toString () {
    String string = "";
    for (Long itemId: stockMap.keySet()) {
      string = string + itemId + " " + stockMap.get(itemId) 
	      + ComsFormat.newline;
    }
    return string;
  }
}





