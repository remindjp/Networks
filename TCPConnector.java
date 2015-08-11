


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPConnector{
	private int port;
	private int type; 					//0 = sever, 1(else) = client
	private ServerSocket ss; 			//not used if acting as client

	private Socket client;				//Used in both cases for r/w
	private PrintWriter writer;			//Writer for socket
	private BufferedReader reader;		//Reader for socket
	
	//for server the InetAddress is not used
	public TCPConnector( int port, InetAddress host, 
			int type) throws IOException {
		this.port = port;
		this.type = type;

		if(type==0) {
			ss = new ServerSocket(port);
			client = ss.accept();
		} else {
			client = new Socket(host, port);
		}
		TCPSetup();
	}

	public Socket getSocket() {
		return client;
	}
	//establish read / write streams
	public void TCPSetup() throws IOException {
		reader = new BufferedReader(new InputStreamReader(
				client.getInputStream()));
		
		writer = new PrintWriter(client.getOutputStream(),true);
	}

	//Sends a message
	public void send(String msg) {
		writer.println(msg);
	}
	
	//Returns the reader that is used at a higher level
	public BufferedReader getReader() {
		return reader;
	}
	
	//Prints to console with an id prefix
	public void print(String s) {
		System.out.println("TCPConnector " + port + ": " + s);
	}
	
	//close the connection
	public void closeAll() throws IOException {
		client.close();
		if(type==0) {
			ss.close();
		}
	}
	
	public int getPort() {
		return port;
	}
}
