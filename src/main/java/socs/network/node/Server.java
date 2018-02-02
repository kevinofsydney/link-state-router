package socs.network.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

	private Router myRouter;
	private short myPort;
	private boolean serverOn = true;

	public Server(Router router, short port) {
		myRouter = router;
		myPort = port;
	}

	public void run() {

		ServerSocket serverSocket = null;

		// create server socket
		try {
			serverSocket = new ServerSocket(myPort);
		} catch (IOException ioe) {
			System.out.println("Could not create server socket on port " + myPort + ". Quitting.");
			System.exit(-1);
		}

		while (serverOn) {

			try {
				
				// accept will block until a client connects to the server. 
				Socket clientSocket = serverSocket.accept();	

				// when an incoming request comes through, create a client thread to handle the request
				ServerThread clientServiceThread = new ServerThread(clientSocket, myRouter);
				clientServiceThread.start();

			} catch (IOException ioe) {
				System.out.println("Exception encountered on accept. Stack Trace :");
				ioe.printStackTrace();
			}

		}
		
		try 
        { 
            serverSocket.close(); 
            System.out.println("Server Stopped"); 
        } 
        catch(Exception ioe) 
        { 
            System.out.println("Problem stopping server socket"); 
            System.exit(-1); 
        } 

	}
}
