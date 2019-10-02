package drmudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/* A class used to store information about a remote Server, connect to a remote
** server and store information about and tools related to the connection.
*/
class Server {
  private String host; //hostname of server
  private String ip; //ip address of server
  private int port; // port of server
  
  /* Create a new Server object. Store information about the server (host, ip,
  ** port and then attempt to connect to the server and open streams into and 
  ** out of the new connection. Store information about these streams.
  **
  ** @param host the server hostname
  ** @param ip the server ip
  ** @param port the server port
  */
  public Server(String host, String ip, int port)  {
    this.host = host;
    this.ip = ip;
    this.port = port;

  }
  
  /*
  ** @param message the message to send
  */
  public String send_message(String message, int retry, int sendTimeout,
      int receiveTimeout) throws IOException {
    byte[] sendData = new byte[1024];
    byte[] receiveData = new byte[1024];
    sendData = message.getBytes();
    String reply = "";
    DatagramSocket serverSocket = null;
    InetAddress serverIP = null;
    boolean sent = false;
    
    try {
      serverSocket = new DatagramSocket(); 
      serverSocket.setSoTimeout(receiveTimeout);
      serverIP = InetAddress.getLocalHost();
    } catch (SocketException | UnknownHostException e) {
      System.out.println(e);
    }
    
    DatagramPacket receivePacket = new DatagramPacket(receiveData, 
        receiveData.length);
    
    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
          serverIP, port);
    
    int i = 1;
    while (i <= retry) {
      sent = false;
      while (!sent) {
        double x = Math.random();
        if (x < ComsFormat.packetLossProb) {
          try { 
            Thread.sleep(sendTimeout);
          } catch (InterruptedException e) {}
          System.out.println("Packet sent to " + host + " lost. Retrying...");
        } else {
          serverSocket.send(sendPacket);
          sent = true;
          break;
        }
      }
      System.out.println("Message sent successfully");
      System.out.println("Message recieve attempt " + i + " of " + retry);
      try {
        serverSocket.receive(receivePacket);
        break;
      }catch (SocketTimeoutException  e) {
        i++;
        if (i > retry) {
          System.out.println("Timed out on recieve. "
              + "Receive attempt failed completly.");
          throw new IOException();
        }
        System.out.println("Timed out on recieve. "
            + "Attempting resend of request.");
      } 

    }
    System.out.println("Message recieved successfully.");
    
    reply = new String(receivePacket.getData());
    reply = reply.trim();
    
    serverSocket.close();
    return reply;     
    
    
  }
  
  /* Return the ip address of server object.
  **
  ** @return the ip address of the server
  */  
  public String get_ip() {
    return this.ip;
  }
   
  /* Return the hostname of server object.
  **
  ** @return the hostname of the server
  */    
  public String get_host() {
    return this.host;
  }
  
  /* Return the port of server object.
  **
  ** @return the port of the server
  */   
  public int get_port() {
    return this.port;
  }
  
}
