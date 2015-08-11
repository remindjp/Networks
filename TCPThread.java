

import java.io.IOException;

public abstract class TCPThread {
	protected TCPConnector tcp;
	protected int port;
	
	public TCPThread(int port) {
		this.port = port;
	}
	public void resetTCP() throws IOException {
		tcp.closeAll();
	}
	public abstract void run();
}
