package drmudp;

/* An exception to throw if a NameServer lookup fails.
*/
class LookupException extends Exception {
  public LookupException(String message) {
        super(message);
    }
    private static final long serialVersionUID = 154297963212548754L;
}

/* An exception to throw if a NameServer registration fails.
*/
class RegistrationException extends Exception {
    
    private static final long serialVersionUID = 342343243245433L;
}

/* An exception to throw if a sever connection attempt fails.
*/
class ServerConnectException extends Exception {
    
    private static final long serialVersionUID = 649841691563749879L;
}

/* An exception to throw if a NamSever connection attempt fails.
*/
class NameServerContactException extends Exception {
    
    private static final long serialVersionUID = 345643534534879L;
}
