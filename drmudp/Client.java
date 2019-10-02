package drmudp;

import java.io.IOException;

/* A client connects to the store server and either requests and prints out a 
** list of items it can buy or attempts to buy an item. The action to take is 
** specified on the command line. The first command line argument is the request
** represented as a number (0 to list and > 0 to buy item number in list).
** The second argument is the name server port which is used to lookup the 
** details of the store server.
**
*/
public class Client {
  //Exit Status codes
  private static final int BAD_ARGS = 1;
  private static final int LOOKUP_FAILURE  = 5;   
  private static final int NAMESERVER_CONNECT_FAIL = 6;
  private static final int STORE_CONNECT_FAIL = 7;
  private static final int NO_ITEM = 8;
  
  //A dummy 16 digit credit card number
  private static final long CREDIT_CARD_NO = 1234567812345678L;
  
  //Intance variable used to store information about servers
  private ServerMap servers = null;
  
  /*Create a new Client object using the arguments given in the commandline.
  **
  ** @param args The command line arguments. The first command line argument is 
  ** the request represented as a number (0 to list and > 0 to buy item 
  ** number in list). The second argument is the name server port
  */
  public static void main (String[] args)  {
    new Client(args);
  }


 
  /* Creates a new Client Object which connects to the name server, gets the
  ** store server's details and then connects to the store and sends it's 
  ** request either to buy an item or list items available. The result of this
  ** action is printed to standard out, unless an error is encountered in which 
  ** case the client exits with the appropriate exit status.
  **
  ** @param args The command line arguments. The first command line argument is 
  ** the request represented as a number (0 to list and > 0 to buy item number  
  ** in list). The second argument is the name server port
  */
  public Client(String[] args) {
    
    int nameServerPort = -1;
    int request = -1;
    
    if (args.length != 2) {
      exit(BAD_ARGS); 
    }
    
    try {
      request = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      exit(BAD_ARGS);      
    }
        
    if ((nameServerPort = check_valid_port(args[1])) < 0 || request < 0 ) {
      exit(BAD_ARGS); 
    }
    
	//Register with name server though this is not necessary at this stage and
	//place holder literal values are used.
    try {
      servers = new ServerMap(nameServerPort);
      servers.register("client", 6465, "localhost");
    } catch ( RegistrationException e) {
      exit(NAMESERVER_CONNECT_FAIL);
    }

    try {
      servers.add_server(ComsFormat.store_hostname);
    } catch (LookupException e) {
      System.err.print(e.getMessage() + ComsFormat.separator
          + "has not registered\n");
      exit(LOOKUP_FAILURE);
    } catch (NameServerContactException e) {   
      exit(NAMESERVER_CONNECT_FAIL);
    } 
    
    if (request == 0) {
      System.out.println(get_list());
    } else {
      Item item = buy_item(request);
      if (item.get_itemContent().contains(ComsFormat.transaction_fail)) {
        System.out.println(item.get_itemContent());
      } else {
        System.out.println(item.get_itemId() + " ($ " + item.get_itemPrice() 
            + ") CONTENT " + item.get_itemContent());
      }
      
    }
  }
  
  /* Sends a request to buy an item to the Store, if successful a new item is 
  ** created with the price, item-id and content of the item. If unsuccessful
  ** the new item has an empty string as it's content. The new item is returned.
  ** 
  ** @param args the number of the item to attempt to buy
  ** @return the item that was attempted to be bought
  */  
  private Item buy_item(int request) {
    String itemContent = "";
    String list = get_list();
    String[] items = list.split(ComsFormat.newline);
    if (request > items.length) {
      exit(NO_ITEM);
    }
    String entry = items[request - 1];
    String[] itemFields = entry.split(" ");
    String itemNo = itemFields[0];
    String itemId = itemFields[1];
    String itemPrice = itemFields[2];
    Server newConnection = null;
	
    String buyRequest = ComsFormat.buyRequest + ComsFormat.separator 
        +  CREDIT_CARD_NO + ComsFormat.separator + itemId;
    
     
    newConnection = new Server(
        servers.get_server(ComsFormat.store_hostname).get_host(),
        servers.get_server(ComsFormat.store_hostname).get_ip(),
        servers.get_server(ComsFormat.store_hostname).get_port()
        );
    
    
    try {
      
      itemContent = newConnection.send_message(
          buyRequest, ComsFormat.clientRetry, ComsFormat.clientSendTimeout,
          ComsFormat.clientReceiveTimeout);;
    } catch (IOException e) {
      exit(STORE_CONNECT_FAIL);
    }
    Item item = new Item(itemNo, itemId, itemPrice, itemContent);
               
    return item;
  }

