
/*
	This class is the server class which instantiates instances
	of the ServerThread class. No computation is done here.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class Server extends SuperProxyServer {
	
	public Server() throws IOException {
		//Create 5 ServerThreads on available sockets
		for(int i = 0; i < ControlMessages.SERVER_MAX_STREAMS; i++) {
			
			int open = findOpenPort();
			
			ServerThreadTCP tcp = new ServerThreadTCP(open);
			new Thread(tcp).start();
			tcpThreadMap.put(open, tcp);
			
			ServerThreadUDP udp = new ServerThreadUDP(open);
			new Thread(udp).start();
			udpThreadMap.put(open, udp);
			
			ports[i] = open;
			multiPort.put(i, new HashSet<Integer>());
		}
		System.out.println("ServerThreads on ports " + Arrays.toString(getPorts()));
	}
	
	public class ServerThreadTCP extends TCPThread implements Runnable {
		public ServerThreadTCP(int port) throws IOException {
			super(port);
		}
		
		//Read FROM DOWN  n
		public void run() {
			try {
				tcp = new TCPConnector(port, null,0);
				tcpPortMap.put(port, tcp);
				
				BufferedReader r = tcp.getReader();
				String data = null;
	    		//see ControlMessages class for formatting
	    		while((data = r.readLine()) != null) {
					System.out.println("SERVER Port "  + port
							+ ": " + data);
					
					String path = getPath(data);
					String destPort = getLeft(path);
					if(destPort.length()==0) {
						destPort = Integer.toString(port);
					}
					
					//dont pass notifications / misformatted
					if(data.startsWith(ControlMessages.NOTIFY)) {
						continue;
					}
					//forward tests
					if(data.startsWith(ControlMessages.TCP_TEST)) {
    					sendTCP(getLeft(data), tcp, false);
					}

					if (data.startsWith(ControlMessages.CLIENT_JOIN)) {
						int multiID = getMultiID(data);
						//if stream already exists or you have room
						HashSet<Integer> hs = multiPort.get(multiID);
						synchronized(hs) {						
							if(hs.size() > 0 || getActiveStreams() < 5) {
		    					hs.add(port);
		    					address.put(port, tcp.getSocket().getInetAddress());
		    					sendTCP(ControlMessages.OK + " " +  multiID + " P " + destPort, tcp, false);
							} else {
		    					sendTCP(ControlMessages.FAIL + " " + multiID + " P " + destPort, tcp, false);
							}
						}
					//already pruned , just need to update records
					} else if (data.startsWith(ControlMessages.CLIENT_PRUNE)) {
    					multiPort.get(getMultiID(data)).remove(port);
						address.remove(port);
    					
					} else if (data.startsWith(ControlMessages.PROXY)) {
						String incoming = getTuple(data);
						String ours = multiString();
						//String ours = "10,10,10,10,10";
						if (data.startsWith(ControlMessages.PROXY_JOIN)) {
							if(checkAndUpdate(ours, incoming, port)) {

		    					address.put(port, tcp.getSocket().getInetAddress());
		    					sendTCP(ControlMessages.PROXY_OK + " " + incoming + " " + destPort, tcp, false);
//		    					System.out.println("SERVER SENT:" +  ControlMessages.PROXY_OK + " " + incoming + " P " + destPort);
							} else {
		    					sendTCP(ControlMessages.PROXY_FAIL + " " + incoming + " P " + destPort, tcp, false);
							}
						} else if(data.startsWith(ControlMessages.PROXY_PRUNE)) {
							remove(incoming, Integer.parseInt(destPort));
							restart(Integer.parseInt(destPort));
							address.remove(port);
						}
					}
	    		}
			} catch (IOException e) {
				//e.printStackTrace();
				return;
			}
		}	
		
		//returns the number of active streams
		public int getActiveStreams() {
			int active = 0;
			for(HashSet<Integer> ports : multiPort.values()) {
				if(ports.size() > 0) {
					active++;
				}
			}
			return active;
		}
	}

	/////////////////////////////////////////////////////////////////
	public class ServerThreadUDP extends UDPThread implements Runnable {
		public ServerThreadUDP(int port) throws IOException {
			super(port);
		}

		//Read from a child (FROM DOWN)
		public void run() {
			try {
				udp = new UDPConnector(port, null,0);
				udpPortMap.put(port, udp);
				
	    		while(true) {
	    			
	    			String m = udp.recv();	    			
	    			if(m.equals("") || m.startsWith(ControlMessages.UDP_HELLO)) {
	    				continue;
	    			}
					System.out.println("Server UDP Port "  + port + ": " + m);
					
					Integer multiID = Integer.parseInt(getRight(m));
	    			//forward to all multicast groups
	    			for(Integer p : multiPort.get(multiID)) {
	    				udpPortMap.get(p).send(m);
	    			}
	    		}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
