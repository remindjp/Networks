

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;

public class Client {
	public int id;
	private int groupID;
	private int proxyPort;
	private TCPConnector tcp;
	private UDPConnector udp;
	private BufferedReader r;
	//Each client has a designated proxy. If the proxy cannot be reached then
	//the client creation will fail.
	public Client(int id, int groupID, 
			int proxyPort, InetAddress proxyAddr) throws IOException {
		this.id = id;
		this.groupID = groupID;
		connect(proxyPort, proxyAddr);

	}
	
	//Can be used for reconnecting as well
	public void connect(int proxyPort, InetAddress proxyAddr){
		try {
			this.proxyPort = proxyPort;
			tcp = new TCPConnector(proxyPort, proxyAddr, 1);	//error if proxy offline
			sendJoin();
			
			r = tcp.getReader();
			//System.out.println(r.readLine());
			if(!r.readLine().equals(ControlMessages.OK + " " + groupID + " P")) {		//error if proxy is full
				System.out.println("Proxy at full capacity");
				tcp.closeAll();
				return;
			}

			readParent();
			udp = new UDPConnector(proxyPort, proxyAddr, 1);
			sendUDP(ControlMessages.UDP_HELLO);
			readUDP();
		} catch (IOException e) {
		}
		
		//Establish UDP client for data transfer and read
		
		
	}
	
	//format is request + port + groupID
	public void sendJoin() {
		sendTCP(ControlMessages.CLIENT_JOIN + " " + groupID);
	}
	//format is request + port + groupID
	public void sendPrune() {
		sendTCP(ControlMessages.CLIENT_PRUNE + " " + groupID);
	}
	
	//Send a message via TCP to the proxy + multicast group
	public void sendTCP(String s) {
		tcp.send(s + " P " + proxyPort);
	}	
	
	//Sends a message to the multicast group
	public void sendUDP(String msg) throws IOException {
		udp.send(msg + " " + groupID);
	}
	
	//reads messages from the parent
	public void readParent() {
		new Thread(new Runnable() {
			public void run() {
				try {
					BufferedReader r = tcp.getReader();
			    	String data = null;
		    		while((data = r.readLine()) != null) {
		    			System.out.println(data);
		    			//always will end with " P" since its the destination
		    			data = data.substring(0, data.length() - 2);
		    			System.out.println("--TCPClient " + id + ":" + data);
		    		}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}		
	
	//reads UDP messages in a seperate thread
	public void readUDP() {
	    new Thread(new Runnable() {
            public void run() {
	    		while(true) {
	    			String data = udp.recv();
	    			if(!data.startsWith(ControlMessages.UDP_HELLO)) {
	    				System.out.println("--UDPClient " + id + ":" + data);
	    			}
				}
    		}
	    }).start();
	}
}