  /* Sends a request for the list of items able to be bought from the store.
  ** If the request is successful the list is returned.
  **
  ** @return the list of items able to be bought from the store
  */  
  private String get_list() {
    String storeReply = "";
    String list = "";
    int count = 0;
	
    try {
      storeReply = servers.get_server(ComsFormat.store_hostname).send_message(
          ComsFormat.listRequest, ComsFormat.clientRetry, 
          ComsFormat.clientSendTimeout, ComsFormat.clientReceiveTimeout);
    } catch (IOException e) {
      exit(STORE_CONNECT_FAIL);
    }
    
    String lines[] = storeReply.split(ComsFormat.newline);
    storeReply = lines[count];
    
    while (!storeReply.equals(ComsFormat.listEnd)) {
      if (storeReply.length() > 0  && !(storeReply.equals(ComsFormat.listStart) 
          || storeReply.equals(ComsFormat.listEnd))) {
          list = list + count + ". " + storeReply + ComsFormat.newline;       
      }
      count++;
      storeReply = lines[count];
    }   
	
    return list.trim();
  }
  
  /* Checks that the supplied port is a number within the valid port range 
  ** > 0 < 65535 and if so returns an int representing that port. 
  ** Otherwise returns -1.
  **
  ** @param porArg the argument to check and convert
  ** @return The port if it's within the valid range, -1 otherwise
  */    
  private int check_valid_port(String portArg) { 
    int port;
    try {
      port = Integer.parseInt(portArg);
    } catch (NumberFormatException e) {
        return -1;      
    }
    
    if (port < 1 || port > 65535) {
        return -1;
    }
    
    return port;
}
  
  /* Exit the Client with the appropriate error message and status.
  **
  ** @param status the exit status to exit with
  */  
  private void exit(int status) {
    switch (status) {
        case BAD_ARGS: 
            System.err.print("Invalid command line arguments\n");
            System.exit(BAD_ARGS);
        case LOOKUP_FAILURE:
            System.exit(LOOKUP_FAILURE);
        case NAMESERVER_CONNECT_FAIL:
          System.err.print("Client unable to communicate with NameServer\n");
          System.exit(NAMESERVER_CONNECT_FAIL);
        case STORE_CONNECT_FAIL:
          System.err.print("Client unable to communicate with Store\n");
          System.exit(STORE_CONNECT_FAIL);
        case NO_ITEM:
          System.err.print("Specified item does not exist in store\n");
          System.exit(NO_ITEM);
    }
    
  }
}

/*A class representing the items that can be bought from the store 
*/
class Item {
  private String itemNo;
  private String itemId;
  private String itemPrice;
  private String itemContent;
  
  /*Create a new instance of an item that can be bought from the store.
  **
  ** @param itemNo The item number in list returned from store
  ** @param itemId The unique item identification (ID) number
  ** @param itemPrice The price of the item
  ** @param itemContent The actual content of the item
  */
  public Item(String itemNo, String itemId, String itemPrice
      , String itemContent) {
    this.itemContent = itemContent;
    this.itemPrice = itemPrice;
    this.itemId = itemId;
    this.itemNo = itemNo;
  }
  
  /* Returns the price of the item.
  **
  ** @return the item price
  */
  public String get_itemPrice() {
    return itemPrice;
  }
  
  /* Returns the content of the item.
  **
  ** @return the item content
  */
  public String get_itemContent() {
    return itemContent;
  }

  /* Returns the ID of the item.
  **
  ** @return the item ID
  */  
  public String get_itemId() {
    return itemId;
  }
  
  /* Returns the number of the item in the list returned by store.
  **
  ** @return the number of the item in the list returned by store
  */    
  public String itemNo() {
    return itemNo;
  }
  
}
