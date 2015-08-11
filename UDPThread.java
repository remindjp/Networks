
//Abstract class for UDP threads
import java.io.IOException;
import java.net.SocketException;

public abstract class UDPThread {
	protected UDPConnector udp;
	protected int port;
	
	public UDPThread(int port) throws SocketException {
		this.port = port;

	}
	
	public void resetUDP() throws IOException {
		udp.closeAll();
	}
	
	public abstract void run();
}
