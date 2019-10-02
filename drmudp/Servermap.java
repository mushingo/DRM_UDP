package drmudp;

import java.util.HashMap;

/* A class used to connect to the NameServer and retrieve and store information
** about other remote servers.
*/
class ServerMap {
  private HashMap<String, Server> servers; //A mapping of hostnames to Servers
  private Server nameServer; // The NameServer Server
  
  private int nameServerPort; //Name Server port
  
  /* Create a new map of servers and open connection/store information about the 
  ** nameServer. The map associates host name with a Server object. The server
  ** object represents a connected server.
  **
  ** @param nameServerPort the port of the Name Server
  */
  public ServerMap(int nameServerPort) {
    servers = new HashMap<String, Server>();
    this.nameServerPort = nameServerPort;
    nameServer = new Server(ComsFormat.nameserver_hostname
        , ComsFormat.nameserver_ip, nameServerPort);
  }
  
  /* Register server details with the name Server.
  **
  ** @param host the hostname of the service to register
  ** @param port the port of the service to register
  ** @param the ip address of the service to register
  */
  public void register(String host, int port, String ip) 
      throws RegistrationException{
    String reply = "";
    String message = ComsFormat.registration  + ComsFormat.separator + host 
        + ComsFormat.separator + port + ComsFormat.separator + ip;

    try {
      reply = nameServer.send_message(message, ComsFormat.retry,
          ComsFormat.sendTimeout,ComsFormat.receiveTimeout);
    } catch (Exception e) {
      throw new RegistrationException();
    }    
      
    if (!reply.equals(ComsFormat.regSucesss)) {
      throw new RegistrationException();
    } else {
      System.out.println(reply);
    }
  }
  
  /* Connect to a new server and add it's details to the map of servers.
  ** 
  ** @param host the hostname of the server
  */
  public void add_server(String host) throws LookupException, 
     NameServerContactException{
      
    String ip = lookup_ip(host);
    int port = lookup_port(host);
    System.out.println("Lookup of host " + host + ": Success");
    
    Server server = new Server(host, ip, port);
    
    servers.put(host, server);
  }
  

  
  /* Send a lookup request to the name server to get the port and ip of given
   ** host.
   **
   ** @param host the hostname to look up
   ** @return an array of strings containing the port and ip of host
   */
  private String[] lookup(String host) throws LookupException, 
      NameServerContactException {
    String reply = "";
    String message = ComsFormat.lookup + ComsFormat.separator + host;
    nameServer = new Server(ComsFormat.nameserver_hostname, 
        ComsFormat.nameserver_ip, nameServerPort);


    try {
      reply = nameServer.send_message(message, ComsFormat.retry, 
          ComsFormat.sendTimeout, ComsFormat.receiveTimeout);
    } catch (Exception e) {
      throw new NameServerContactException();
    }

    if (reply.equals(ComsFormat.lookupError)) {
      throw new LookupException(host);
    } 

    return reply.split(ComsFormat.separator);
  }
  
  /* Returns the IP address of the given host after looking it up with the name
  ** Server.
  **
  ** @param host the hostname to lookup
  ** @return the IP address as a string
  */  
  private String lookup_ip (String host) throws LookupException
      , NameServerContactException{
    return lookup(host)[0];
  }
  
  /* Returns the port of the given host after looking it up with the name
  ** Server.
  **
  ** @param host the hostname to lookup
  ** @return the port of the given host
  */    
  private int lookup_port (String host) throws LookupException
      , NameServerContactException{
    return Integer.parseInt(lookup(host)[1]);
  }
  
  
  /* Return the server object for the given hostname.
  ** 
  ** @param host the hostname of the server to retrieve
  ** @return the Server object associated with hostname
  */
  public Server get_server(String host) {
    return servers.get(host);
  }
}
