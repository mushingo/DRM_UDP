package drmudp;


import java.net.*;
import java.io.*;

/* The Bank is used to check if the financial credentials are valid or invalid
**
** The Bank takes two command line arguments. The first is the port the 
** server is to listen to for incoming connections. The second is the nameServer
** port which is used to register the Bank's ip/port/hostname details.
*/
public class Bank {
  //Exit Status codes
  private static final int BAD_ARGS = 1;
  private static final int LISTEN_FAILURE = 2;
  private static final int SOCKET_FAILURE = 3;
  private static final int REGISTRATION_FAILURE = 4;
  
  //Instance variables
  private DatagramSocket serverSocket = null; //socket to receive datagrams 
  private ServerMap servers = null; 
  
  InetAddress clientIPAddress;
  int clientPort;
  int bankPort;
  
  private String message; //message read from remote client process
  
  
  /* Creates a new Bank Object using the command line arguments.
  **
  ** @param args The arguments supplied on the command line. This should be a 
  ** a port for the for the server to listen for messages and the NameServer
  ** port.
  */
  public static void main (String[] args) {
    new Bank(args);
  }
  
  /* Creates a new Bank Object which registers its details with the nameServer
  ** and then starts listening for new messages and then replies to incoming
  ** messages. 
  **
  ** @param args Command line arguments supplied to constructor and should be a
  ** port for the Bank to listen to and the NameServer port.
  */
  public Bank(String[] args) {
    int bankPort;
    int nameServerPort;
    
    if (args.length != 2) {
      exit(BAD_ARGS);  
    }
    
    if ((bankPort = check_valid_port(args[0])) < 0) {
      exit(LISTEN_FAILURE);  
    }
    
    if ((nameServerPort = check_valid_port(args[1])) < 0) {
      exit(BAD_ARGS);  
    }
    
    listen(bankPort);
    System.err.print("Bank waiting for incoming messages\n");
    
    try {
      servers = new ServerMap(nameServerPort);
      servers.register(ComsFormat.bank_hostname, bankPort, ComsFormat.bank_ip);
    } catch ( RegistrationException e){
      exit(REGISTRATION_FAILURE);
    }
    
   
    //Gets a datagram, processes it, then gets next datagram and repeats
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


  /* Process message from remote client process. If the message is a valid 
  ** financial credential check the bank checks to see if the credentials are 
  ** valid. If they are it replies "1" and prints out "OK" if they are not the 
  ** reply is "0" and "NOT OK" is printed out. If the message is not a valid 
  ** financial credential check the bank does not reply.
  **/
  private void process_message () {
    String[] messageParts = message.split(" ");
    String result;
    long itemId;
    
    if (messageParts.length != 3) {
      return;
    }
    
    try {
      itemId = Long.parseLong(messageParts[0]);
    } catch (NumberFormatException e) {
      return;
    }
	
    System.out.println(itemId);
	
    if ((itemId % 2) == 0){
      result = "1";
      System.out.println("OK");
    } else {
      result = "0";
      System.out.println("NOT OK");
    }
    
    reply(result);
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
      System.out.println("Message to client: " + reply + ComsFormat.newline);
    } catch (IOException e) {
      System.err.println("Unable to send reply");
      return;
    }
    
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
  
  /* Exit the Bank server with the appropriate error message and status.
  **
  ** @param status the exit status to exit with
  */
  private void exit(int status) {
    switch (status) {
      case BAD_ARGS: 
        System.err.print("Invalid command line arguments for Bank\n");
        System.exit(BAD_ARGS);
      case REGISTRATION_FAILURE: 
        System.err.print("Registration with NameServer failed\n");
        System.exit(REGISTRATION_FAILURE);
      case LISTEN_FAILURE:
        System.err.print("Bank unable to listen on given port\n");
      case SOCKET_FAILURE:
        System.err.print("Socket couldn't be opened or could not bind to port: "
            + bankPort + "\n");
        System.exit(SOCKET_FAILURE);
    }
  }
}
