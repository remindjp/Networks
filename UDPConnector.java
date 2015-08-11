


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPConnector {
	private int type;
	
	private InetAddress host;
	private int port; //server reuses: initially set to binding port, then client's port
	
	private final static int PACKET_SIZE = 100 ;
	private DatagramSocket s;
	
	public DatagramSocket getSocket() {
		return s;
	}
	//for server the InetAddress is not used
	public UDPConnector(int port, InetAddress host, 
			int type) throws SocketException {
		this.port = port;
		if(type==0) { 	//server
			s = new DatagramSocket(port);
		} else { 		//client
			this.host = host;
			s = new DatagramSocket();
		}
	}
	
	//Sends a message. Cannot send as server unless recv() has been called once.
	public void send(String msg) throws IOException {
		byte[] data = msg.getBytes() ;
        DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
        s.send(packet);
	}
	
	//Receive a message. If server, client's host / port is acquired after recv().
	public String recv() {
		DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);

		try {
			s.receive(packet);
		} catch (IOException e) {
		}

		if(type==0) {
			this.host = packet.getAddress();
			this.port = packet.getPort();
		}
		String m = (new String(packet.getData())).trim();
		return m;
	}

	
	//close the connection
	public void closeAll() throws IOException {
		s.close();
	}	
	
}
