package drmudp;

/* Definitions of some commonly used communication Strings.
*/
class ComsFormat {
  private static final String DEFAULT_IP = "localhost";
  
  public static final String separator = " ";
  public static final String newline = System.getProperty("line.separator");
  public static final String fileSep = System.getProperty("file.separator");
  public static final String registration = "REG";
  public static final String regSucesss = "REGISTRATION_SUCCESS";
  public static final String lookup = "LOOKUP";
  public static final String lookupError = "Error: Process has not registered"
      + " with the Name Server";
  public static final String listRequest = "LIST";
  public static final String buyRequest = "BUY";
  public static final String listStart = "LIST_START";
  public static final String listEnd = "LIST_END";
  public static final String purchase_success = "1";
  public static final String purchase_fail = "0";
  public static final String transaction_fail = "\"transaction aborted\"";
  public static final String request_content = "REQ";
  public static final String store_hostname = "Store";
  public static final String bank_hostname = "Bank";
  public static final String content_hostname = "Content";
  public static final String nameserver_hostname = "NameServer";
  public static final String nameserver_ip = DEFAULT_IP;
  public static final String bank_ip = DEFAULT_IP;
  public static final String content_ip = DEFAULT_IP;
  public static final String store_ip = DEFAULT_IP;
  public static final int sendTimeout = 100;
  public static final int receiveTimeout = 500;
  public static final int retry = 3;
  public static final int clientSendTimeout = 100;
  public static final int clientReceiveTimeout = 1000;
  public static final int clientRetry = 5;
  public static final double packetLossProb = 0.5;
}
