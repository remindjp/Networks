

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class Proxy extends SuperProxyServer {
	public int id;
	public int parentPort;
	public InetAddress host;
	
	public ConcurrentHashMap<Integer, HashSet<Integer>> optimizationCache; //maps multicast ID to the cache

	public TCPConnector tcpParent;
	public UDPConnector udpParent;
	
	
	public final int[] active;
	
	//holds child requests if not connected to a server
	public volatile boolean isConnected;
	
	public Proxy(int id, int parentPort, InetAddress host) throws IOException {
		
		this.id = id;
		this.parentPort = parentPort;
		this.host = host;
		this.active = new int[]{0,0};
		
		this.isConnected = false;
		
		//The cache stores string hashes for each multicast group
		this.optimizationCache = new ConcurrentHashMap<Integer, HashSet<Integer>>();
		
		//Create 5 ProxyThreads with unique ports based on the unique Proxy id
		for(int i = 0; i < ControlMessages.PROXY_MAX_CHILDREN; i++) {
			int open = findOpenPort();
			
			ProxyThreadTCP tcp = new ProxyThreadTCP(open);
			new Thread(tcp).start();
			tcpThreadMap.put(open, tcp);	
			
			ProxyThreadUDP udp = new ProxyThreadUDP(open);
			new Thread(udp).start();
			udpThreadMap.put(open, udp);
			

			ports[i] = open;
			multiPort.put(i, new HashSet<Integer>());
			
			optimizationCache.put(i, new HashSet<Integer>());
		}

		System.out.println("Proxy " + id + " threads on ports " + Arrays.toString(ports));
		
	}
	//send join message as proxy
	public void proxyJoin() {
		try {
			tcpParent = new TCPConnector(parentPort, host, 1);
			BufferedReader r = tcpParent.getReader();			
			sendTCP(ControlMessages.PROXY_JOIN + " " + multiString() + " P", tcpParent, true);
			if(!r.readLine().startsWith(ControlMessages.PROXY_OK)) {
				System.out.println("Rejected");
				tcpParent.closeAll();
				return;
			} else {
				System.out.println("Proxy " + id + " joining on port " + parentPort);
				isConnected = true;
				readParentTCP();
				udpParent = new UDPConnector(parentPort, host, 1);
				readParentUDP();
			}
		} catch (IOException e) {
		}
	}

	public void proxyPrune() throws IOException {
		sendTCP(ControlMessages.PROXY_PRUNE + " " + multiString() + " P", tcpParent, true);
		isConnected = false;
		tcpParent.closeAll();
	}
	
	private void readParentUDP() {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					String m = udpParent.recv();

					Integer multiID = Integer.parseInt(getRight(m));
////////////////////forward udp AND OPTIMIZE
					try {
						int h = m.hashCode();
						HashSet<Integer> cache = optimizationCache.get(multiID);

						synchronized(cache) {
							if(cache.contains(h)) {
								cache.remove(h); //decache
								System.out.println("***Proxy " + id + " already sent " + m);
							} else {
								for(Integer port : multiPort.get(multiID)) {
									udpPortMap.get(port).send(m);
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
							
				}
			}				
		}).start();
		
	}
	
	//reads messages from the parent TCP
	public void readParentTCP() {
		new Thread(new Runnable() {
			public void run() {
				try {
					BufferedReader r = tcpParent.getReader();
			    	String data = null;
		    		while((data = r.readLine()) != null) {
						System.out.println("Proxy "  + id + ", " + parentPort
								+ ": " + data);
							forwardDown(data);

////////////////////////////Client join request approved, officially add
						int destPort = Integer.parseInt(getRight(data));
		    			if(data.startsWith(ControlMessages.OK)) {
	    					multiPort.get(getMultiID(data)).add(destPort);
	    					address.put(destPort, tcpPortMap.get(destPort).getSocket().getInetAddress());
							if(!getPath(data).contains(" ")) { //for you 
								synchronized(active) {	
									active[1]++;
								}
							}

		    			} else if(data.startsWith(ControlMessages.FAIL) ||
		    					data.startsWith(ControlMessages.PROXY_FAIL)) {
////////////////////////////restart
		    				restart(destPort);

////////////////////////////Proxy join request approved, officially add
		    			} else if(data.startsWith(ControlMessages.PROXY_OK)) {
							if(!getTuple(data).contains(" ")) { //for you 
								synchronized(active) {	
									active[0]++;
								}
							}
							String incoming = getTuple(data);
							String ours = multiString();
		    				checkAndUpdate(ours, incoming,destPort);

	    					address.put(destPort, tcpPortMap.get(destPort).getSocket().getInetAddress());
		    			}
		    		}
				} catch (IOException e) {
				}
			}
		}).start();
	}		
	
	//Forwards a formatted TCP control message downwards
	public void forwardDown(String m) {
		sendTCP(getLeft(m), tcpPortMap.get(Integer.parseInt(getRight(m))), false);
		System.out.println("ProxyThread " + parentPort + " fwd: " + getLeft(m) +
				" to " + getRight(m));
	}

	/////////////////////////////////////////////////PROXYTHREAD
	
	public class ProxyThreadTCP extends TCPThread implements Runnable  {
		private boolean isClient;
		
		public ProxyThreadTCP(int port) {
			super(port);
			this.isClient = false;
		}
		
		
		//Read from below FROM DOWN
		public void run() {
			try {

				tcp = new TCPConnector(port, null,0);
				tcpPortMap.put(port, tcp);

				BufferedReader r = tcp.getReader();
		    	String data = null;
	    		while((data = r.readLine()) != null && isConnected) {
					System.out.println("ProxyThread "  + id + ", " + port
							+ ": " + data);
					
					String path = getPath(data);
					String destPort = getLeft(path);

//////////////////////////////////////// HANDLE CLIENT JOIN REQUEST 
	    			//(if you have room, forward up for approval)
					synchronized(active) {
		    			if(data.startsWith(ControlMessages.CLIENT_JOIN)) {
							int multiID = getMultiID(data);
							if((active[0] + active[1]) >= 5) { //make sure you have room
								sendTCP(ControlMessages.FAIL + " " + multiID + " " + destPort, tcp, false);
		    					return;
							}
		    			}
	/////////////////////////////////////// FORWARD NO MATTER WHAT UNLESS INSTAFAIL
		    			//DURING A JOIN FOR PROCESSING. DO ADDING IN THE PARENT.
		    			sendTCP(data, tcpParent, true);
	////////////////////////////////////////HANDLE CLIENT PRUNE REQUEST 		
	
		    			
		    			if(data.startsWith(ControlMessages.CLIENT_PRUNE)) {
			    				active[1]--;
		    					multiPort.get(getMultiID(data)).remove(port);
			    				restart(port);
			    				address.remove(port);
	
		    			} else if (data.startsWith(ControlMessages.PROXY_JOIN)) {
							if(active[0] >= 3) { //make sure you have room
								String incoming = getTuple(data);
								sendTCP(ControlMessages.PROXY_FAIL + " " + incoming + " " + destPort, tcp, false);
		    					return;
							}
		    			} else if (data.startsWith(ControlMessages.PROXY_PRUNE)) {

		    				address.remove(port);
		    				active[0]--;
		    				remove(getTuple(data), port);
		    			}
					}
	    		}
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}	
		
	}
	
	///////////////////////////////////////////////// UDP
	
	public class ProxyThreadUDP extends UDPThread implements Runnable {
		public ProxyThreadUDP(int port) throws SocketException {
			super(port);
		}
		
		//Read from below FROM DOWN + caching
		public void run() {
			try {
				udp = new UDPConnector(port, null,0);
				udpPortMap.put(port, udp);	
				
		    	while(true) {
	    			String m = udp.recv();
	    			//connect / disconnect artifact
	    			if(m.equals("")) continue; 
					System.out.println("UDP ProxyThread "  + id + ", " + port + ": " + m);
					Integer multiID = Integer.parseInt(getRight(m));
					
//////////////////dont optimize control messages CACHE
	    			if(!m.startsWith(ControlMessages.UDP_HELLO)) {
						HashSet<Integer> cache = optimizationCache.get(multiID);
						synchronized(cache) {
							cache.add(m.hashCode());
							System.out.println("****Proxy " + id + " cached " + m);
							for(Integer port : multiPort.get(multiID)) {
								udpPortMap.get(port).send(m);
							}
						}
	    			}
	    			
	    			sendUDP(udpParent, m);
	    		}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		
		//Synchronized send for UDP
		public void sendUDP (UDPConnector udpSync, String m) throws IOException {
			synchronized (udpSync) {
				udpSync.send(m);
			}
		}
	}
	
	public void printCache() {
		System.out.println("Optimization cache " + id);
		for(int id : optimizationCache.keySet()) {
			System.out.println("multicast " + id + ": " +
					optimizationCache.get(id).toString());
		}
		System.out.println();
	}
}
