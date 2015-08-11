

public class ControlMessages {
	public static String NOTIFY = "CONTROL:NOTIFY";
	public static final String CONTROL = "CONTROL:";
	
	public static final String CONNECTION_ESTABLISHED = 
			"CONTROL:CONNECTION_ESTABLISHED";
	
	/*
	 * Every message starts with CONTROL.. [parameters] and ends with a variable length 
	 * path that is updated at each node, indicated after a " P " placeholder.
	 * example: CONTROL:CLIENT_JOIN	0 P 34834 34899 ...
	 */
	
	//parameter: mutlicast group
	public static final String CLIENT_JOIN = "CONTROL:CLIENT_JOIN";
	public static final String CLIENT_PRUNE = "CONTROL:CLIENT_PRUNE";

	public static final String PROXY = "CONTROL:PROXY";
	//parameters: multicast group counts expressed as a spaced string, ex 0 0 1 0 0
	public static final String PROXY_JOIN = "CONTROL:PROXY_JOIN";
	public static final String PROXY_PRUNE = "CONTROL:PROXY_PRUNE";

	public static final String TCP_TEST = "CONTROL:TCP_TEST";
	//parameters: type, group
	public static final String ASK = "CONTROL:ASK";
	//parameters: + multicast group string representation
	public static final String PROXY_REJOIN = "CONTROL:PROXY_REJOIN";
	
	public static final String FAIL = "CONTROL:FAIL";
	public static final String OK = "CONTROL:OK";

	public static final String PROXY_FAIL = "CONTROL:PROXY_FAIL";
	public static final String PROXY_OK = "CONTROL:PROXY_OK";
	
	public static final String UDP_HELLO = "CONTROL:UDP_HELLO";
	
	
	//limits
	public static final int PROXY_MAX_CHILDREN = 5;
	public static final int PROXY_MAX_PROXIES = 3;
	public static final int SERVER_MAX_STREAMS = 5;
	
	
}
