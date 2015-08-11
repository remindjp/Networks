//Superclass for proxy server redundancies

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

//This class is used to remove redundancy from Proxy / Server
public abstract class SuperProxyServer {

	//Maps multicast ID to the ports of that group
	public ConcurrentHashMap<Integer, HashSet<Integer>> multiPort;
	
	//Maps ports to UDP Connections, fixed
	ConcurrentHashMap<Integer, UDPConnector> udpPortMap;
	//Maps ports to TCP Connections, fixed
	ConcurrentHashMap<Integer, TCPConnector> tcpPortMap;
	
	//Maps ports to TCP Threads
	ConcurrentHashMap<Integer, TCPThread> tcpThreadMap;		
	//Maps ports to UDP Threads
	ConcurrentHashMap<Integer, UDPThread> udpThreadMap;

	ConcurrentHashMap<Integer, InetAddress> address;
	
	//Outgoing ports
	public int[] ports;
	
	public SuperProxyServer () throws UnknownHostException {

		InetAddress cs1 = InetAddress.getByName("cs1.utdallas.edu");
		InetAddress cs2 = InetAddress.getByName("cs2.utdallas.edu");
		InetAddress cs3 = InetAddress.getByName("cs3.utdallas.edu");
		InetAddress cs4 = InetAddress.getByName("cs4.utdallas.edu");
		
		this.multiPort = new ConcurrentHashMap<Integer, HashSet<Integer>>();
		this.udpPortMap = new ConcurrentHashMap<Integer, UDPConnector>();
		this.tcpPortMap = new ConcurrentHashMap<Integer, TCPConnector>();
		this.ports = new int[ControlMessages.PROXY_MAX_CHILDREN];

		this.tcpThreadMap = new ConcurrentHashMap<Integer, TCPThread>();
		this.udpThreadMap = new ConcurrentHashMap<Integer, UDPThread>();
		this.address = new ConcurrentHashMap<Integer, InetAddress>();
	}
	
	//finds an open port by letting OS decide
	public int findOpenPort() throws IOException {
		ServerSocket ss = new ServerSocket(0);
		int open = ss.getLocalPort();
		ss.close();
		return open;
	}
	
	//Returns ports
	public int[] getPorts() {
		return ports;
	}
	//Assumes its a proxy request. Gets the 5 tuple
	public String getTuple(String s) {
		String[] split = s.split(" ", 3);
		return split[1];
	}
	
	//if theirs > 0, they had a stream so remove it
	public void remove(String s2, int port) {
		String[] split2 = s2.split(",");
		
		for(int i = 0 ; i < split2.length; i ++) {
			int two = Integer.parseInt(split2[i]);
			if(two > 0) {
				multiPort.get(i).remove(port);
			}
		}	
	}
	//Reutrns the path of a formatted TCP message
	public String getPath(String m) {
		String[] split = m.split(" ", 4);
		return split[3];
	}
	//Checks two multiStrings and does s1 - s2, updating the caller on the given port
	public boolean checkAndUpdate(String s1, String s2, int port) {
		String[] split1 = s1.split(",");
		String[] split2 = s2.split(",");
		int[] result = new int[split1.length];
		boolean[] changed = new boolean[split1.length];
		
		for(int i = 0 ; i < split1.length; i ++) {
			int one = Integer.parseInt(split1[i]);
			int two = Integer.parseInt(split2[i]);
			result[i] = one - two;
			
			if(result[i] < 0) {
				return false; // no space
			}
			if(two > 0) {
				changed[i] = true;
			}
		}
		//update map
		for(int i = 0; i < split1.length; i++) {
			if(changed[i]) {
				multiPort.get(i).add(port);
			}
		}
		return true;
	}
	//returns the multiPort as a string with commas
	public String multiString() {
		String s = "";
		for(int i = 0; i < multiPort.size(); i ++) {
			s+= multiPort.get(i).size() + ",";
		}
		return s.substring(0, s.length() - 1);
	}
	
	public ConcurrentHashMap<Integer, HashSet<Integer>> getMultiPort() {
		return multiPort;
	}
	//Reset the TCP and UDP thread at this port
	public void restart(int port) throws IOException {
		TCPThread t = tcpThreadMap.get(port);
		t.resetTCP();
		new Thread((Runnable) t).start();
		UDPThread u = udpThreadMap.get(port);
		u.resetUDP();
		new Thread((Runnable) u).start();

	}

	//Returns the rightmost string (THE STRING - PORT)
	public String getRight(String m) {
		String[] split = m.split(" ");
		return split[split.length - 1].trim();
	}
	//Returns the left - 1, (THE PORT)
	public String getLeft(String m) {
		return m.substring(0, m.length() - getRight(m).length()).trim();
	}
		
	//Gets the multicast ID from a string
	public int getMultiID(String m) {
		String[] split = m.split(" ", 3);
		return Integer.parseInt(split[1]);
	}
	
//	//Reset the UDP states
//	public void udpCommonReset(int port, UDPConnector udp) throws IOException {
//		resets.put(port, false);
//		udp.closeAll();
//		udpPortMap.remove(port);
//	}
	
	//Send a synchronized TCP message, append port at end
	public void sendTCP(String m, TCPConnector tcp, boolean appendPort) {			
		synchronized(tcp) {
			if(appendPort) {
				tcp.send(m + " " + tcp.getPort());
			} else {
				tcp.send(m);
			}
		}	
	}	
//	public void printState() {
//		System.out.println("destinations mapping:");
//		for(Integer mG : destinations.keySet()) {
//			System.out.print("multicast ID" + mG + ": ");
//			System.out.println(destinations.get(mG).toString());
//		}
//	}
	
}
