//main
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;

public class SystemControl {

	public static void main(String[] args) throws IOException, InterruptedException {
		//type, id, port, mcgroup, TCP/UDP, csx, msg
		String type = args[0];
		int id = Integer.parseInt(args[1]);
		int port = Integer.parseInt(args[2]);
		int mGroup = Integer.parseInt(args[3]);
		String mType = args[4];
		String addr = args[5];
		String msg = args[6];
		
		InetAddress in = InetAddress.getByName(addr + ".utdallas.edu");
		
		if(type.equals("server")) {
			Server s = new Server();
		} else if(type.equals("proxy")) {
			Proxy p = new Proxy(id, port, in);
			p.proxyJoin();
		} else { //client
			Client c = new Client(id, mGroup, port, in);
			if(mType.equals("tcp")) {
				c.sendTCP(msg);
			} else {
				c.sendUDP(msg);
			}
		}
		
//		InetAddress host = InetAddress.getLocalHost();
//		//Example on localhost. Will try to somehow run it on the networks during the demo
//		Server s = new Server();
//		
//		int[] sp = s.getPorts();
//		
//		Proxy p1 = new Proxy(1, sp[0], host);
//		int[] ports1 = p1.getPorts();
//		p1.proxyJoin();
//
//		Client c1 = new Client(10, 0, ports1[0], host);
//		Client c2 = new Client(11, 1, ports1[1], host);
//		Client c3 = new Client(13, 2, ports1[2], host);
//		Client c4 = new Client(14, 3, ports1[3], host);
//		Client c5 = new Client(15, 4, ports1[4], host);
//
//		c1.sendUDP("C1");
//		c2.sendUDP("C2");
//		c3.sendUDP("C3");
//		c4.sendUDP("C4");
//		c5.sendUDP("C5");
		
//		//works as expected with caching
//		//current tree:
//		/*
//		Server
//			\
//			P1
//		   / \
//		  c4  P2
//			  \
//			  P3
//			/ | \
//		   C1 C2 C3
//		 */
//		
	}
}
